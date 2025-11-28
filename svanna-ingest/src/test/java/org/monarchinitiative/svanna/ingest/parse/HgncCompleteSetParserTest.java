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
    public void parseHgncToNcbiGeneTable() throws IOException {
        Map<String, String> hgncToNcbigene;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(HgncCompleteSetParserTest.class.getResourceAsStream(TEST_PATH))))) {
            hgncToNcbigene = HgncCompleteSetParser.parseHgncToNcbiGeneTable(reader);
        }

        assertThat(hgncToNcbigene, hasEntry("HGNC:5", "NCBIGene:1")); // A1BG
        assertThat(hgncToNcbigene, hasEntry("HGNC:37133", "NCBIGene:503538")); // A1BG-AS1
        assertThat(hgncToNcbigene, hasEntry("HGNC:24086", "NCBIGene:29974")); // A1CF
        assertThat(hgncToNcbigene, hasEntry("HGNC:7", "NCBIGene:2")); // A2M
    }
}