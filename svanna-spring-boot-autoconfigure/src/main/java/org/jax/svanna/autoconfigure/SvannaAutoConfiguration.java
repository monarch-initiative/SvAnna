package org.jax.svanna.autoconfigure;

import com.google.common.collect.Multimap;
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
import org.jax.svanna.db.landscape.*;
import org.jax.svanna.db.phenotype.MicaDao;
import org.jax.svanna.db.service.ClinGenGeneDosageDataService;
import org.jax.svanna.io.hpo.PhenotypeDataServiceDefault;
import org.jax.svanna.io.service.SilentGenesGeneService;
import org.monarchinitiative.phenol.annotations.assoc.HpoAssociationParser;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.annotations.obo.hpo.HpoDiseaseAnnotationParser;
import org.monarchinitiative.phenol.io.OntologyLoader;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.svart.GenomicAssemblies;
import org.monarchinitiative.svart.GenomicAssembly;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import xyz.ielis.silent.genes.model.GeneIdentifier;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.util.*;
import java.util.stream.Collectors;

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

    private static Map<TermId, Set<TermId>> prepareDiseaseToGeneIdMap(Multimap<TermId, TermId> diseaseToGeneIdMap) {
        Map<TermId, Set<TermId>> dtgIdMap = new HashMap<>(diseaseToGeneIdMap.size());
        for (TermId termId : diseaseToGeneIdMap.keySet()) {
            Collection<TermId> termIds = diseaseToGeneIdMap.get(termId);
            dtgIdMap.put(termId, Set.copyOf(termIds));
        }
        return dtgIdMap;
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
        LOGGER.info("Spooling up SvAnna v{} using resources in `{}`", SVANNA_VERSION, dataDirPath.toAbsolutePath());
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
                                                  SvannaDataResolver svannaDataResolver,
                                                  AnnotationDataService annotationDataService,
                                                  GeneService geneService,
                                                  PhenotypeDataService phenotypeDataService) {
        return new SvPrioritizerFactoryImpl(genomicAssembly, dataSource, svannaProperties, svannaDataResolver, annotationDataService, geneService, phenotypeDataService);
    }

    @Bean
    public GeneDosageDataService geneDosageDataService(DataSource dataSource,
                                                       GenomicAssembly genomicAssembly,
                                                       GeneService geneService,
                                                       SvannaProperties svannaProperties) throws UndefinedResourceException {

        switch (svannaProperties.prioritization().geneDosageSource()) {
            case "constant":
                LOGGER.debug("Using `constant` gene dosage source");
                return new ConstantGeneDosageDataService(geneService);
            case "clingen":
                LOGGER.debug("Using `clingen` gene dosage source");
                ClingenDosageElementDao clingenDosageElementDao = new ClingenDosageElementDao(dataSource, genomicAssembly);
                return new ClinGenGeneDosageDataService(clingenDosageElementDao);
            default:
                LOGGER.error("Unknown gene dosage source: `{}`", svannaProperties.prioritization().geneDosageSource());
                throw new UndefinedResourceException(String.format("Unknown gene dosage source: `%s`", svannaProperties.prioritization().geneDosageSource()));
        }
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
    public PhenotypeDataService phenotypeDataService(SvannaDataResolver svannaDataResolver, DataSource svannaDatasource, SvannaProperties properties) throws UndefinedResourceException, IOException {
        LOGGER.debug("Reading HPO obo file from `{}`", svannaDataResolver.hpOntologyPath().toAbsolutePath());
        Ontology ontology = OntologyLoader.loadOntology(svannaDataResolver.hpOntologyPath().toFile());
        Path hpoaPath = svannaDataResolver.phenotypeHpoaPath().toAbsolutePath();
        LOGGER.debug("Parsing HPO disease associations at `{}`", hpoaPath);
        Path geneInfoPath = svannaDataResolver.geneInfoPath();
        LOGGER.debug("Parsing gene info file at `{}`", geneInfoPath.toAbsolutePath());
        Path mim2geneMedgenPath = svannaDataResolver.mim2geneMedgenPath();
        LOGGER.debug("Parsing MIM to gene medgen file at `{}`", mim2geneMedgenPath.toAbsolutePath());

        HpoAssociationParser hap = new HpoAssociationParser(geneInfoPath.toFile(),
                mim2geneMedgenPath.toFile(), null,
                svannaDataResolver.phenotypeHpoaPath().toFile(), ontology);
        Map<TermId, HpoDisease> diseaseMap = HpoDiseaseAnnotationParser.loadDiseaseMap(hpoaPath.toString(), ontology);
        Set<GeneIdentifier> geneIdentifiers = hap.getGeneIdToSymbolMap().entrySet().stream()
                .map(e -> GeneIdentifier.of(e.getValue(), e.getKey().getValue(), null, null)) // TODO - check values
                .collect(Collectors.toUnmodifiableSet());

        SimilarityScoreCalculator similarityScoreCalculator;
        PrioritizationProperties.TermSimilarityMeasure similarityMeasure = properties.prioritization().termSimilarityMeasure();
        LOGGER.debug("Initializing phenotype term similarity calculator `{}`", similarityMeasure);

        MicaCalculator similarityCalculator = prepareMicaCalculator(svannaDatasource, properties.prioritization().icMicaMode());
        if (similarityMeasure == PrioritizationProperties.TermSimilarityMeasure.RESNIK_SYMMETRIC) {
            similarityScoreCalculator = new ResnikSimilarityScoreCalculator(similarityCalculator, true);
        } else if (similarityMeasure == PrioritizationProperties.TermSimilarityMeasure.RESNIK_ASYMMETRIC) {
            similarityScoreCalculator = new ResnikSimilarityScoreCalculator(similarityCalculator, false);
        } else {
            throw new UndefinedResourceException("Unknown term similarity measure " + similarityMeasure);
        }
        LOGGER.debug("Done");

        Map<TermId, Set<TermId>> diseaseToGeneIdMap = prepareDiseaseToGeneIdMap(hap.getDiseaseToGeneIdMap());

        return new PhenotypeDataServiceDefault(ontology, diseaseToGeneIdMap, diseaseMap, geneIdentifiers, similarityScoreCalculator);
    }

    @Bean
    public GeneOverlapper geneOverlapper(GeneService geneService) {
        return GeneOverlapper.of(geneService);
    }

    @Bean
    public GeneService geneService(GenomicAssembly genomicAssembly, SvannaDataResolver svannaDataResolver) throws InvalidResourceException {
        LOGGER.debug("Reading genes from `{}`", svannaDataResolver.genesJsonPath());
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
