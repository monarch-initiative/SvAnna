package org.jax.svanna.ingest.parse.dosage;

import org.jax.svanna.ingest.parse.IOUtils;
import org.jax.svanna.ingest.parse.IngestRecordParser;
import org.jax.svanna.model.landscape.dosage.DosageRegion;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.svart.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.jax.svanna.ingest.parse.dosage.DosageElementsUtil.makeDosageElements;

/**
 * Clingen gene list is a TSV file with several header lines that start with <code>#</code>.
 * <p>
 * The rows represent genes.
 */
public class ClingenGeneCurationParser implements IngestRecordParser<DosageRegion> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClingenGeneCurationParser.class);

    private static final Pattern NUMBER = Pattern.compile("\\d+");
    private static final Pattern GENOMIC_LOCATION = Pattern.compile("(?<contig>(chr)?(\\d+|[XYZ])):(?<start>\\d+)-(?<end>\\d+)");

    private final Path clingenGeneListPath;
    private final GenomicAssembly assembly;
    private final Map<TermId, ? extends GenomicRegion> geneRegions;
    private final Map<Integer, Integer> ncbiGeneToHgnc;

    public ClingenGeneCurationParser(Path clingenGeneListPath,
                                     GenomicAssembly assembly, Map<TermId, ? extends GenomicRegion> geneRegions,
                                     Map<Integer, Integer> ncbiGeneToHgnc) {
        this.clingenGeneListPath = clingenGeneListPath;
        this.assembly = assembly;
        this.geneRegions = geneRegions;
        this.ncbiGeneToHgnc = ncbiGeneToHgnc;
    }

    @Override
    public Stream<? extends DosageRegion> parse() throws IOException {
        BufferedReader reader = Files.newBufferedReader(clingenGeneListPath);
        return reader.lines()
                .onClose(IOUtils.close(reader))
                .filter(line -> !line.startsWith("#")) // header
                .map(toDosageElements())
                .flatMap(Optional::stream);
    }

    private Function<String, Optional<DosageRegion>> toDosageElements() {
        return line -> {
            /*
            A line like:
            #Gene Symbol    Gene ID cytoBand        Genomic Location        Haploinsufficiency Score        Haploinsufficiency Description  Haploinsufficiency PMID1        Haploinsufficiency PMID2        Haploinsufficiency PMID3        Haploinsufficiency PMID4        Haploinsufficiency PMID5        Haploinsufficiency PMID6        Triplosensitivity Score Triplosensitivity Description   Triplosensitivity PMID1 Triplosensitivity PMID2 Triplosensitivity PMID3 Triplosensitivity PMID4 Triplosensitivity PMID5 Triplosensitivity PMID6 Date Last Evaluated     Loss phenotype OMIM ID  Triplosensitive phenotype OMIM ID
            A4GALT  53947   22q13.2 chr22:42692115-42721301 30      Gene associated with autosomal recessive phenotype                                                      0       No evidence available 2014-12-11      111400

            We're interested in columns (indices) `Gene ID` (1), `Haploinsufficiency Score` (4), and `Triplosensitivity Score` (12)
             */
            String[] tokens = line.split("\t", 23);
            if (tokens.length != 23) {
                LOGGER.warn("Expected {} columns and found {}. Skipping the line `{}`", 23, tokens.length, line);
                return Optional.empty();
            }

            // ID - we use gene symbol as the ID, assuming that there is only one line per gene
            String id = tokens[0];
            if (id.isBlank()) {
                LOGGER.warn("Skipping line with no ID: `{}`", line);
                return Optional.empty();
            }

            // parse Gene ID - numeric part of NCBIGene
            String ncbiGeneId = tokens[1];
            Matcher geneIdMatcher = NUMBER.matcher(ncbiGeneId);
            if (!geneIdMatcher.matches()) {
                LOGGER.warn("Invalid gene ID {} (not numeric)", ncbiGeneId);
                return Optional.empty();
            }

            // We must extract gene region for the gene, either:
            // - using HGNC ID that corresponds to the NCBIGene and then the corresponding gene region, or
            // - by parsing the `Genomic Location` (token[3])
            GenomicRegion geneRegion = null;

            // Try to get the
            Optional<TermId> hgncId = getHgncId(tokens[1]);
            if (hgncId.isPresent()) {
                geneRegion = geneRegions.get(hgncId.get());
            }


            if (geneRegion == null) {
                Optional<GenomicRegion> region = getRegion(tokens[3]);
                if (region.isEmpty())
                    return Optional.empty();
                else
                    geneRegion = region.get();
            }


            String haploinsufficiency = tokens[4];
            String triplosensitivity = tokens[12];

            return makeDosageElements(geneRegion.contig(), geneRegion.strand(), geneRegion.coordinates(), id, haploinsufficiency, triplosensitivity);
        };
    }

    private Optional<GenomicRegion> getRegion(String payload) {
        // parse `Genomic Location`
        Matcher genomicLocation = GENOMIC_LOCATION.matcher(payload);
        if (!genomicLocation.matches()) {
            LOGGER.warn("Invalid genomic location `{}`", payload);
            return Optional.empty();
        }

        Contig contig = assembly.contigByName(genomicLocation.group("contig"));
        if (contig.isUnknown()) {
            LOGGER.warn("Unknown contig `{}`", genomicLocation.group("contig"));
            return Optional.empty();
        }

        int start = Integer.parseInt(genomicLocation.group("start"));
        int end = Integer.parseInt(genomicLocation.group("end"));
        return Optional.of(GenomicRegion.of(contig, Strand.POSITIVE, CoordinateSystem.oneBased(), start, end));
    }

    private Optional<TermId> getHgncId(String ncbiGeneId) {
        // parse Gene ID - numeric part of NCBIGene
        Matcher geneIdMatcher = NUMBER.matcher(ncbiGeneId);
        if (!geneIdMatcher.matches()) {
            LOGGER.warn("Invalid gene ID {} (not numeric)", ncbiGeneId);
            return Optional.empty();
        }

        // get HGNC ID that corresponds to the NCBIGene
        int ncbiGeneNumber = Integer.parseInt(ncbiGeneId);
        Integer hgncIdNumber = ncbiGeneToHgnc.get(ncbiGeneNumber);
        if (hgncIdNumber == null) {
            // there is no HGNC ID for the NCBIGene
            return Optional.empty();
        }

        return Optional.of(TermId.of(String.format("HGNC:%s", hgncIdNumber)));
    }

}
