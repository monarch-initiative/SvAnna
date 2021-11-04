package org.jax.svanna.ingest.parse.dosage;

import org.jax.svanna.ingest.parse.IOUtils;
import org.jax.svanna.ingest.parse.IngestRecordParser;
import org.jax.svanna.model.landscape.dosage.DosageElement;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.svart.GenomicRegion;
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
public class ClingenGeneCurationParser implements IngestRecordParser<DosageElement> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClingenGeneCurationParser.class);

    private static final Pattern NUMBER = Pattern.compile("\\d+");

    private final Path clingenGeneListPath;

    private final Map<TermId, ? extends GenomicRegion> geneRegions;

    public ClingenGeneCurationParser(Path clingenGeneListPath,
                                     Map<TermId, ? extends GenomicRegion> geneRegions) {
        this.clingenGeneListPath = clingenGeneListPath;
        this.geneRegions = geneRegions;
    }

    @Override
    public Stream<? extends DosageElement> parse() throws IOException {
        BufferedReader reader = Files.newBufferedReader(clingenGeneListPath);
        return reader.lines()
                .onClose(IOUtils.close(reader))
                .filter(line -> !line.startsWith("#")) // header
                .map(toDosageElements())
                .flatMap(Collection::stream);
    }

    private Function<String, Collection<DosageElement>> toDosageElements() {
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
                return Collections.emptyList();
            }

            // ID - we use gene symbol as the ID, assuming that there is only one line per gene
            String id = tokens[0];
            if (id.isBlank()) {
                LOGGER.warn("Skipping line with no ID: `{}`", line);
                return Collections.emptyList();
            }


            String geneId = tokens[1];
            Matcher geneIdMatcher = NUMBER.matcher(geneId);
            if (!geneIdMatcher.matches()) {
                LOGGER.warn("Invalid gene ID {} (not numeric)", geneId);
                return Collections.emptyList();
            }

            TermId geneTermId = TermId.of("NCBIGene:" + geneId);
            GenomicRegion geneRegion = geneRegions.get(geneTermId);
            if (geneRegion == null) {
                LOGGER.warn("Skipping unknown gene {} ({}) in line `{}`", tokens[0], geneTermId, line);
                return Collections.emptyList();
            }

            String haploinsufficiency = tokens[4];
            String triplosensitivity = tokens[12];

            return makeDosageElements(geneRegion.contig(), geneRegion.strand(), geneRegion.coordinates(), id, haploinsufficiency, triplosensitivity);
        };
    }

}
