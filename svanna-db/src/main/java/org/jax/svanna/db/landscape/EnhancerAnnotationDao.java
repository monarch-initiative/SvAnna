package org.jax.svanna.db.landscape;

import org.jax.svanna.core.LogUtils;
import org.jax.svanna.db.IngestDao;
import org.jax.svanna.model.landscape.enhancer.Enhancer;
import org.jax.svanna.model.landscape.enhancer.EnhancerSource;
import org.jax.svanna.model.landscape.enhancer.EnhancerTissueSpecificity;
import org.monarchinitiative.phenol.ontology.data.Term;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.svart.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

public class EnhancerAnnotationDao implements AnnotationDao<Enhancer>, IngestDao<Enhancer> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnhancerAnnotationDao.class);

    private final DataSource dataSource;

    private final GenomicAssembly genomicAssembly;

    private final Map<String, Integer> contigIdMap;

    private final int unknownContigId;

    private final EnhancerParameters enhancerParameters;

    public EnhancerAnnotationDao(DataSource dataSource, GenomicAssembly genomicAssembly) {
        this(dataSource, genomicAssembly, EnhancerParameters.defaultParameters());
    }

    public EnhancerAnnotationDao(DataSource dataSource, GenomicAssembly genomicAssembly, EnhancerParameters enhancerParameters) {
        this.dataSource = dataSource;
        this.genomicAssembly = genomicAssembly;
        this.enhancerParameters = enhancerParameters;

        contigIdMap = new HashMap<>();
        for (Contig contig : genomicAssembly.contigs()) {
            if (contig.equals(Contig.unknown())) continue;

            contigIdMap.put(contig.name(), contig.id());
            contigIdMap.put(contig.genBankAccession(), contig.id());
            contigIdMap.put(contig.refSeqAccession(), contig.id());
            contigIdMap.put(contig.ucscName(), contig.id());
        }
        unknownContigId = Contig.unknown().id();
    }

    @Override
    public int insertItem(Enhancer enhancer) {
        int updated = 0;

        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            String enhancerSql = "insert into SVANNA.ENHANCERS(CONTIG, START, END, " +
                    " ENHANCER_SOURCE, NAME, IS_DEVELOPMENTAL, TAU) " +
                    " VALUES ( ?, ?, ?, ?, ?, ?, ? )";
            String tissueSpecSql = "insert into SVANNA.ENHANCER_TISSUE_SPECIFICITY(ENHANCER_ID, " +
                    " TERM_ID, TERM_LABEL, HPO_ID, HPO_LABEL, SPECIFICITY) " +
                    " VALUES ( ?, ?, ?, ?, ?, ? )";
            try (PreparedStatement enhancerPs = connection.prepareStatement(enhancerSql, Statement.RETURN_GENERATED_KEYS);
                 PreparedStatement tissueSpecPs = connection.prepareStatement(tissueSpecSql)) {

                // insert enhancer
                enhancerPs.setInt(1, contigIdMap.getOrDefault(enhancer.contigName(), unknownContigId));
                enhancerPs.setInt(2, enhancer.startOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased()));
                enhancerPs.setInt(3, enhancer.endOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased()));
                enhancerPs.setString(4, enhancer.enhancerSource().name());
                enhancerPs.setString(5, enhancer.id());
                enhancerPs.setBoolean(6, enhancer.isDevelopmental());
                enhancerPs.setDouble(7, enhancer.tau());

                updated += enhancerPs.executeUpdate();

                int enhancerId;
                try (ResultSet rs = enhancerPs.getGeneratedKeys()) {
                    enhancerId = (rs.last()) ? rs.getInt(1) : 0;
                }

                // insert tissue specificity
                for (EnhancerTissueSpecificity ets : enhancer.tissueSpecificity()) {
                    tissueSpecPs.setInt(1, enhancerId);
                    tissueSpecPs.setString(2, ets.tissueTerm().getId().getValue());
                    tissueSpecPs.setString(3, ets.tissueTerm().getName());
                    tissueSpecPs.setString(4, ets.hpoTerm().getId().getValue());
                    tissueSpecPs.setString(5, ets.hpoTerm().getName());
                    tissueSpecPs.setDouble(6, ets.specificityValue());
                    updated += tissueSpecPs.executeUpdate();
                }
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
    public List<Enhancer> getAllItems() {
        String sql = "select E.ENHANCER_ID, CONTIG, START, END, ENHANCER_SOURCE, NAME, IS_DEVELOPMENTAL, TAU, " +
                " TERM_ID, TERM_LABEL, HPO_ID, HPO_LABEL, SPECIFICITY " +
                " from SVANNA.ENHANCERS E join SVANNA.ENHANCER_TISSUE_SPECIFICITY ETS on E.ENHANCER_ID = ETS.ENHANCER_ID";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            return processEnhancers(preparedStatement);
        } catch (SQLException e) {
            if (LOGGER.isWarnEnabled()) LOGGER.warn("Error occurred: {}", e.getMessage());
            return List.of();
        }
    }

    public Set<TermId> getPhenotypeAssociations() {
        JdbcTemplate template = new JdbcTemplate(dataSource);
        try {
            String sql = "select distinct HPO_ID from SVANNA.ENHANCER_TISSUE_SPECIFICITY";
            return template.query(sql, processResultSet());
        } catch (DataAccessException e) {
            LogUtils.logWarn(LOGGER, "Error getting enhancer phenotype associations: {}", e.getMessage());
            throw e;
        }
    }

    private ResultSetExtractor<Set<TermId>> processResultSet() {
        return rs -> {
            Set<TermId> results = new HashSet<>();
            while (rs.next()) {
                results.add(TermId.of(rs.getString("HPO_ID")));
            }
            return results;
        };
    }

    /**
     * Get enhancers that meet the selection criteria set by {@link EnhancerParameters} and overlap with given
     * {@code query}.
     */
    @Override
    public List<Enhancer> getOverlapping(GenomicRegion query) {
        if (!enhancerParameters.useEnhancers()) {
            return List.of();
        }

        try (Connection connection = dataSource.getConnection()) {
            if (!enhancerParameters.useFantom5()) {
                // just VISTA
                String enhancerSql = "select E.ENHANCER_ID, CONTIG, START, END, ENHANCER_SOURCE, NAME, IS_DEVELOPMENTAL, TAU, " +
                        " TERM_ID, TERM_LABEL, HPO_ID, HPO_LABEL, SPECIFICITY " +
                        "   from SVANNA.ENHANCERS E join SVANNA.ENHANCER_TISSUE_SPECIFICITY ETS on E.ENHANCER_ID = ETS.ENHANCER_ID " +
                        " where E.CONTIG = ? " +
                        "   and ? < E.END " +
                        "   and E.START < ? " +
                        "   and E.IS_DEVELOPMENTAL = true";
                try (PreparedStatement ps = connection.prepareStatement(enhancerSql)) {
                    ps.setInt(1, query.contigId());
                    ps.setInt(2, query.startOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased()));
                    ps.setInt(3, query.endOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased()));
                    return processEnhancers(ps);
                }
            } else {
                // FANTOM5 and maybe VISTA
                String enhancerSql = "select E.ENHANCER_ID, CONTIG, START, END, ENHANCER_SOURCE, NAME, IS_DEVELOPMENTAL, TAU, " +
                        " TERM_ID, TERM_LABEL, HPO_ID, HPO_LABEL, SPECIFICITY " +
                        "   from SVANNA.ENHANCERS E join SVANNA.ENHANCER_TISSUE_SPECIFICITY ETS on E.ENHANCER_ID = ETS.ENHANCER_ID" +
                        " where E.CONTIG = ? " +
                        "   and ? < E.END " +
                        "   and E.START < ? " +
                        "   and (E.IS_DEVELOPMENTAL = ? or (E.IS_DEVELOPMENTAL = false and ETS.SPECIFICITY > ?))";
                try (PreparedStatement ps = connection.prepareStatement(enhancerSql)) {
                    ps.setInt(1, query.contigId());
                    ps.setInt(2, query.startOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased()));
                    ps.setInt(3, query.endOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased()));
                    ps.setBoolean(4, enhancerParameters.useVista());
                    ps.setDouble(5, enhancerParameters.fantom5TissueSpecificity());
                    return processEnhancers(ps);
                }
            }
        } catch (SQLException e) {
            if (LOGGER.isWarnEnabled()) LOGGER.warn("Error occurred: {}", e.getMessage());
            return List.of();
        }
    }

    private List<Enhancer> processEnhancers(PreparedStatement statement) throws SQLException {
        Map<Integer, BaseEnhancer.Builder> builders = new HashMap<>();
        try (ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                int enhancerId = rs.getInt("ENHANCER_ID");
                String enhancerName = rs.getString("NAME");
                int contigId = rs.getInt("CONTIG");
                Contig contig = genomicAssembly.contigById(contigId);
                if (contig == Contig.unknown()) {
                    if (LOGGER.isWarnEnabled())
                        LOGGER.warn("Unknown contig id {} for enhancer {}({})", contigId, enhancerName, enhancerId);
                    continue;
                }

                if (!builders.containsKey(enhancerId)) {
                    BaseEnhancer.Builder builder = BaseEnhancer.builder().with(contig,
                            Strand.POSITIVE, Coordinates.of(CoordinateSystem.zeroBased(), // database invariant
                            rs.getInt("START"), rs.getInt("END")))
                            .enhancerSource(EnhancerSource.valueOf(rs.getString("ENHANCER_SOURCE")))
                            .id(enhancerName)
                            .isDevelopmental(rs.getBoolean("IS_DEVELOPMENTAL"))
                            .tau(rs.getDouble("TAU"));
                    builders.put(enhancerId, builder);
                }

                builders.get(enhancerId).addSpecificity(
                        EnhancerTissueSpecificity.of(
                                Term.of(TermId.of(rs.getString("TERM_ID")), rs.getString("TERM_LABEL")),
                                Term.of(TermId.of(rs.getString("HPO_ID")), rs.getString("HPO_LABEL")),
                                rs.getDouble("SPECIFICITY")));
            }
        }
        return builders.values().stream()
                .map(BaseEnhancer.Builder::build)
                .collect(Collectors.toList());
    }

    public static class EnhancerParameters {

        private static final EnhancerParameters DEFAULT_PARAMETERS = new EnhancerParameters(true, false, Double.NaN);
        private final boolean useVista;
        private final boolean useFantom5;
        private final double fantom5TissueSpecificity;

        public static EnhancerParameters of(boolean useVista, boolean useFantom5, double fantom5Specificity) {
            return new EnhancerParameters(useVista, useFantom5, fantom5Specificity);
        }

        private EnhancerParameters(boolean useVista, boolean useFantom5, double fantom5TissueSpecificity) {
            this.useVista = useVista;
            this.useFantom5 = useFantom5;
            this.fantom5TissueSpecificity = fantom5TissueSpecificity;
        }

        public boolean useEnhancers() {
            return useVista || useFantom5;
        }

        public boolean useVista() {
            return useVista;
        }

        public boolean useFantom5() {
            return useFantom5;
        }

        public double fantom5TissueSpecificity() {
            return fantom5TissueSpecificity;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            EnhancerParameters that = (EnhancerParameters) o;
            return useVista == that.useVista && useFantom5 == that.useFantom5 && Double.compare(that.fantom5TissueSpecificity, fantom5TissueSpecificity) == 0;
        }

        @Override
        public int hashCode() {
            return Objects.hash(useVista, useFantom5, fantom5TissueSpecificity);
        }

        @Override
        public String toString() {
            return "EnhancerParameters{" +
                    "useVista=" + useVista +
                    ", useFantom5=" + useFantom5 +
                    ", fantom5Specificity=" + fantom5TissueSpecificity +
                    '}';
        }

        public static EnhancerParameters defaultParameters() {
            return DEFAULT_PARAMETERS;
        }
    }
}
