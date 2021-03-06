package org.jax.svanna.db.landscape;

import org.jax.svanna.core.landscape.TadBoundary;
import org.jax.svanna.core.landscape.TadBoundaryDefault;
import org.jax.svanna.db.IngestDao;
import org.monarchinitiative.svart.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class TadBoundaryDao implements IngestDao<TadBoundary>, AnnotationDao<TadBoundary> {

    private static final Logger LOGGER = LoggerFactory.getLogger(TadBoundaryDao.class);

    private final DataSource dataSource;

    private final GenomicAssembly genomicAssembly;

    private final NamedParameterJdbcTemplate template;

    private final double stabilityThreshold;

    public TadBoundaryDao(DataSource dataSource, GenomicAssembly genomicAssembly, double stabilityThreshold) {
        this.dataSource = dataSource;
        this.template = new NamedParameterJdbcTemplate(dataSource);
        this.genomicAssembly = genomicAssembly;
        this.stabilityThreshold = stabilityThreshold;
    }

    public TadBoundaryDao(DataSource dataSource, GenomicAssembly genomicAssembly) {
        this(dataSource, genomicAssembly, 0.);
    }

    @Override
    public int insertItem(TadBoundary item) {
        int updated = 0;

        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            String sql = "insert into SVANNA.TAD_BOUNDARY(CONTIG, START, END, ID, STABILITY) " +
                    "VALUES ( ?, ?, ?, ?, ?)";
            try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                preparedStatement.setInt(1, item.contigId());
                preparedStatement.setInt(2, item.startOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased()));
                preparedStatement.setInt(3, item.endOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased()));
                preparedStatement.setString(4, item.id());
                preparedStatement.setFloat(5, item.stability());

                updated += preparedStatement.executeUpdate();
                connection.commit();
            }catch (SQLException e) {
                if (LOGGER.isWarnEnabled())
                    LOGGER.warn("Error occurred, rolling back: {}", e.getMessage());
                connection.rollback();
            }
            connection.setAutoCommit(true);
        } catch (SQLException e) {
            if (LOGGER.isWarnEnabled())
                LOGGER.warn("Error occurred: {}", e.getMessage());
        }

        return updated;
    }

    @Override
    public List<TadBoundary> getAllItems() {
        throw new RuntimeException("The deprecated method is not implemented");
    }

    @Override
    public List<TadBoundary> getOverlapping(GenomicRegion query) {
        String sql = "select CONTIG, START, END, ID, STABILITY " +
                " from SVANNA.TAD_BOUNDARY " +
                " where CONTIG = :contig " +
                "   and :start < END " +
                "   and START < :end " +
                "   and STABILITY > :stability" +
                " order by START";
        SqlParameterSource paramsSource = new MapSqlParameterSource()
                .addValue("contig", query.contigId())
                .addValue("start", query.startOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased()))
                .addValue("end", query.endOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased()))
                .addValue("stability", stabilityThreshold);
        return template.query(sql, paramsSource, processResults());
    }

    private RowMapper<TadBoundary> processResults() {
        return (rs, i) -> TadBoundaryDefault.of(
                genomicAssembly.contigById(rs.getInt("CONTIG")),
                Strand.POSITIVE, CoordinateSystem.zeroBased(),
                Position.of(rs.getInt("START")),
                Position.of(rs.getInt("END")),
                rs.getString("ID"),
                rs.getFloat("STABILITY"));
    }

    public Optional<TadBoundary> upstreamOf(GenomicRegion region) {
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

    public Optional<TadBoundary> downstreamOf(GenomicRegion region) {
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


    private TadBoundary mapRowToTadBoundary(ResultSet rs) throws SQLException {
        return TadBoundaryDefault.of(genomicAssembly.contigById(rs.getInt("CONTIG")),
                Strand.POSITIVE, CoordinateSystem.zeroBased(),
                Position.of(rs.getInt("START")), Position.of(rs.getInt("END")),
                rs.getString("ID"),
                rs.getFloat("STABILITY"));
    }
}
