package org.monarchinitiative.svanna.ingest.parse;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HgncCompleteSetParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(HgncCompleteSetParser.class);
    private static final int HGNC_ID_COL_IDX = 0;
    private static final int ENTREZ_ID_COL_IDX = 18;

    private HgncCompleteSetParser() {
        // static utility class
    }

    public static Map<Integer, Integer> parseNcbiToHgncTable(Path ncbiGeneToHgnc) throws IOException {
        if (Files.notExists(ncbiGeneToHgnc)) {
            throw new IOException("Table for mapping NCBIGene to HGNC does not exist at " + ncbiGeneToHgnc.toAbsolutePath());
        }

        try (BufferedReader reader = org.monarchinitiative.svanna.io.IOUtils.openForReading(ncbiGeneToHgnc)) {
            return parseNcbiToHgncTable(reader);
        }
    }

    public static Map<Integer, Integer> parseNcbiToHgncTable(BufferedReader reader) throws IOException {
        Map<Integer, Integer> results = new HashMap<>();

        CSVParser parser = CSVFormat.TDF.builder()
                .setSkipHeaderRecord(true)
                .build()
                .parse(reader);
        Pattern hgncPattern = Pattern.compile("HGNC:(?<payload>\\d+)");
        // hgnc_id	symbol	name    ... entrez_id	... mane_select	gencc
        // HGNC:5	A1BG	alpha-1-B glycoprotein	... 8086    ...	"ENST00000263100.8|NM_130786.4"
        for (CSVRecord record : parser) {
            // parse NCBIGene. Should be a number, but may be missing.
            String ncbiGene = record.get(ENTREZ_ID_COL_IDX);
            if (ncbiGene.isBlank())
                // missing NCBI gene ID for this gene
                continue;

            int ncbiGeneId;
            try {
                ncbiGeneId = Integer.parseInt(ncbiGene);
            } catch (NumberFormatException e) {
                LOGGER.warn("Skipping non-numeric NCBIGene/Entrez id `{}` on line #{}: `{}`", ncbiGene, record.getRecordNumber(), record);
                continue;
            }

            // parse HGNC id
            Matcher hgncMatcher = hgncPattern.matcher(record.get(HGNC_ID_COL_IDX));
            if (!hgncMatcher.matches()) {
                LOGGER.warn("Skipping HGNC id `{}` on line #{}: `{}`", record.get(HGNC_ID_COL_IDX), record.getRecordNumber(), record);
                continue;
            }
            Integer hgncId = Integer.parseInt(hgncMatcher.group("payload"));

            // store the results
            results.put(ncbiGeneId, hgncId);
        }
        return results;
    }
}

