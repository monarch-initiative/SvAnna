package org.monarchinitiative.svanna.ingest.parse;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Objects;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class HgncCompleteSetParserTest {

    private static final String TEST_PATH = "hgnc_complete_set.head.txt";

    @Test
    public void parseNcbiToHgncTable() throws IOException {
        Map<Integer, Integer> table;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(HgncCompleteSetParserTest.class.getResourceAsStream(TEST_PATH))))) {
            table = HgncCompleteSetParser.parseNcbiToHgncTable(reader);
        }

        assertThat(table, hasEntry(1, 5)); // A1BG
        assertThat(table, hasEntry(2, 7)); // A2M
        assertThat(table, hasEntry(503_538, 37_133)); // A1BG-AS1
        assertThat(table, hasEntry(29_974, 24_086)); // A1CF
    }
}