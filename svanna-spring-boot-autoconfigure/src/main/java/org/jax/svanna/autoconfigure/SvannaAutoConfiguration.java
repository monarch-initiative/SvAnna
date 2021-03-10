package org.jax.svanna.autoconfigure;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import de.charite.compbio.jannovar.data.JannovarData;
import de.charite.compbio.jannovar.data.JannovarDataSerializer;
import de.charite.compbio.jannovar.data.SerializationException;
import org.jax.svanna.autoconfigure.exception.MissingResourceException;
import org.jax.svanna.autoconfigure.exception.UndefinedResourceException;
import org.jax.svanna.core.exception.LogUtils;
import org.jax.svanna.core.hpo.*;
import org.jax.svanna.core.landscape.AnnotationDataService;
import org.jax.svanna.core.priority.SvPriorityFactory;
import org.jax.svanna.core.reference.GeneService;
import org.jax.svanna.core.reference.TranscriptService;
import org.jax.svanna.core.reference.transcripts.JannovarGeneService;
import org.jax.svanna.core.reference.transcripts.JannovarTranscriptService;
import org.jax.svanna.db.landscape.*;
import org.jax.svanna.io.hpo.PhenotypeDataServiceDefault;
import org.jax.svanna.io.hpo.PrecomputedResnikSimilaritiesParser;
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

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

@Configuration
@EnableConfigurationProperties(SvannaProperties.class)
public class SvannaAutoConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(SvannaAutoConfiguration.class);

    private static final Properties properties = readProperties();

    private static final String SVANNA_VERSION = properties.getProperty("svanna.version", "unknown version");

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
        LogUtils.logInfo(LOGGER, "Spooling up SvAnna v{} using resources in `{}`", SVANNA_VERSION, dataDirPath.toAbsolutePath());
        return dataDirPath;
    }

    @Bean
    public GenomicAssembly genomicAssembly() {
        return GenomicAssemblies.GRCh38p13();
    }

    @Bean
    public SvPriorityFactory svPriorityFactory(GenomicAssembly genomicAssembly,
                                               DataSource dataSource,
                                               SvannaProperties svannaProperties,
                                               SvannaDataResolver svannaDataResolver,
                                               AnnotationDataService annotationDataService,
                                               TranscriptService transcriptService,
                                               GeneService geneService,
                                               PhenotypeDataService phenotypeDataService) {
        return new SvPriorityFactoryImpl(genomicAssembly, dataSource, svannaProperties, svannaDataResolver, annotationDataService, transcriptService, geneService, phenotypeDataService);
    }

    @Bean
    public AnnotationDataService annotationDataService(DataSource dataSource, GenomicAssembly genomicAssembly, SvannaProperties svannaProperties) {
        double stabilityThreshold = svannaProperties.dataParameters().tadStabilityThreshold();
        LogUtils.logDebug(LOGGER, "Including TAD boundaries with stability >{}", stabilityThreshold);

        double enhancerSpecificityThreshold = svannaProperties.dataParameters().enhancerSpecificityThreshold();
        LogUtils.logDebug(LOGGER, "Including enhancers with tissue specificity >{}", enhancerSpecificityThreshold);

        return new DbAnnotationDataService(
                new EnhancerAnnotationDao(dataSource, genomicAssembly, enhancerSpecificityThreshold),
                new RepetitiveRegionDao(dataSource, genomicAssembly),
                new DbPopulationVariantDao(dataSource, genomicAssembly),
                new TadBoundaryDao(dataSource, genomicAssembly, stabilityThreshold));
    }

    @Bean
    public PhenotypeDataService phenotypeDataService(DataSource dataSource, SvannaDataResolver svannaDataResolver, SvannaProperties properties) throws UndefinedResourceException, IOException {
        LogUtils.logDebug(LOGGER, "Reading HPO obo file from `{}`", svannaDataResolver.hpOntologyPath().toAbsolutePath());
        Ontology ontology = OntologyLoader.loadOntology(svannaDataResolver.hpOntologyPath().toFile());
        Path hpoaPath = svannaDataResolver.phenotypeHpoaPath().toAbsolutePath();
        LogUtils.logDebug(LOGGER, "Parsing HPO disease associations at `{}`", hpoaPath);
        Path geneInfoPath = svannaDataResolver.geneInfoPath();
        LogUtils.logDebug(LOGGER, "Parsing gene info file at `{}`", geneInfoPath.toAbsolutePath());
        Path mim2geneMedgenPath = svannaDataResolver.mim2geneMedgenPath();
        LogUtils.logDebug(LOGGER, "Parsing MIM to gene medgen file at `{}`", mim2geneMedgenPath.toAbsolutePath());

        HpoAssociationParser hap = new HpoAssociationParser(geneInfoPath.toFile(),
                mim2geneMedgenPath.toFile(), null,
                svannaDataResolver.phenotypeHpoaPath().toFile(), ontology);
        Map<TermId, HpoDisease> diseaseMap = HpoDiseaseAnnotationParser.loadDiseaseMap(hpoaPath.toString(), ontology);
        Set<GeneWithId> geneWithIds = hap.getGeneIdToSymbolMap().entrySet().stream().map(e -> GeneWithId.of(e.getValue(), e.getKey())).collect(Collectors.toSet());

        SimilarityScoreCalculator similarityScoreCalculator;
        SvannaProperties.TermSimilarityMeasure similarityMeasure = properties.prioritizationParameters().termSimilarityMeasure();
        LogUtils.logDebug(LOGGER, "Initializing phenotype term similarity calculator `{}`", similarityMeasure);

        if (similarityMeasure == SvannaProperties.TermSimilarityMeasure.RESNIK_SYMMETRIC) {
            LogUtils.logDebug(LOGGER, "Reading precomputed Resnik term similarities from `{}`", svannaDataResolver.precomputedResnikSimilaritiesPath());
            Map<TermPair, Double> resnikSimilarities = PrecomputedResnikSimilaritiesParser.readSimilarities(svannaDataResolver.precomputedResnikSimilaritiesPath());
            similarityScoreCalculator = new ResnikSimilarityScoreCalculator(resnikSimilarities, true);
        } else if (similarityMeasure == SvannaProperties.TermSimilarityMeasure.RESNIK_ASYMMETRIC) {
            LogUtils.logDebug(LOGGER, "Reading precomputed Resnik term similarities from `{}`", svannaDataResolver.precomputedResnikSimilaritiesPath());
            Map<TermPair, Double> resnikSimilarities = PrecomputedResnikSimilaritiesParser.readSimilarities(svannaDataResolver.precomputedResnikSimilaritiesPath());
            similarityScoreCalculator = new ResnikSimilarityScoreCalculator(resnikSimilarities, false);
        } else {
            throw new UndefinedResourceException("Unknown term similarity measure " + similarityMeasure);
        }

        LogUtils.logDebug(LOGGER, "Done");

        return new PhenotypeDataServiceDefault(ontology, hap.getDiseaseToGeneIdMap(), diseaseMap, geneWithIds, similarityScoreCalculator);
    }

    @Bean
    public GeneService geneService(GenomicAssembly genomicAssembly, JannovarData jannovarData) {
        return JannovarGeneService.of(genomicAssembly, jannovarData);
    }


    @Bean
    public TranscriptService transcriptService(GenomicAssembly genomicAssembly, JannovarData jannovarData) {
        return JannovarTranscriptService.of(genomicAssembly, jannovarData);
    }


    @Bean
    public JannovarData jannovarData(SvannaProperties svannaProperties) throws SerializationException {
        LogUtils.logInfo(LOGGER, "Reading transcript definitions from `{}`", svannaProperties.jannovarCachePath());
        return new JannovarDataSerializer(svannaProperties.jannovarCachePath()).load();
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


    private static Properties readProperties() {
        Properties properties = new Properties();

        try (InputStream is = SvannaAutoConfiguration.class.getResourceAsStream("/svanna.properties")) {
            properties.load(is);
        } catch (IOException e) {
            LogUtils.logWarn(LOGGER, "Error loading properties: {}", e.getMessage());
        }
        return properties;
    }
}