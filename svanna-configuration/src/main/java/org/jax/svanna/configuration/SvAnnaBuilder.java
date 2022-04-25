package org.jax.svanna.configuration;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jax.svanna.configuration.exception.InvalidResourceException;
import org.jax.svanna.configuration.exception.MissingResourceException;
import org.jax.svanna.configuration.exception.UndefinedResourceException;
import org.jax.svanna.core.SvAnna;
import org.jax.svanna.core.configuration.DataProperties;
import org.jax.svanna.core.configuration.SvAnnaProperties;
import org.jax.svanna.core.hpo.*;
import org.jax.svanna.core.priority.SvPrioritizerFactory;
import org.jax.svanna.core.service.AnnotationDataService;
import org.jax.svanna.core.service.GeneDosageDataService;
import org.jax.svanna.core.service.GeneService;
import org.jax.svanna.core.service.PhenotypeDataService;
import org.jax.svanna.db.gene.GeneDiseaseDao;
import org.jax.svanna.db.landscape.*;
import org.jax.svanna.db.phenotype.MicaDao;
import org.jax.svanna.db.service.ClinGenGeneDosageDataService;
import org.jax.svanna.io.hpo.DbPhenotypeDataService;
import org.jax.svanna.io.service.SilentGenesGeneService;
import org.jax.svanna.model.HpoDiseaseSummary;
import org.monarchinitiative.phenol.io.OntologyLoader;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.sgenes.model.GeneIdentifier;
import org.monarchinitiative.svart.assembly.GenomicAssemblies;
import org.monarchinitiative.svart.assembly.GenomicAssembly;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class SvAnnaBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(SvAnnaBuilder.class);

    private static final NumberFormat NF = NumberFormat.getNumberInstance();

    static {
        NF.setMaximumFractionDigits(2);
    }

    private final SvAnnaProperties properties;
    private final SvannaDataResolver dataResolver;
    private GenomicAssembly genomicAssembly = GenomicAssemblies.GRCh38p13();
    private GeneService geneService;
    private PhenotypeDataService phenotypeDataService;
    private AnnotationDataService annotationDataService;
    private SvPrioritizerFactory svPrioritizerFactory;

    private SvAnnaBuilder(SvAnnaProperties properties) throws MissingResourceException {
        this.properties = Objects.requireNonNull(properties);
        this.dataResolver = new SvannaDataResolver(properties.dataDirectory());
    }

    public static SvAnnaBuilder builder(SvAnnaProperties properties) throws MissingResourceException {
        return new SvAnnaBuilder(properties);
    }

    public SvAnnaBuilder genomicAssembly(GenomicAssembly assembly) {
        this.genomicAssembly = assembly;
        return this;
    }

    public SvAnnaBuilder geneService(GeneService geneService) {
        this.geneService = geneService;
        return this;
    }

    public SvAnnaBuilder phenotypeDataService(PhenotypeDataService phenotypeDataService) {
        this.phenotypeDataService = phenotypeDataService;
        return this;
    }

    public SvAnnaBuilder annotationDataService(AnnotationDataService annotationDataService) {
        this.annotationDataService = annotationDataService;
        return this;
    }

    public SvAnnaBuilder svPrioritizerFactory(SvPrioritizerFactory svPrioritizerFactory) {
        this.svPrioritizerFactory = svPrioritizerFactory;
        return this;
    }

    public SvAnna build() throws UndefinedResourceException, InvalidResourceException {
        // Let's build SvAnna components.
        // 1 - genomic assembly is given
        if (genomicAssembly == null)
            throw new UndefinedResourceException("Genomic assembly must not be null");

        // 2 - GeneService ---------------------------------------------------------------------------------------------
        if (geneService == null) {
            LOGGER.debug("Reading genes from {}", dataResolver.genesJsonPath());
            try {
                geneService = SilentGenesGeneService.of(genomicAssembly, dataResolver.genesJsonPath());
            } catch (IOException e) {
                throw new InvalidResourceException("Error reading genes from `" + dataResolver.genesJsonPath().toAbsolutePath() + "`", e);
            }
        }

        // 3 - PhenotypeDataService ------------------------------------------------------------------------------------
        DataSource dataSource = null;
        if (phenotypeDataService == null) {
            dataSource = svAnnaDataSource(dataResolver.dataSourcePath());
            LOGGER.debug("Reading HPO file from {}", dataResolver.hpOntologyPath().toAbsolutePath());
            Ontology ontology = OntologyLoader.loadOntology(dataResolver.hpOntologyPath().toFile());

            GeneDiseaseDao geneDiseaseDao = new GeneDiseaseDao(dataSource);
            List<GeneIdentifier> geneIdentifiers = geneDiseaseDao.geneIdentifiers();
            Map<String, List<HpoDiseaseSummary>> hgncGeneIdToDiseases = geneDiseaseDao.hgncGeneIdToDiseases();
            Map<String, List<TermId>> phenotypicAbnormalitiesForDiseaseId = geneDiseaseDao.diseaseToPhenotypes();
            phenotypeDataService = new DbPhenotypeDataService(ontology, geneIdentifiers, hgncGeneIdToDiseases, phenotypicAbnormalitiesForDiseaseId);
        }

        // 4 - AnnotationDataService -----------------------------------------------------------------------------------
        if (annotationDataService == null) {
            if (dataSource == null)
                dataSource = svAnnaDataSource(dataResolver.dataSourcePath());

            DataProperties dataProperties = properties.dataProperties();
            LOGGER.debug("Including TAD boundaries with stability >{}%", NF.format(dataProperties.tadStabilityThresholdAsPercentage()));

            if (dataProperties.useVista())
                LOGGER.debug("Including VISTA enhancers");
            if (dataProperties.useFantom5())
                LOGGER.debug("Including FANTOM5 enhancers with tissue specificity >{}", dataProperties.fantom5TissueSpecificity());

            EnhancerAnnotationDao.EnhancerParameters enhancerParameters = EnhancerAnnotationDao.EnhancerParameters.of(dataProperties.useVista(),
                    dataProperties.useFantom5(),
                    dataProperties.fantom5TissueSpecificity());

            LOGGER.debug("Using `clingen` gene dosage source");
            ClingenDosageElementDao clingenDosageElementDao = new ClingenDosageElementDao(dataSource, genomicAssembly);
            GeneDosageDataService geneDosageDataService = new ClinGenGeneDosageDataService(clingenDosageElementDao);

            annotationDataService = new DbAnnotationDataService(
                    new EnhancerAnnotationDao(dataSource, genomicAssembly, enhancerParameters),
                    new RepetitiveRegionDao(dataSource, genomicAssembly),
                    new DbPopulationVariantDao(dataSource, genomicAssembly),
                    new TadBoundaryDao(dataSource, genomicAssembly, dataProperties.tadStabilityThresholdAsFraction()),
                    geneDosageDataService);
        }

        // 5 - SvPrioritizerFactory ------------------------------------------------------------------------------------
        if (svPrioritizerFactory == null) {
            if (dataSource == null)
                dataSource = svAnnaDataSource(dataResolver.dataSourcePath());

            SimilarityScoreCalculator similarityScoreCalculator;
            TermSimilarityMeasure similarityMeasure = properties.prioritizationProperties().termSimilarityMeasure();
            LOGGER.debug("Initializing phenotype term similarity calculator {}", similarityMeasure);

            MicaCalculator similarityCalculator = prepareMicaCalculator(dataSource, properties.prioritizationProperties().icMicaMode());
            if (similarityMeasure.equals(TermSimilarityMeasure.RESNIK_SYMMETRIC)) {
                similarityScoreCalculator = new ResnikSimilarityScoreCalculator(similarityCalculator, true);
            } else if (similarityMeasure.equals(TermSimilarityMeasure.RESNIK_ASYMMETRIC)) {
                similarityScoreCalculator = new ResnikSimilarityScoreCalculator(similarityCalculator, false);
            } else {
                throw new UndefinedResourceException("Unknown term similarity measure " + similarityMeasure);
            }

            svPrioritizerFactory = new SvPrioritizerFactoryImpl(genomicAssembly,
                    dataSource,
                    properties,
                    annotationDataService,
                    geneService,
                    phenotypeDataService,
                    similarityScoreCalculator);
        }

        // We're done!
        return SvAnna.of(genomicAssembly, geneService, phenotypeDataService, annotationDataService, svPrioritizerFactory);
    }

    private static DataSource svAnnaDataSource(Path svAnnaDataSourcePath) {
        String jdbcUrl = String.format("jdbc:h2:file:%s;ACCESS_MODE_DATA=r", svAnnaDataSourcePath.toFile().getAbsolutePath());
        HikariConfig config = new HikariConfig();
        config.setUsername("sa");
        config.setPassword("sa");
        config.setDriverClassName("org.h2.Driver");
        config.setJdbcUrl(jdbcUrl);
        config.setPoolName("svanna-pool");

        return new HikariDataSource(config);
    }

    private static MicaCalculator prepareMicaCalculator(DataSource svannaDatasource,
                                                        IcMicaMode icMicaMode) {
        MicaDao dao = new MicaDao(svannaDatasource);
        switch (icMicaMode) {
            case IN_MEMORY:
                LOGGER.debug("Using `{}` to get IC of the most informative common ancestor for HPO terms", icMicaMode);
                return new InMemoryMicaCalculator(dao.getAllMicaValues());
            default:
                LOGGER.warn("Unknown value `{}` for getting IC of the most informative common ancestor for HPO terms. Falling back to DATABASE", icMicaMode);
            case DATABASE:
                LOGGER.debug("Using `{}` to get IC of the most informative common ancestor for HPO terms", icMicaMode);
                return (a, b) -> dao.getMica(TermPair.symmetric(a, b));
        }
    }
}
