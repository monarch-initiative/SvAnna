package org.jax.svann.reference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * This class provides genome assemblies defined in assembly report files. The assembly report files can be downloaded
 * from Genome Reference Consortium web.
 */
abstract class AssemblyReportGenomeAssemblyProvider implements GenomeAssemblyProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(AssemblyReportGenomeAssemblyProvider.class);

    private static final Pattern ASSEMBLY_ID_PATTERN = Pattern.compile("^#\\s*Assembly\\s+name:\\s*(?<assembly>[\\w.]+)$");
    private static final Pattern ORGANISM_PATTERN = Pattern.compile("^#\\s*Taxid:\\s+(?<taxon>\\w+)$");

    private final Map<String, GenomeAssembly> assemblyMap;

    protected AssemblyReportGenomeAssemblyProvider(Path... assemblyReportPaths) {
        assemblyMap = Arrays.stream(assemblyReportPaths)
                .map(AssemblyReportGenomeAssemblyProvider::parseAssemblyReport)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toUnmodifiableMap(GenomeAssembly::getId, Function.identity()));
    }

    private static Optional<GenomeAssembly> parseAssemblyReport(Path assemblyReportPath) {
        String assemblyId = null;
        String taxonId = null;
        List<String> contigLines = new ArrayList<>();
        try (final BufferedReader reader = Files.newBufferedReader(assemblyReportPath)) {
            String line;
            boolean idFound = false, organismFound = false;

            while ((line = reader.readLine()) != null) {
                if (!idFound) {
                    final Matcher matcher = ASSEMBLY_ID_PATTERN.matcher(line);
                    if (matcher.matches()) {
                        assemblyId = matcher.group("assembly");
                        idFound = true;
                        continue;
                    }
                }
                if (!organismFound) {
                    final Matcher matcher = ORGANISM_PATTERN.matcher(line);
                    if (matcher.matches()) {
                        taxonId = matcher.group("taxon");
                        organismFound = true;
                        continue;
                    }
                }
                if (!line.startsWith("#")) {
                    // header lines start with #
                    contigLines.add(line);
                }
            }
        } catch (IOException e) {
            LOGGER.warn("Error parsing assembly report at `{}`: `{}`", assemblyReportPath, e.getMessage());
            return Optional.empty();
        }

        // check that we have assembly ID and taxon id
        if (assemblyId == null || taxonId == null) {
            LOGGER.warn("Did not find assembly ID or taxon ID in the assembly report file `{}`", assemblyReportPath);
            return Optional.empty();
        }

        // now let's create contigs
        final Map<Integer, Contig> contigMap = new HashMap<>();
        ContigIdAssigner idAssigner = new ContigIdAssigner();
        for (String line : contigLines) {
            /*
            The line is a string like
            # Sequence-Name	Sequence-Role	Assigned-Molecule	Assigned-Molecule-Location/Type	GenBank-Accn	Relationship	RefSeq-Accn	Assembly-Unit	Sequence-Length	UCSC-style-name
            1	assembled-molecule	1	Chromosome	CM000663.1	=	NC_000001.10	Primary Assembly	249250621	chr1
            */
            final String[] token = line.split("\t");
            if (token.length != 10) {
                LOGGER.warn("Found contig line with {} columns, expected 10: `{}`", token.length, line);
                return Optional.empty();
            }

            final Set<String> names = new HashSet<>();
            final String primaryName = token[0];
            final SequenceRole role = parseSequenceRole(token[1]);
            final int contigId = idAssigner.makeIdFromPrimaryName(primaryName);

            names.add(token[4]); // add GenBank-Accn (e.g. CM000663.2, KI270722.1, J01415.2, ...)
            names.add(token[6]); // add RefSeq-Accn (e.g. NC_000001.11, NT_187377.1, NC_012920.1, ...)
            names.add(token[9]); // add UCSC-style-name (e.g. chr1, chr14_KI270722v1_random, chrM, ...)

            final int length;
            try {
                length = Integer.parseInt(token[8]);
            } catch (NumberFormatException e) {
                LOGGER.warn("Found contig line with invalid length `{}`: `{}`", token[8], line);
                return Optional.empty();
            }
            contigMap.put(contigId, StandardContig.of(contigId, primaryName, names, role, length));
        }

        return Optional.of(StandardGenomeAssembly.of(assemblyId, taxonId, contigMap));
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

    @Override
    public Optional<GenomeAssembly> getAssembly(String id) {
        return assemblyMap.containsKey(id)
                ? Optional.of(assemblyMap.get(id))
                : Optional.empty();
    }

    @Override
    public Set<String> getAssemblyIds() {
        return Set.copyOf(assemblyMap.keySet());
    }

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
