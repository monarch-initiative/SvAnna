package org.jax.svanna.db.additive.dispatch;

import org.jax.svanna.core.exception.LogUtils;
import org.jax.svanna.core.landscape.TadBoundary;
import org.jax.svanna.core.landscape.TadBoundaryDefault;
import org.monarchinitiative.svart.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

class PaddingTadBoundaryDao {

    private static final Logger LOGGER = LoggerFactory.getLogger(PaddingTadBoundaryDao.class);

    private final NamedParameterJdbcTemplate template;
    private final GenomicAssembly genomicAssembly;
    private final double stabilityThreshold;

    PaddingTadBoundaryDao(DataSource dataSource, GenomicAssembly genomicAssembly, double stabilityThreshold) {
        this.template = new NamedParameterJdbcTemplate(dataSource);
        this.genomicAssembly = genomicAssembly;
        this.stabilityThreshold = stabilityThreshold;
    }

    Optional<TadBoundary> upstreamOf(GenomicRegion region) {
        SqlParameterSource paramSource = new MapSqlParameterSource()
                .addValue("contig", region.contigId())
                .addValue("position", region.startOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased()))
                .addValue("stability", stabilityThreshold);
        String sql = region.strand().isPositive()
                ? "select top 1 CONTIG, START, END, ID, STABILITY " +
                "  from SVANNA.TAD_BOUNDARY " +
                "    where CONTIG = :contig and END < :position and STABILITY > :stability " +
                "  order by END DESC"

                : "select top 1 CONTIG, START, END, ID, STABILITY " +
                "  from SVANNA.TAD_BOUNDARY " +
                "    where CONTIG = :contig and START > :position and STABILITY > :stability " +
                "  order by START";
        return template.query(sql, paramSource, mapToTadBoundary(region.strand()));
    }

    Optional<TadBoundary> downstreamOf(GenomicRegion region) {
        SqlParameterSource paramSource = new MapSqlParameterSource()
                .addValue("contig", region.contigId())
                .addValue("position", region.endOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased()))
                .addValue("stability", stabilityThreshold);
        String sql = region.strand().isPositive()
                ? "select top 1 CONTIG, START, END, ID, STABILITY " +
                "  from SVANNA.TAD_BOUNDARY " +
                "    where CONTIG = :contig and START > :position and STABILITY > :stability " +
                "  order by START"
                : "select top 1 CONTIG, START, END, ID, STABILITY " +
                "  from SVANNA.TAD_BOUNDARY " +
                "    where CONTIG = :contig and END < :position and STABILITY > :stability " +
                "  order by END DESC";
        return template.query(sql, paramSource, mapToTadBoundary(region.strand()));
    }

    private ResultSetExtractor<Optional<TadBoundary>> mapToTadBoundary(Strand strand) {
        return rs -> {
            if (rs.first()) {
                TadBoundary boundary = mapRowToTadBoundary(rs);
                return Optional.of(boundary.withStrand(strand));
            }
            return Optional.empty();
        };
    }

    /**
     ** @param contig contig
     * @param left 0-based start coordinate on positive strand
     * @param right 0-based end coordinate on positive strand
     * @return TAD pair where left TAD is upstream of {@code left} on positive strand
     * and right TAD is downstream of {@code right} on positive strand
     */
    TadBoundaryPair getBoundaryPair(Contig contig, int left, int right) {
        SqlParameterSource paramSource = new MapSqlParameterSource()
                .addValue("contig", contig.id())
                .addValue("left", left)
                .addValue("right", right)
                .addValue("stability", stabilityThreshold);

        String sql = "(select top 1 CONTIG, ID, START, END, STABILITY " + // upstream
                "  from SVANNA.TAD_BOUNDARY " +
                "  where CONTIG = :contig and END < :left and STABILITY > :stability " +
                "  order by END DESC) " +
                "union " +
                "(select top 1 CONTIG, ID, START, END, STABILITY " + // downstream
                "  from SVANNA.TAD_BOUNDARY " +
                "  where CONTIG = :contig and START > :right and STABILITY > :stability " +
                "  order by START) " +
                "order by START";

        return template.query(sql, paramSource, boundaryPairExtractor(left, right));
    }

    private ResultSetExtractor<TadBoundaryPair> boundaryPairExtractor(int left, int right) {
        return rs -> {
            TadBoundary upstream = null, downstream = null;
            while (rs.next()) {
                TadBoundary boundary = mapRowToTadBoundary(rs);
                int boundaryStart = boundary.startOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased());
                int boundaryEnd = boundary.endOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased());
                if (boundaryStart <= left) {
                    if (upstream != null) {
                        LogUtils.logWarn(LOGGER, "Found the 2nd upstream boundary!");
                        throw new IllegalArgumentException("Found the 2nd upstream boundary!");
                    }
                    upstream = boundary;
                } else if (right <= boundaryEnd) {
                    if (downstream != null) {
                        LogUtils.logWarn(LOGGER, "Found 2nd downstream TAD boundary!");
                        throw new IllegalArgumentException("Found 2nd downstream TAD boundary!");
                    }
                    downstream = boundary;
                } else {
                    LogUtils.logWarn(LOGGER, "Unexpected TAD boundary!: {}", boundary);
                    throw new IllegalArgumentException("Unexpected TAD boundary: " + boundary);
                }
            }
            return new TadBoundaryPair(upstream, downstream);
        };
    }

    private TadBoundary mapRowToTadBoundary(ResultSet rs) throws SQLException {
        return TadBoundaryDefault.of(genomicAssembly.contigById(rs.getInt("CONTIG")),
                Strand.POSITIVE, CoordinateSystem.zeroBased(),
                Position.of(rs.getInt("START")), Position.of(rs.getInt("END")),
                rs.getString("ID"),
                rs.getFloat("STABILITY"));
    }
}
