package org.jax.svanna.db.landscape;

import org.jax.svanna.db.IngestDao;
import org.jax.svanna.model.landscape.dosage.*;
import org.monarchinitiative.svart.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class ClingenDosageElementDao implements AnnotationDao<DosageRegion>, IngestDao<DosageRegion> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClingenDosageElementDao.class);

    private final DataSource dataSource;
    private final GenomicAssembly genomicAssembly;

    public ClingenDosageElementDao(DataSource dataSource, GenomicAssembly genomicAssembly) {
        this.dataSource = dataSource;
        this.genomicAssembly = genomicAssembly;
    }

    private static List<Dosage> processDosageDataStatement(PreparedStatement preparedStatement) throws SQLException {
        List<Dosage> regions = new LinkedList<>();
        try (ResultSet rs = preparedStatement.executeQuery()) {
            while (rs.next()) {
                Dosage dosageData = Dosage.of(rs.getString("ID"),
                        DosageSensitivity.valueOf(rs.getString("DOSAGE_SENSITIVITY")),
                        DosageSensitivityEvidence.valueOf(rs.getString("DOSAGE_EVIDENCE")));

                regions.add(dosageData);
            }
        }

        return regions;
    }

    @Override
    public int insertItem(DosageRegion item) {
        int updated = 0;

        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            String sql = "insert into SVANNA.CLINGEN_DOSAGE_ELEMENT(" +
                    " CONTIG, START, END, " +
                    " ID, DOSAGE_SENSITIVITY, DOSAGE_EVIDENCE) " +
                    " VALUES ( ?, ?, ?, ?, ?, ? )";
            try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                preparedStatement.setInt(1, item.contigId());
                preparedStatement.setInt(2, item.startOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased()));
                preparedStatement.setInt(3, item.endOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased()));

                preparedStatement.setString(4, item.dosage().id());
                preparedStatement.setString(5, item.dosage().dosageSensitivity().name());
                preparedStatement.setString(6, item.dosage().dosageSensitivityEvidence().name());

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
    public List<DosageRegion> getOverlapping(GenomicRegion query) {
        String sql = "select CONTIG, START, END, ID, DOSAGE_SENSITIVITY, DOSAGE_EVIDENCE " +
                " from SVANNA.CLINGEN_DOSAGE_ELEMENT " +
                "  where CONTIG = ? " +
                "    and ? < END " +
                "    and START < ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, query.contigId());
            preparedStatement.setInt(2, query.startOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased()));
            preparedStatement.setInt(3, query.endOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased()));
            return processDosageRegionStatement(preparedStatement);
        } catch (SQLException e) {
            if (LOGGER.isWarnEnabled()) LOGGER.warn("Error occurred: {}", e.getMessage());
            return List.of();
        }
    }

    public List<Dosage> geneDosageDataForHgncId(String hgncId) {
        String sql = "select ID, DOSAGE_SENSITIVITY, DOSAGE_EVIDENCE " +
                " from SVANNA.CLINGEN_DOSAGE_ELEMENT " +
                "  where ID = ?";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, hgncId);
            return processDosageDataStatement(preparedStatement);
        } catch (SQLException e) {
            if (LOGGER.isWarnEnabled()) LOGGER.warn("Error occurred: {}", e.getMessage());
            return List.of();
        }
    }

    public List<Dosage> geneDosageDataForHgncIdAndRegion(String hgncId, GenomicRegion query) {
        String sql = "select distinct ID, DOSAGE_SENSITIVITY, DOSAGE_EVIDENCE " +
                " from SVANNA.CLINGEN_DOSAGE_ELEMENT " +
                "  where (CONTIG = ? " +
                "      and ? < END " +
                "      and START < ?) " +
                "    or ID = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, query.contigId());
            preparedStatement.setInt(2, query.startOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased()));
            preparedStatement.setInt(3, query.endOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased()));
            preparedStatement.setString(4, hgncId);
            return processDosageDataStatement(preparedStatement);
        } catch (SQLException e) {
            if (LOGGER.isWarnEnabled()) LOGGER.warn("Error occurred: {}", e.getMessage());
            return List.of();
        }
    }

    private List<DosageRegion> processDosageRegionStatement(PreparedStatement preparedStatement) throws SQLException {
        try (ResultSet rs = preparedStatement.executeQuery()) {
            List<DosageRegion> dosageRegions = new LinkedList<>();

            while (rs.next()) {
                Contig contig = genomicAssembly.contigById(rs.getInt("CONTIG"));
                if (contig == Contig.unknown()) {
                    if (LOGGER.isWarnEnabled()) LOGGER.warn("Unknown contig id `{}`", rs.getInt("CONTIG"));
                    continue;
                }
                Coordinates coordinates = Coordinates.of(CoordinateSystem.zeroBased(), // database invariant
                        rs.getInt("START"), rs.getInt("END"));
                GenomicRegion location = GenomicRegion.of(contig, Strand.POSITIVE, coordinates);
                Dosage dosage = Dosage.of(rs.getString("ID"),
                        DosageSensitivity.valueOf(rs.getString("DOSAGE_SENSITIVITY")),
                        DosageSensitivityEvidence.valueOf(rs.getString("DOSAGE_EVIDENCE")));

                dosageRegions.add(DosageRegion.of(location, dosage));
            }

            return dosageRegions;
        }
    }

    @Override
    public List<DosageRegion> getAllItems() {
        return List.of();
    }
}
