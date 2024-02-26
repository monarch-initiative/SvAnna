package org.monarchinitiative.svanna.db.landscape;

import org.monarchinitiative.svanna.db.IngestDao;
import org.monarchinitiative.svanna.model.landscape.variant.BasePopulationVariant;
import org.monarchinitiative.svanna.model.landscape.variant.PopulationVariant;
import org.monarchinitiative.svanna.model.landscape.variant.PopulationVariantOrigin;
import org.monarchinitiative.svart.*;
import org.monarchinitiative.svart.assembly.GenomicAssembly;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DbPopulationVariantDao implements PopulationVariantDao, IngestDao<PopulationVariant> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DbPopulationVariantDao.class);

    private final DataSource dataSource;
    private final GenomicAssembly genomicAssembly;

    private final Set<PopulationVariantOrigin> origins;

    public DbPopulationVariantDao(DataSource dataSource, GenomicAssembly genomicAssembly) {
        this.dataSource = dataSource;
        this.genomicAssembly = genomicAssembly;
        this.origins = readOrigins();
    }

    private Set<PopulationVariantOrigin> readOrigins() {
        String sql = "select distinct ORIGIN from SVANNA.POPULATION_VARIANTS";
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            Set<PopulationVariantOrigin> builder = new HashSet<>();
            while (rs.next()) {
                builder.add(PopulationVariantOrigin.valueOf(rs.getString("ORIGIN")));
            }
            return Set.copyOf(builder);
        } catch (SQLException e) {
            if (LOGGER.isWarnEnabled()) LOGGER.warn("Error occurred: {}", e.getMessage());
            return Set.of();
        }
    }

    private List<PopulationVariant> processStatement(PreparedStatement preparedStatement, Set<PopulationVariantOrigin> origins) throws SQLException {
        List<PopulationVariant> regions = new ArrayList<>();
        try (ResultSet rs = preparedStatement.executeQuery()) {
            while (rs.next()) {
                PopulationVariantOrigin origin = PopulationVariantOrigin.valueOf(rs.getString("ORIGIN"));
                if (!origins.contains(origin)) continue;
                Contig contig = genomicAssembly.contigById(rs.getInt("CONTIG"));
                if (contig == Contig.unknown()) {
                    if (LOGGER.isWarnEnabled()) LOGGER.warn("Unknown contig id `{}`", rs.getInt("CONTIG"));
                    continue;
                }
                regions.add(
                        BasePopulationVariant.of(
                                GenomicRegion.of(contig, Strand.POSITIVE, CoordinateSystem.zeroBased(), rs.getInt("START_POS"), rs.getInt("END_POS")),
                                rs.getString("ID"), VariantType.valueOf(rs.getString("VARIANT_TYPE")),
                                rs.getFloat("ALLELE_FREQUENCY"), origin));
            }
        }
        return regions;
    }

    @Override
    public int insertItem(PopulationVariant item) {
        int updated = 0;

        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            String sql = "insert into SVANNA.POPULATION_VARIANTS(CONTIG, START_POS, END_POS, " +
                    "ID, VARIANT_TYPE, ORIGIN, ALLELE_FREQUENCY) " +
                    "VALUES ( ?, ?, ?, ?, ?, ?, ? )";
            try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                preparedStatement.setInt(1, item.contigId());
                preparedStatement.setInt(2, item.startOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased()));
                preparedStatement.setInt(3, item.endOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased()));
                preparedStatement.setString(4, item.id());
                preparedStatement.setString(5, item.variantType().name());
                preparedStatement.setString(6, item.origin().name());
                preparedStatement.setFloat(7, item.alleleFrequency());

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
    public Set<PopulationVariantOrigin> availableOrigins() {
        return origins;
    }

    @Override
    public List<PopulationVariant> getOverlapping(GenomicRegion query, Set<PopulationVariantOrigin> origins) {

        String sql = "select CONTIG, START_POS, END_POS, ID, VARIANT_TYPE, ORIGIN, ALLELE_FREQUENCY " +
                " from SVANNA.POPULATION_VARIANTS " +
                "  where CONTIG = ? " +
                "    and ? < END_POS " +
                "    and START_POS < ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, query.contigId());
            preparedStatement.setInt(2, query.startOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased()));
            preparedStatement.setInt(3, query.endOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased()));
            return processStatement(preparedStatement, origins);
        } catch (SQLException e) {
            if (LOGGER.isWarnEnabled()) LOGGER.warn("Error occurred: {}", e.getMessage());
            return List.of();
        }
    }
}
