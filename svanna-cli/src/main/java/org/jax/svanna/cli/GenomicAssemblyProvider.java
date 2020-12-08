package org.jax.svanna.cli;

import org.monarchinitiative.variant.api.Contig;
import org.monarchinitiative.variant.api.GenomicAssembly;
import org.monarchinitiative.variant.api.SequenceRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Static utility class for getting {@link GenomicAssembly} from the genome assembly report file.
 */
public class GenomicAssemblyProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(GenomicAssemblyProvider.class);

    private static final NumberFormat NF = NumberFormat.getInstance();

    private static final Pattern ASSEMBLY_NAME_PATTERN = Pattern.compile("^#\\s*Assembly\\s+name:\\s*(?<payload>[\\w.]+)$");
    private static final Pattern ORGANISM_PATTERN = Pattern.compile("^#\\s*Organism name:\\s+(?<payload>[\\w\\s()]+)$");
    private static final Pattern TAXON_ID_PATTERN = Pattern.compile("^#\\s*Taxid:\\s+(?<payload>\\w+)$");
    private static final Pattern SUBMITTER_PATTERN = Pattern.compile("^#\\s*Submitter:\\s+(?<payload>[\\w\\s]+)$");
    private static final Pattern DATE_PATTERN = Pattern.compile("^#\\s*Date:\\s+(?<payload>[\\d-]+)$");
    private static final Pattern GEN_BANK_PATTERN = Pattern.compile("^#\\s*GenBank assembly accession:\\s+(?<payload>[\\w_.]+)$");
    private static final Pattern REF_SEQ_PATTERN = Pattern.compile("^#\\s*RefSeq assembly accession:\\s+(?<payload>[\\w_.]+)$");

    private GenomicAssemblyProvider() {
        // private no-op
    }

    public static GenomicAssembly fromAssemblyReport(Path assemblyReport) throws IOException {
        String assemblyName = null;
        String organismName = null;
        String taxonId = null;
        String submitter = null;
        String date = null;
        String genBankAccession = null;
        String refSeqAccession = null;
        List<String> contigLines = new ArrayList<>();
        try (final BufferedReader reader = Files.newBufferedReader(assemblyReport)) {
            String line;
            boolean nameFound = false;
            boolean organismFound = false;
            boolean taxonIdFound = false;
            boolean submitterFound = false;
            boolean dateFound = false;
            boolean genBankAccFound = false;
            boolean refSeqAccFound = false;

            while ((line = reader.readLine()) != null) {
                if (!nameFound) {
                    Matcher matcher = ASSEMBLY_NAME_PATTERN.matcher(line);
                    if (matcher.matches()) {
                        assemblyName = matcher.group("payload");
                        nameFound = true;
                        continue;
                    }
                }
                if (!organismFound) {
                    Matcher matcher = ORGANISM_PATTERN.matcher(line);
                    if (matcher.matches()) {
                        organismName = matcher.group("payload");
                        organismFound = true;
                        continue;
                    }
                }
                if (!taxonIdFound) {
                    Matcher matcher = TAXON_ID_PATTERN.matcher(line);
                    if (matcher.matches()) {
                        taxonId = matcher.group("payload");
                        taxonIdFound = true;
                        continue;
                    }
                }
                if (!submitterFound) {
                    Matcher matcher = SUBMITTER_PATTERN.matcher(line);
                    if (matcher.matches()) {
                        submitter = matcher.group("payload");
                        submitterFound = true;
                        continue;
                    }
                }
                if (!dateFound) {
                    Matcher matcher = DATE_PATTERN.matcher(line);
                    if (matcher.matches()) {
                        date = matcher.group("payload");
                        dateFound = true;
                        continue;
                    }
                }
                if (!genBankAccFound) {
                    Matcher matcher = GEN_BANK_PATTERN.matcher(line);
                    if (matcher.matches()) {
                        genBankAccession = matcher.group("payload");
                        genBankAccFound = true;
                        continue;
                    }
                }
                if (!refSeqAccFound) {
                    Matcher matcher = REF_SEQ_PATTERN.matcher(line);
                    if (matcher.matches()) {
                        refSeqAccession = matcher.group("payload");
                        refSeqAccFound = true;
                        continue;
                    }
                }

                if (!line.startsWith("#")) {
                    // header lines start with #
                    contigLines.add(line);
                }
            }
        }


        // check that we have assembly ID and taxon id
        if (Stream.of(assemblyName, organismName, taxonId, submitter, date, genBankAccession, refSeqAccession).anyMatch(Objects::isNull)) {
            String msg = "Did not find at least one required metadata field the assembly report file " + assemblyReport;
            throw new IOException(msg);
        }

        // now let's create contigs
        Set<Contig> contigs = new HashSet<>();
        ContigIdAssigner idAssigner = new ContigIdAssigner();
        for (String line : contigLines) {
            /*
            The line is a string like
            # Sequence-Name	Sequence-Role	Assigned-Molecule	Assigned-Molecule-Location/Type	GenBank-Accn	Relationship	RefSeq-Accn	Assembly-Unit	Sequence-Length	UCSC-style-name
            1	assembled-molecule	1	Chromosome	CM000663.1	=	NC_000001.10	Primary Assembly	249250621	chr1
            */
            String[] token = line.split("\t");
            if (token.length != 10) {
                LOGGER.warn("Found contig line with {} columns, expected 10: `{}`", token.length, line);
                continue;
            }

            String sequenceName = token[0];
            SequenceRole role = parseSequenceRole(token[1]);
            int contigId = idAssigner.makeIdFromPrimaryName(sequenceName);

            String ctgGenBankAccn = token[4]; // add GenBank-Accn (e.g. CM000663.2, KI270722.1, J01415.2, ...)
            String ctgRefSeqAccn = token[6]; // add RefSeq-Accn (e.g. NC_000001.11, NT_187377.1, NC_012920.1, ...)
            String ctgUcscSeqAccn = token[9]; // add UCSC-style-name (e.g. chr1, chr14_KI270722v1_random, chrM, ...)

            final int length;
            try {
                length = NF.parse(token[8]).intValue();
            } catch (ParseException e) {
                LOGGER.warn("Found contig line with invalid length `{}`: `{}`", token[8], line);
                continue;
            }

            Contig contig = Contig.of(contigId, sequenceName, role, length, ctgGenBankAccn, ctgRefSeqAccn, ctgUcscSeqAccn);
            contigs.add(contig);
        }

        return new GenomicAssemblyDefault(assemblyName, organismName, taxonId, submitter, date, genBankAccession, refSeqAccession, contigs);
    }

    private static SequenceRole parseSequenceRole(String role) {
        switch (role.toLowerCase()) {
            case "assembled-molecule":
                return SequenceRole.ASSEMBLED_MOLECULE;
            case "unlocalized-scaffold":
                return SequenceRole.UNLOCALIZED_SCAFFOLD;
            case "unplaced-scaffold":
                return SequenceRole.UNPLACED_SCAFFOLD;
            case "alt-scaffold":
                return SequenceRole.ALT_SCAFFOLD;
            case "novel-patch":
                return SequenceRole.NOVEL_PATCH;
            case "fix-patch":
                return SequenceRole.FIX_PATCH;
            default:
                return SequenceRole.UNKNOWN;
        }
    }

    /**
     * Each contig of a genome assembly is assigned an integer ID. We want the ID numbering to be intuitive within human
     * assembly (e.g. "chr1" maps to 1, "chrX" maps to 23, ...). The assignment of intuitive integer IDs is
     * a responsibility of this class.
     */
    private static class ContigIdAssigner {

        /**
         * We start the assignment of IDs for other than primary chromosomes from this number.
         */
        private final AtomicInteger contigId = new AtomicInteger(26);

        int makeIdFromPrimaryName(String primaryName) {
            try {
                // if chromosome name in range 1..22
                return Integer.parseInt(primaryName);
            } catch (NumberFormatException e) {
                // we just move on
            }

            switch (primaryName.toUpperCase()) {
                case "X":
                    return 23;
                case "Y":
                    return 24;
                case "MT":
                case "M":
                    return 25;
                default:
                    // this is not the primary chromosome
                    return contigId.getAndIncrement();
            }
        }
    }

}
