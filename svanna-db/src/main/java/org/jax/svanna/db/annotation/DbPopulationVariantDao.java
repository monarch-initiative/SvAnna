package org.jax.svanna.db.annotation;

import org.jax.svanna.core.annotation.PopulationVariantDao;
import org.jax.svanna.core.reference.BasePopulationVariant;
import org.jax.svanna.core.reference.PopulationVariant;
import org.jax.svanna.core.reference.PopulationVariantOrigin;
import org.jax.svanna.db.IngestDao;
import org.monarchinitiative.svart.*;
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

    @Override
    public List<PopulationVariant> getAllItems() {
        throw new UnsupportedOperationException("Loading all population variants into memory is not supported");
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
                regions.add(BasePopulationVariant.of(contig,
                        Strand.POSITIVE, CoordinateSystem.zeroBased(),
                        Position.of(rs.getInt("START")), Position.of(rs.getInt("END")),
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
            String sql = "insert into SVANNA.POPULATION_VARIANTS(CONTIG, START, END, " +
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

        String sql = "select CONTIG, START, END, ID, VARIANT_TYPE, ORIGIN, ALLELE_FREQUENCY " +
                " from SVANNA.POPULATION_VARIANTS " +
                "  where CONTIG = ? " +
                "    and ? < END " +
                "    and START < ?";
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
