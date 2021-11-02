package org.jax.svanna.db.landscape;

import org.jax.svanna.core.landscape.TadBoundary;
import org.jax.svanna.core.landscape.TadBoundaryDefault;
import org.jax.svanna.db.IngestDao;
import org.monarchinitiative.svart.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public class TadBoundaryDao implements IngestDao<TadBoundary>, AnnotationDao<TadBoundary> {

    private static final Logger LOGGER = LoggerFactory.getLogger(TadBoundaryDao.class);

    private static final CoordinateSystem CS = CoordinateSystem.zeroBased();

    private final DataSource dataSource;

    private final GenomicAssembly genomicAssembly;

    private final NamedParameterJdbcTemplate template;

    // stability threshold as a fraction - in range [0,1]
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
            String sql = "insert into SVANNA.TAD_BOUNDARY(CONTIG, START, END, MIDPOINT, ID, STABILITY) " +
                    "VALUES ( ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                preparedStatement.setInt(1, item.contigId());
                int start = item.startOnStrandWithCoordinateSystem(Strand.POSITIVE, CS);
                int midpoint = start + item.length() / 2;
                preparedStatement.setInt(2, start);
                preparedStatement.setInt(3, item.endOnStrandWithCoordinateSystem(Strand.POSITIVE, CS));
                preparedStatement.setInt(4, midpoint);
                preparedStatement.setString(5, item.id());
                preparedStatement.setFloat(6, item.stability());

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
        String sql = "select CONTIG, MIDPOINT, ID, STABILITY " +
                " from SVANNA.TAD_BOUNDARY " +
                " where CONTIG = :contig " +
                "   and :start < MIDPOINT " +
                "   and MIDPOINT <= :end " +
                "   and STABILITY >= :stability" +
                " order by MIDPOINT";
        SqlParameterSource paramsSource = new MapSqlParameterSource()
                .addValue("contig", query.contigId())
                .addValue("start", query.startOnStrandWithCoordinateSystem(Strand.POSITIVE, CS))
                .addValue("end", query.endOnStrandWithCoordinateSystem(Strand.POSITIVE, CS))
                .addValue("stability", stabilityThreshold);
        return template.query(sql, paramsSource, processResults());
    }

    /**
     * Get the closest upstream TAD boundary or empty optional if no such TAD boundary exists. The returned TAD boundary
     * is adjusted to <code>region</code>'s strand.
     */
    public Optional<TadBoundary> upstreamOf(GenomicRegion region) {
        SqlParameterSource paramSource = new MapSqlParameterSource()
                .addValue("contig", region.contigId())
                .addValue("position", region.startOnStrandWithCoordinateSystem(Strand.POSITIVE, CS))
                .addValue("stability", stabilityThreshold);
        String sql = region.strand().isPositive()
                ? "select top 1 CONTIG, MIDPOINT, ID, STABILITY " +
                "  from SVANNA.TAD_BOUNDARY " +
                "    where CONTIG = :contig and MIDPOINT < :position and STABILITY >= :stability " +
                "  order by MIDPOINT DESC"

                : "select top 1 CONTIG, MIDPOINT, ID, STABILITY " +
                "  from SVANNA.TAD_BOUNDARY " +
                "    where CONTIG = :contig and MIDPOINT > :position and STABILITY >= :stability " +
                "  order by MIDPOINT";
        return template.query(sql, paramSource, mapToTadBoundary(region.strand()));
    }

    /**
     * Get the closest downstream TAD boundary or empty optional if no such TAD boundary exists. The returned TAD boundary
     * is adjusted to <code>region</code>'s strand.
     */
    public Optional<TadBoundary> downstreamOf(GenomicRegion region) {
        SqlParameterSource paramSource = new MapSqlParameterSource()
                .addValue("contig", region.contigId())
                .addValue("position", region.endOnStrandWithCoordinateSystem(Strand.POSITIVE, CS))
                .addValue("stability", stabilityThreshold);
        String sql = region.strand().isPositive()
                ? "select top 1 CONTIG, MIDPOINT, ID, STABILITY " +
                "  from SVANNA.TAD_BOUNDARY " +
                "    where CONTIG = :contig and MIDPOINT > :position and STABILITY >= :stability " +
                "  order by MIDPOINT"
                : "select top 1 CONTIG, MIDPOINT, ID, STABILITY " +
                "  from SVANNA.TAD_BOUNDARY " +
                "    where CONTIG = :contig and MIDPOINT < :position and STABILITY >= :stability " +
                "  order by MIDPOINT DESC";
        return template.query(sql, paramSource, mapToTadBoundary(region.strand()));
    }

    private ResultSetExtractor<List<TadBoundary>> processResults() {
        return (rs) -> {
            List<TadBoundary> boundaries = new LinkedList<>();
            while (rs.next()) {
                boundaries.add(mapRowToTadBoundary(rs, Strand.POSITIVE));
            }
            return boundaries;
        };
    }

    private ResultSetExtractor<Optional<TadBoundary>> mapToTadBoundary(Strand strand) {
        return rs -> {
            if (rs.first()) {
                return Optional.of(mapRowToTadBoundary(rs, strand));
            }
            return Optional.empty();
        };
    }

    private TadBoundary mapRowToTadBoundary(ResultSet rs, Strand strand) throws SQLException {
        Contig contig = genomicAssembly.contigById(rs.getInt("CONTIG"));
        int midpoint = rs.getInt("MIDPOINT");

        int pos = strand.isPositive()
                ? midpoint
                : Coordinates.invertPosition(CS, contig, midpoint);
        return TadBoundaryDefault.of(contig,
                Strand.POSITIVE, CS,
                pos, pos,
                rs.getString("ID"),
                rs.getFloat("STABILITY"));
    }
}
