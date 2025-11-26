package org.monarchinitiative.svanna.io.hpo;

import org.junit.jupiter.api.Test;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.phenol.ontology.similarity.TermPair;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;

public class IcMicaDictUtilsTest {

    private static final Map<TermPair, Double> EXAMPLE = Map.of(
            TermPair.asymmetric(TermId.of("HP:0000001"), TermId.of("HP:0000002")), 1.2,
            TermPair.asymmetric(TermId.of("HP:0000004"), TermId.of("HP:0000003")), 2.,
            TermPair.asymmetric(TermId.of("HP:0000009"), TermId.of("HP:0000007")), 3.2,
            TermPair.asymmetric(TermId.of("HP:0000005"), TermId.of("HP:0000005")), 4.4
    );


    @Test
    public void encode() throws IOException {
        Writer writer = new StringWriter();
        LocalDate date = LocalDate.of(2025, 2, 14);
        IcMicaDictUtils.writeTermPairMap(EXAMPLE, writer, date, "v2025-11-12", "v2025-11-12");

        List<String> split = Arrays.asList(writer.toString().split(System.lineSeparator()));
        assertThat(split, hasItems(
                "# Information content of the most informative common ancestor for term pairs",
                "# HPO=v2025-11-12;HPOA=v2025-11-12;CREATED=2025-02-14",
                "term_a,term_b,ic_mica",
                "HP:0000001,HP:0000002,1.2",
                "HP:0000009,HP:0000007,3.2",
                "HP:0000004,HP:0000003,2.0",
                "HP:0000005,HP:0000005,4.4"
        ));
    }

    @Test
    public void roundTrip() throws IOException {
        Writer writer = new StringWriter();
        LocalDate date = LocalDate.of(2025, 2, 14);
        IcMicaDictUtils.writeTermPairMap(EXAMPLE, writer, date, "v2025-11-12", "v2025-11-12");

        StringReader reader = new StringReader(writer.toString());
        Map<TermPair, Double> actual = IcMicaDictUtils.readTermPairMap(reader);

        assertThat(EXAMPLE, equalTo(actual));
    }
}