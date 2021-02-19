package org.jax.svanna.db.landscape;

import org.jax.svanna.core.landscape.AnnotationDao;
import org.jax.svanna.core.landscape.RepeatFamily;
import org.jax.svanna.core.landscape.RepetitiveRegion;
import org.jax.svanna.db.IngestDao;
import org.monarchinitiative.svart.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class RepetitiveRegionDao implements AnnotationDao<RepetitiveRegion>, IngestDao<RepetitiveRegion> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RepetitiveRegionDao.class);

    private final DataSource dataSource;

    private final GenomicAssembly genomicAssembly;

    public RepetitiveRegionDao(DataSource dataSource, GenomicAssembly genomicAssembly) {
        this.dataSource = dataSource;
        this.genomicAssembly = genomicAssembly;
    }


    @Override
    public List<RepetitiveRegion> getAllItems() {
        String sql = "select CONTIG, START, END, REPEAT_FAMILY from SVANNA.REPETITIVE_REGIONS";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            return processStatement(preparedStatement);
        } catch (SQLException e) {
            if (LOGGER.isWarnEnabled()) LOGGER.warn("Error occurred: {}", e.getMessage());
            return List.of();
        }
    }

    @Override
    public List<RepetitiveRegion> getOverlapping(GenomicRegion query) {
        String sql = "select CONTIG, START, END, REPEAT_FAMILY " +
                " from SVANNA.REPETITIVE_REGIONS " +
                "  where CONTIG = ? " +
                "    and ? < END " +
                "    and START < ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, query.contigId());
            preparedStatement.setInt(2, query.startOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased()));
            preparedStatement.setInt(3, query.endOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased()));
            return processStatement(preparedStatement);
        } catch (SQLException e) {
            if (LOGGER.isWarnEnabled()) LOGGER.warn("Error occurred: {}", e.getMessage());
            return List.of();
        }
    }

    private List<RepetitiveRegion> processStatement(PreparedStatement preparedStatement) throws SQLException {
        List<RepetitiveRegion> regions = new ArrayList<>();
        try (ResultSet rs = preparedStatement.executeQuery()) {
            while (rs.next()) {
                Contig contig = genomicAssembly.contigById(rs.getInt("CONTIG"));
                if (contig == Contig.unknown()) {
                    if (LOGGER.isWarnEnabled()) LOGGER.warn("Unknown contig id `{}`", rs.getInt("CONTIG"));
                    continue;
                }
                regions.add(RepetitiveRegion.of(contig,
                        Strand.POSITIVE, CoordinateSystem.zeroBased(), // database invariant
                        Position.of(rs.getInt("START")), Position.of(rs.getInt("END")),
                        RepeatFamily.valueOf(rs.getString("REPEAT_FAMILY"))));
            }
        }
        return regions;
    }

    @Override
    public int insertItem(RepetitiveRegion item) {
        int updated = 0;

        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            String sql = "insert into SVANNA.REPETITIVE_REGIONS(CONTIG, START, END, REPEAT_FAMILY) " +
                    "VALUES ( ?, ?, ?, ? )";
            try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                preparedStatement.setInt(1, item.contigId());
                preparedStatement.setInt(2, item.startOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased()));
                preparedStatement.setInt(3, item.endOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased()));
                preparedStatement.setString(4, item.repeatFamily().toString());

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
}
