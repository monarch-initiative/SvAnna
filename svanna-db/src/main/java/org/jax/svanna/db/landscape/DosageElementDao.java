package org.jax.svanna.db.landscape;

import org.jax.svanna.db.IngestDao;
import org.jax.svanna.model.landscape.dosage.DosageElement;
import org.jax.svanna.model.landscape.dosage.DosageSensitivity;
import org.jax.svanna.model.landscape.dosage.DosageSensitivityEvidence;
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

public class DosageElementDao implements AnnotationDao<DosageElement>, IngestDao<DosageElement> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DosageElementDao.class);

    private final DataSource dataSource;
    private final GenomicAssembly genomicAssembly;

    public DosageElementDao(DataSource dataSource, GenomicAssembly genomicAssembly) {
        this.dataSource = dataSource;
        this.genomicAssembly = genomicAssembly;
    }

    @Override
    public int insertItem(DosageElement item) {
        int updated = 0;

        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            String sql = "insert into SVANNA.DOSAGE_ELEMENT(" +
                    " CONTIG, START, END, " +
                    " ID, DOSAGE_SENSITIVITY, DOSAGE_EVIDENCE) " +
                    " VALUES ( ?, ?, ?, ?, ?, ? )";
            try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                preparedStatement.setInt(1, item.contigId());
                preparedStatement.setInt(2, item.startOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased()));
                preparedStatement.setInt(3, item.endOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased()));
                preparedStatement.setString(4, item.id());
                preparedStatement.setString(5, item.dosageSensitivity().name());
                preparedStatement.setString(6, item.dosageSensitivityEvidence().name());

                updated += preparedStatement.executeUpdate();
                connection.commit();
            } catch (SQLException e) {
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
    public List<DosageElement> getOverlapping(GenomicRegion query) {
        String sql = "select CONTIG, START, END, ID, DOSAGE_SENSITIVITY, DOSAGE_EVIDENCE " +
                " from SVANNA.DOSAGE_ELEMENT " +
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

    private List<DosageElement> processStatement(PreparedStatement preparedStatement) throws SQLException {
        List<DosageElement> regions = new ArrayList<>();
        try (ResultSet rs = preparedStatement.executeQuery()) {
            while (rs.next()) {
                Contig contig = genomicAssembly.contigById(rs.getInt("CONTIG"));
                if (contig == Contig.unknown()) {
                    if (LOGGER.isWarnEnabled()) LOGGER.warn("Unknown contig id `{}`", rs.getInt("CONTIG"));
                    continue;
                }
                Coordinates coordinates = Coordinates.of(CoordinateSystem.zeroBased(), // database invariant
                        rs.getInt("START"), rs.getInt("END"));
                regions.add(DosageElement.of(contig,
                        Strand.POSITIVE, coordinates,
                        rs.getString("ID"),
                        DosageSensitivity.valueOf(rs.getString("DOSAGE_SENSITIVITY")),
                        DosageSensitivityEvidence.valueOf(rs.getString("DOSAGE_EVIDENCE"))
                ));
            }
        }
        return regions;
    }

    @Override
    public List<DosageElement> getAllItems() {
        return List.of();
    }
}
