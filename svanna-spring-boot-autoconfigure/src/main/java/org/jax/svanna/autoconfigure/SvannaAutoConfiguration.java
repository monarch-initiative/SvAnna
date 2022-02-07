package org.jax.svanna.autoconfigure;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jax.svanna.autoconfigure.configuration.DataProperties;
import org.jax.svanna.autoconfigure.configuration.EnhancerProperties;
import org.jax.svanna.autoconfigure.configuration.PrioritizationProperties;
import org.jax.svanna.autoconfigure.configuration.SvannaProperties;
import org.jax.svanna.autoconfigure.exception.InvalidResourceException;
import org.jax.svanna.autoconfigure.exception.MissingResourceException;
import org.jax.svanna.autoconfigure.exception.UndefinedResourceException;
import org.jax.svanna.core.hpo.*;
import org.jax.svanna.core.overlap.GeneOverlapper;
import org.jax.svanna.core.priority.SvPrioritizerFactory;
import org.jax.svanna.core.service.*;
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
import org.monarchinitiative.svart.assembly.GenomicAssemblies;
import org.monarchinitiative.svart.assembly.GenomicAssembly;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.monarchinitiative.sgenes.model.GeneIdentifier;

import javax.sql.DataSource;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.util.*;

@Configuration
@EnableConfigurationProperties({
        SvannaProperties.class,
        DataProperties.class,
        EnhancerProperties.class,
        PrioritizationProperties.class
})
public class SvannaAutoConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(SvannaAutoConfiguration.class);

    private static final NumberFormat NF = NumberFormat.getNumberInstance();
    private static final Properties properties = readProperties();
    private static final String SVANNA_VERSION = properties.getProperty("svanna.version", "unknown version");

    static {
        NF.setMaximumFractionDigits(2);
    }

    private static MicaCalculator prepareMicaCalculator(DataSource svannaDatasource,
                                                        PrioritizationProperties.IcMicaMode icMicaMode) {
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

    private static Properties readProperties() {
        Properties properties = new Properties();

        try (InputStream is = SvannaAutoConfiguration.class.getResourceAsStream("/svanna.properties")) {
            properties.load(is);
        } catch (IOException e) {
            LOGGER.warn("Error loading properties: {}", e.getMessage());
        }
        return properties;
    }

    @Bean
    @ConditionalOnMissingBean(name = "svannaDataDirectory")
    public Path svannaDataDirectory(SvannaProperties properties) throws UndefinedResourceException {
        String dataDir = properties.dataDirectory();
        if (dataDir == null || dataDir.isEmpty()) {
            throw new UndefinedResourceException("Path to SvAnna data directory (`--svanna.data-directory`) is not specified");
        }
        Path dataDirPath = Paths.get(dataDir);
        if (!Files.isDirectory(dataDirPath)) {
            throw new UndefinedResourceException(String.format("Path to SvAnna data directory '%s' does not point to real directory", dataDirPath));
        }
        LOGGER.info("Spooling up SvAnna v{} using resources in {}", SVANNA_VERSION, dataDirPath.toAbsolutePath());
        return dataDirPath;
    }

    @Bean
    public GenomicAssembly genomicAssembly() {
        return GenomicAssemblies.GRCh38p13();
    }

    @Bean
    public SvPrioritizerFactory svPriorityFactory(GenomicAssembly genomicAssembly,
                                                  DataSource dataSource,
                                                  SvannaProperties svannaProperties,
                                                  AnnotationDataService annotationDataService,
                                                  GeneService geneService,
                                                  PhenotypeDataService phenotypeDataService,
                                                  SimilarityScoreCalculator similarityScoreCalculator) {
        return new SvPrioritizerFactoryImpl(genomicAssembly, dataSource, svannaProperties, annotationDataService, geneService, phenotypeDataService, similarityScoreCalculator);
    }

    @Bean
    public GeneDosageDataService geneDosageDataService(DataSource dataSource, GenomicAssembly genomicAssembly) {
            LOGGER.debug("Using `clingen` gene dosage source");
            ClingenDosageElementDao clingenDosageElementDao = new ClingenDosageElementDao(dataSource, genomicAssembly);
            return new ClinGenGeneDosageDataService(clingenDosageElementDao);
    }

    @Bean
    public AnnotationDataService annotationDataService(DataSource dataSource,
                                                       GenomicAssembly genomicAssembly,
                                                       SvannaProperties svannaProperties,
                                                       GeneDosageDataService geneDosageDataService) {
        LOGGER.debug("Including TAD boundaries with stability >{}%", NF.format(svannaProperties.dataParameters().tadStabilityThresholdAsPercentage()));

        EnhancerProperties enhancers = svannaProperties.dataParameters().enhancers();
        if (enhancers.useVista())
            LOGGER.debug("Including VISTA enhancers");
        if (enhancers.useFantom5())
            LOGGER.debug("Including FANTOM5 enhancers with tissue specificity >{}", enhancers.fantom5TissueSpecificity());

        EnhancerAnnotationDao.EnhancerParameters enhancerParameters = EnhancerAnnotationDao.EnhancerParameters.of(enhancers.useVista(), enhancers.useFantom5(), enhancers.fantom5TissueSpecificity());

        return new DbAnnotationDataService(
                new EnhancerAnnotationDao(dataSource, genomicAssembly, enhancerParameters),
                new RepetitiveRegionDao(dataSource, genomicAssembly),
                new DbPopulationVariantDao(dataSource, genomicAssembly),
                new TadBoundaryDao(dataSource, genomicAssembly, svannaProperties.dataParameters().tadStabilityThresholdAsFraction()),
                geneDosageDataService);
    }

    @Bean
    public PhenotypeDataService phenotypeDataService(SvannaDataResolver svannaDataResolver,
                                                     DataSource svannaDatasource) {
        LOGGER.debug("Reading HPO file from {}", svannaDataResolver.hpOntologyPath().toAbsolutePath());
        Ontology ontology = OntologyLoader.loadOntology(svannaDataResolver.hpOntologyPath().toFile());

        GeneDiseaseDao geneDiseaseDao = new GeneDiseaseDao(svannaDatasource);
        List<GeneIdentifier> geneIdentifiers = geneDiseaseDao.geneIdentifiers();
        Map<String, List<HpoDiseaseSummary>> hgncGeneIdToDiseases = geneDiseaseDao.hgncGeneIdToDiseases();
        Map<String, List<TermId>> phenotypicAbnormalitiesForDiseaseId = geneDiseaseDao.diseaseToPhenotypes();
        return new DbPhenotypeDataService(ontology, geneIdentifiers, hgncGeneIdToDiseases, phenotypicAbnormalitiesForDiseaseId);
    }

    @Bean
    public SimilarityScoreCalculator similarityScoreCalculator(DataSource svannaDatasource,
                                                               SvannaProperties properties) throws UndefinedResourceException {
        SimilarityScoreCalculator similarityScoreCalculator;
        PrioritizationProperties.TermSimilarityMeasure similarityMeasure = properties.prioritization().termSimilarityMeasure();
        LOGGER.debug("Initializing phenotype term similarity calculator {}", similarityMeasure);

        MicaCalculator similarityCalculator = prepareMicaCalculator(svannaDatasource, properties.prioritization().icMicaMode());
        if (similarityMeasure == PrioritizationProperties.TermSimilarityMeasure.RESNIK_SYMMETRIC) {
            similarityScoreCalculator = new ResnikSimilarityScoreCalculator(similarityCalculator, true);
        } else if (similarityMeasure == PrioritizationProperties.TermSimilarityMeasure.RESNIK_ASYMMETRIC) {
            similarityScoreCalculator = new ResnikSimilarityScoreCalculator(similarityCalculator, false);
        } else {
            throw new UndefinedResourceException("Unknown term similarity measure " + similarityMeasure);
        }
        return similarityScoreCalculator;
    }

    @Bean
    public GeneOverlapper geneOverlapper(GeneService geneService) {
        return GeneOverlapper.of(geneService);
    }

    @Bean
    public GeneService geneService(GenomicAssembly genomicAssembly, SvannaDataResolver svannaDataResolver) throws InvalidResourceException {
        LOGGER.debug("Reading genes from {}", svannaDataResolver.genesJsonPath());
        try {
            return SilentGenesGeneService.of(genomicAssembly, svannaDataResolver.genesJsonPath());
        } catch (IOException e) {
            throw new InvalidResourceException("Error reading genes from `" + svannaDataResolver.genesJsonPath().toAbsolutePath() + "`", e);
        }
    }

    @Bean
    public SvannaDataResolver svannaDataResolver(Path svannaDataDirectory) throws MissingResourceException {
        return new SvannaDataResolver(svannaDataDirectory);
    }

    @Bean
    public DataSource svannaDatasource(SvannaDataResolver svannaDataResolver) {
        Path datasourcePath = svannaDataResolver.dataSourcePath();
        String jdbcUrl = String.format("jdbc:h2:file:%s;ACCESS_MODE_DATA=r", datasourcePath.toFile().getAbsolutePath());
        HikariConfig config = new HikariConfig();
        config.setUsername("sa");
        config.setPassword("sa");
        config.setDriverClassName("org.h2.Driver");
        config.setJdbcUrl(jdbcUrl);
        config.setPoolName("svanna-pool");

        return new HikariDataSource(config);
    }

}
