package org.monarchinitiative.svanna.io.hpo;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.phenol.ontology.similarity.TermPair;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class IcMicaDictUtils {

    public static final String TERM_PAIR_SIMILARITY_NAME = "term-pair-similarity.csv.gz";
    private static final String[] HEADER = {"term_a", "term_b", "ic_mica"};
    private static final char COMMENT_MARKER = '#';

    private IcMicaDictUtils() {
        // static code only
    }

    public static void writeTermPairMap(Map<TermPair, Double> termPairResnikSimilarityMap,
                                        Writer writer,
                                        LocalDate now,
                                        String hpoVersion,
                                        String hpoaVersion) throws IOException {


        CSVPrinter printer = CSVFormat.Builder.create(CSVFormat.DEFAULT)
                .setRecordSeparator(System.lineSeparator())
                .setCommentMarker(COMMENT_MARKER)
                .build()
                .print(writer);
        // Metadata
        printer.printComment("Information content of the most informative common ancestor for term pairs");
        printer.printComment(String.format("HPO=%s;HPOA=%s;CREATED=%s", hpoVersion, hpoaVersion, now));

        // Header
        printer.printRecord(Arrays.asList(HEADER));

        // Content
        for (Map.Entry<TermPair, Double> e : termPairResnikSimilarityMap.entrySet()) {
            TermPair pair = e.getKey();
            printer.print(pair.getTidA().getValue());
            printer.print(pair.getTidB().getValue());
            printer.print(e.getValue());
            printer.println();
        }
    }

    public static Map<TermPair, Double> readTermPairMap(Reader reader) throws IOException {
        CSVParser parser = CSVFormat.DEFAULT.builder()
                .setSkipHeaderRecord(true)
                .setHeader(HEADER)
                .setCommentMarker(COMMENT_MARKER)
                .build()
                .parse(reader);

        Map<TermPair, Double> map = new HashMap<>();
        for (CSVRecord record : parser) {
            map.put(
                    TermPair.asymmetric(TermId.of(record.get("term_a")), TermId.of(record.get("term_b"))),
                    Double.parseDouble(record.get("ic_mica"))
            );
        }
        return map;
    }
}
