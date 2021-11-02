package org.jax.svanna.db.phenotype;

import org.jax.svanna.core.LogUtils;
import org.jax.svanna.core.hpo.TermPair;
import org.jax.svanna.db.SvAnnaDbException;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class MicaDao {

    private static final Logger LOGGER = LoggerFactory.getLogger(MicaDao.class);

    private static final double TOLERANCE = 5E-9;

    private final JdbcTemplate jdbcTemplate;

    public MicaDao(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    private static int parseTermId(String value) throws SvAnnaDbException {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new SvAnnaDbException("Unable to parse TermID `" + value + '`', e);
        }
    }

    private static String prepareNZeros(int n) {
        char[] chars = new char[n];
        Arrays.fill(chars, '0');
        return new String(chars);
    }

    private static ResultSetExtractor<Double> processResults() {
        return rs -> (rs.first()) ? rs.getDouble(1) : Double.NaN;
    }

    public int insertItem(TermPair pair, double similarity) {
        if (similarity < TOLERANCE) {
            if (similarity < 0.)
                throw new SvAnnaDbException("Similarity must be positive, got " + similarity);
            else
                // skip inserting 0 as this is the default similarity for the non-related terms
                return 0;
        }
        int leftId = parseTermId(pair.left().getId());
        int rightId = parseTermId(pair.right().getId());

        double existing = getMica(leftId, rightId);
        if (!Double.isNaN(existing))
            // similarity for the term pair is already in the database
            return 0;

        String sql = "insert into SVANNA.RESNIK_SIMILARITY(LEFT_ID, RIGHT_ID, SIMILARITY) " +
                "values ( ?, ?, ? )";

        return jdbcTemplate.update(sql, leftId, rightId, similarity);
    }

    public double getMica(TermPair pair) {
        int leftId = parseTermId(pair.left().getId());
        int rightId = parseTermId(pair.right().getId());
        try {
            double similarity = getMica(leftId, rightId);
            return Double.isNaN(similarity)
                    ? 0. // missing term pair is assumed to be non-related, hence 0.
                    : similarity;
        } catch (DataAccessException e) {
            LogUtils.logDebug(LOGGER, "Could not find entry for `{}`, `{}`", leftId, rightId);
            return 0.;
        }
    }

    public Map<TermPair, Double> getAllMicaValues() {
        String sql = "select LEFT_ID, RIGHT_ID, SIMILARITY from SVANNA.RESNIK_SIMILARITY";
        return jdbcTemplate.query(sql, rs -> {
            Map<TermPair, Double> similarityMap = new HashMap<>();
            while (rs.next()) {
                String lPart = rs.getString(1);
                if (lPart.length() > 7)
                    throw new SvAnnaDbException("Unable to work with HPO terms with id consisting of >7 positions: HP:" + lPart);
                String left = "HP:" + prepareNZeros(7 - lPart.length()) + lPart;

                String rPart = rs.getString(2);
                if (rPart.length() > 7)
                    throw new SvAnnaDbException("Unable to work with HPO terms with id consisting of >7 positions: HP:" + rPart);
                String right = "HP:" + prepareNZeros(7 - rPart.length()) + rPart;

                similarityMap.put(TermPair.of(TermId.of(left), TermId.of(right)), rs.getDouble(3));
            }
            return similarityMap;
        });
    }

    private Double getMica(int left, int right) {
        String sql = "select SIMILARITY from SVANNA.RESNIK_SIMILARITY " +
                "where LEFT_ID = ? and RIGHT_ID = ? " +
                "limit 1";
        return jdbcTemplate.query(sql, processResults(), left, right);
    }
}
