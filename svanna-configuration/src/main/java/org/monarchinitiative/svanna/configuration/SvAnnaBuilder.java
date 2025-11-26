package org.monarchinitiative.svanna.configuration;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoAssociationData;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoaderOptions;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoaders;
import org.monarchinitiative.phenol.io.MinimalOntologyLoader;
import org.monarchinitiative.phenol.ontology.data.MinimalOntology;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.phenol.ontology.similarity.TermPair;
import org.monarchinitiative.svanna.configuration.exception.InvalidResourceException;
import org.monarchinitiative.svanna.configuration.exception.MissingResourceException;
import org.monarchinitiative.svanna.configuration.exception.UndefinedResourceException;
import org.monarchinitiative.svanna.core.SvAnna;
import org.monarchinitiative.svanna.core.configuration.DataProperties;
import org.monarchinitiative.svanna.core.configuration.SvAnnaProperties;
import org.monarchinitiative.svanna.core.hpo.*;
import org.monarchinitiative.svanna.core.priority.SvPrioritizerFactory;
import org.monarchinitiative.svanna.core.service.AnnotationDataService;
import org.monarchinitiative.svanna.core.service.GeneDosageDataService;
import org.monarchinitiative.svanna.core.service.GeneService;
import org.monarchinitiative.svanna.core.service.PhenotypeDataService;
import org.monarchinitiative.svanna.db.gene.GeneDiseaseDao;
import org.monarchinitiative.svanna.db.landscape.*;
import org.monarchinitiative.svanna.db.service.ClinGenGeneDosageDataService;
import org.monarchinitiative.svanna.io.IOUtils;
import org.monarchinitiative.svanna.io.hpo.DbPhenotypeDataService;
import org.monarchinitiative.svanna.io.hpo.IcMicaDictUtils;
import org.monarchinitiative.svanna.io.service.SilentGenesGeneService;
import org.monarchinitiative.sgenes.model.GeneIdentifier;
import org.monarchinitiative.svart.assembly.GenomicAssemblies;
import org.monarchinitiative.svart.assembly.GenomicAssembly;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.util.Collection;
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
            MinimalOntology hpo = MinimalOntologyLoader.loadOntology(dataResolver.hpOntologyPath().toFile());
            HpoDiseases diseases;
            try {
                LOGGER.debug("Reading HPO annotations file from {}", dataResolver.phenotypeHpoaPath().toAbsolutePath());
                diseases = HpoDiseaseLoaders.defaultLoader(hpo, HpoDiseaseLoaderOptions.defaultOptions()).load(dataResolver.phenotypeHpoaPath());
            } catch (IOException e) {
                throw new InvalidResourceException("Error reading HPO annotations from `" + dataResolver.phenotypeHpoaPath().toAbsolutePath() + "`", e);
            }

            HpoAssociationData data = HpoAssociationData.builder(hpo)
                    .hpoDiseases(diseases)
                    .mim2GeneMedgen(dataResolver.mim2GeneMedgenPath())
                    .hgncCompleteSetArchive(dataResolver.hgncCompleteSetPath())
                    .build();

            Map<TermId, Collection<TermId>> geneIdToDiseaseIds = data.associations().geneIdToDiseaseIds();
            GeneDiseaseDao geneDiseaseDao = new GeneDiseaseDao(dataSource);
            List<GeneIdentifier> geneIdentifiers = geneDiseaseDao.geneIdentifiers();

            phenotypeDataService = new DbPhenotypeDataService(hpo, diseases, geneIdentifiers, geneIdToDiseaseIds);
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

            LOGGER.debug("Reading IC MICA table from {}", dataResolver.termToIcMicaPath());
            MicaCalculator micaCalculator;
            try (BufferedReader reader = IOUtils.openForReading(dataResolver.termToIcMicaPath())) {
                Map<TermPair, Double> termPairDoubleMap = IcMicaDictUtils.readTermPairMap(reader);
                micaCalculator = new InMemoryMicaCalculator(termPairDoubleMap);
            } catch (IOException e) {
                throw new InvalidResourceException("Cannot configure MICA calculator", e);
            }

            if (similarityMeasure.equals(TermSimilarityMeasure.RESNIK_SYMMETRIC)) {
                similarityScoreCalculator = new ResnikSimilarityScoreCalculator(micaCalculator, true);
            } else if (similarityMeasure.equals(TermSimilarityMeasure.RESNIK_ASYMMETRIC)) {
                similarityScoreCalculator = new ResnikSimilarityScoreCalculator(micaCalculator, false);
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
}
