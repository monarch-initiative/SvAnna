package org.jax.svanna.db.metadata;

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
                ? "select top 1 ID, START, END, STABILITY from SVANNA.TAD_BOUNDARY " +
                "  where CONTIG = :contig and END < :position and STABILITY > :stability " +
                "  order by END DESC"
                : "select top 1 ID, START, END, STABILITY from SVANNA.TAD_BOUNDARY " +
                "  where CONTIG = :contig and START > :position and STABILITY > :stability " +
                "  order by START";
        return template.query(sql, paramSource, mapToTadBoundary(region.strand()));
    }

    Optional<TadBoundary> downstreamOf(GenomicRegion region) {
        SqlParameterSource paramSource = new MapSqlParameterSource()
                .addValue("contig", region.contigId())
                .addValue("position", region.startOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased()))
                .addValue("stability", stabilityThreshold);
        String sql = region.strand().isPositive()
                ? "select top 1 ID, START, END, STABILITY from SVANNA.TAD_BOUNDARY " +
                "  where CONTIG = :contig and START > :position and STABILITY > :stability " +
                "  order by START"
                : "select top 1 ID, START, END, STABILITY from SVANNA.TAD_BOUNDARY " +
                "  where CONTIG = :contig and END < :position and STABILITY > :stability " +
                "  order by END DESC";
        return template.query(sql, paramSource, mapToTadBoundary(region.strand()));
    }

    private ResultSetExtractor<Optional<TadBoundary>> mapToTadBoundary(Strand strand) {
        return rs -> {
            if (rs.first()) {
                TadBoundary boundary = processRow(rs);
                return Optional.of(boundary.withStrand(strand));
            }
            return Optional.empty();
        };
    }

    TadBoundaryPair getBoundaryPair(Contig leftContig, int leftPosition, Contig rightContig, int rightPosition) {
        SqlParameterSource paramSource = new MapSqlParameterSource()
                .addValue("leftContig", leftContig.id())
                .addValue("leftPosition", leftPosition)
                .addValue("rightContig", rightContig.id())
                .addValue("rightPosition", rightPosition)
                .addValue("stability", stabilityThreshold);
        String sql = "(select top 1 ID, START, END, STABILITY " + // upstream
                "  from SVANNA.TAD_BOUNDARY " +
                "  where CONTIG = :leftContig and END < :leftPosition and STABILITY > :stability " +
                "  order by END DESC) " +
                "union " +
                "(select top 1 ID, START, END, STABILITY " + // downstream
                "  from SVANNA.TAD_BOUNDARY " +
                "  where CONTIG = :rightContig and START > :rightPosition and STABILITY > :stability " +
                "  order by START) " +
                "order by START";

        return template.query(sql, paramSource, boundaryPairExtractor(leftContig, leftPosition, rightContig, rightPosition));
    }

    private ResultSetExtractor<TadBoundaryPair> boundaryPairExtractor(Contig leftContig, int leftPosition, Contig rightContig, int rightPosition) {
        return rs -> {
            TadBoundary upstream = null, downstream = null;
            while (rs.next()) {
                TadBoundary boundary = processRow(rs);
                int boundaryStart = boundary.startOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased());
                int boundaryEnd = boundary.endOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased());
                if (boundary.contig().equals(leftContig) && boundaryStart <= leftPosition) {
                    if (upstream != null) {
                        LogUtils.logWarn(LOGGER, "Found the 2nd upstream boundary!");
                        throw new IllegalArgumentException("Found the 2nd upstream boundary!");
                    }
                    upstream = boundary;
                } else if (boundary.contig().equals(rightContig) && rightPosition <= boundaryEnd) {
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

    private TadBoundary processRow(ResultSet rs) throws SQLException {
        return TadBoundaryDefault.of(genomicAssembly.contigById(rs.getInt("ID")),
                Strand.POSITIVE, CoordinateSystem.zeroBased(),
                Position.of(rs.getInt("START")), Position.of(rs.getInt("END")),
                rs.getString("ID"), rs.getFloat("STABILITY"));
    }
}
