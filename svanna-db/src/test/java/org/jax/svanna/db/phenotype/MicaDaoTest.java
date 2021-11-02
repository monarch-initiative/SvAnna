package org.jax.svanna.db.phenotype;

import org.jax.svanna.core.hpo.TermPair;
import org.jax.svanna.db.TestDataConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;

import javax.sql.DataSource;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@SpringBootTest(classes = TestDataConfig.class)
public class MicaDaoTest {

    private static final double ERROR = 5e-6;
    @Autowired
    public DataSource dataSource;

    @ParameterizedTest
    @CsvSource({
            "HP:0000001, HP:0000002, .5",
            "HP:0000003, HP:0000004, .2",
            "HP:9999999, HP:8888888, 0.",
    })
    @Sql({"resnik_similarity_create_table.sql", "resnik_similarity_insert_data.sql"})
    public void getSimilarity(String left, String right, double expected) {
        MicaDao dao = new MicaDao(dataSource);
        double similarity = dao.getMica(TermPair.symmetric(TermId.of(left), TermId.of(right)));

        assertThat(similarity, closeTo(expected, ERROR));
    }

    @Test
    @Sql({"resnik_similarity_create_table.sql", "resnik_similarity_insert_data.sql"})
    public void getAllSimilarities() {
        MicaDao dao = new MicaDao(dataSource);
        Map<TermPair, Double> similarities = dao.getAllMicaValues();

        assertThat(similarities.size(), equalTo(2));
        assertThat(similarities.keySet(), containsInAnyOrder(
                TermPair.symmetric(TermId.of("HP:0000002"), TermId.of("HP:0000001")),
                TermPair.symmetric(TermId.of("HP:0000004"), TermId.of("HP:0000003"))
        ));
    }

    @ParameterizedTest
    @CsvSource({
            "HP:0000001, HP:0000002, .5, 1",
            "HP:0000003, HP:0000004, .2, 1",
            "HP:9999999, HP:8888888, 0., 0",
    })
    @Sql({"resnik_similarity_create_table.sql"})
    public void insertItem(String left, String right, double similarity, int expected) {
        MicaDao dao = new MicaDao(dataSource);
        int inserted = dao.insertItem(TermPair.symmetric(TermId.of(left), TermId.of(right)), similarity);

        assertThat(inserted, equalTo(expected));
    }

    @Test
    @Sql({"resnik_similarity_create_table.sql"})
    public void insertDuplicateItem() {
        MicaDao dao = new MicaDao(dataSource);
        TermPair pair = TermPair.symmetric(TermId.of("HP:0000123"), TermId.of("HP:0000456"));
        int first = dao.insertItem(pair, .5);
        assertThat(first, equalTo(1));

        int second = dao.insertItem(pair, .5);
        assertThat(second, equalTo(0));
    }
}