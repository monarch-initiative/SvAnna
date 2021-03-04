package org.jax.svanna.db;

import org.jax.svanna.core.hpo.TermPair;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

public class TermSimilarityDao {

    private final JdbcTemplate template;

    public TermSimilarityDao(DataSource dataSource) {
        this.template = new JdbcTemplate(dataSource);
    }

    public int insertItem(String similarityMeasure, TermId left, TermId right, double similarity) {
        String sql = "INSERT INTO SVANNA.TERM_SIMILARITY (SIMILARITY_MEASURE, LEFT_TERM_ID, RIGHT_TERM_ID, SIMILARITY) " +
                " VALUES ( ?, ?, ?, ?)";
        return template.update(sql, similarityMeasure, left.getValue(), right.getValue(), similarity);
    }


    public Map<TermPair, Double> getResnikSimilarities() {
        String sql = "SELECT SIMILARITY_MEASURE, LEFT_TERM_ID, RIGHT_TERM_ID, SIMILARITY" +
                " from SVANNA.TERM_SIMILARITY " +
                " where SIMILARITY_MEASURE = 'RESNIK'";
        return template.query(sql, termPairMapExtractor());
    }

    private ResultSetExtractor<Map<TermPair, Double>> termPairMapExtractor() {
        return rs -> {
            Map<TermPair, Double> builder = new HashMap<>();
            while (rs.next()) {
                TermId left = TermId.of(rs.getString("LEFT_TERM_ID"));
                TermId right = TermId.of(rs.getString("RIGHT_TERM_ID"));
                double similarity = rs.getDouble("SIMILARITY");
                builder.put(TermPair.of(left, right), similarity);
            }
            return Map.copyOf(builder);
        };
    }

}
