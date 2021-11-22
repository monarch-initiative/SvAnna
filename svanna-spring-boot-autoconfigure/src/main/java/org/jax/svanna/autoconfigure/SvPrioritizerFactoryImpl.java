package org.jax.svanna.autoconfigure;

import com.google.common.collect.Sets;
import org.jax.svanna.core.LogUtils;
import org.jax.svanna.core.priority.SvPrioritizer;
import org.jax.svanna.core.priority.SvPrioritizerFactory;
import org.jax.svanna.core.priority.SvPrioritizerType;
import org.jax.svanna.core.priority.SvPriority;
import org.jax.svanna.core.priority.additive.*;
import org.jax.svanna.core.priority.additive.ge.GranularRouteDataEvaluatorGE;
import org.jax.svanna.core.priority.additive.ge.RouteDataGE;
import org.jax.svanna.core.priority.additive.impact.EnhancerSequenceImpactCalculator;
import org.jax.svanna.core.priority.additive.impact.GeneSequenceImpactCalculator;
import org.jax.svanna.core.priority.additive.impact.SequenceImpactCalculator;
import org.jax.svanna.core.service.AnnotationDataService;
import org.jax.svanna.core.service.GeneService;
import org.jax.svanna.core.service.PhenotypeDataService;
import org.jax.svanna.db.additive.DbRouteDataServiceGE;
import org.jax.svanna.db.additive.dispatch.DispatchOptions;
import org.jax.svanna.db.additive.dispatch.GeneDispatcher;
import org.jax.svanna.db.additive.dispatch.TadAwareDispatcher;
import org.jax.svanna.db.landscape.TadBoundaryDao;
import org.jax.svanna.model.landscape.enhancer.Enhancer;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.annotations.obo.hpo.HpoDiseaseAnnotationParser;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.phenol.ontology.data.TermIds;
import org.monarchinitiative.svart.GenomicAssembly;
import xyz.ielis.silent.genes.model.Gene;

import javax.sql.DataSource;
import java.util.*;

class SvPrioritizerFactoryImpl implements SvPrioritizerFactory {

    private final GenomicAssembly genomicAssembly;
    private final DataSource dataSource;
    private final SvannaProperties svannaProperties;
    private final SvannaDataResolver svannaDataResolver;
    private final AnnotationDataService annotationDataService;
    private final GeneService geneService;
    private final PhenotypeDataService phenotypeDataService;


    SvPrioritizerFactoryImpl(
            GenomicAssembly genomicAssembly,
            DataSource dataSource,
            SvannaProperties svannaProperties,
            SvannaDataResolver svannaDataResolver, AnnotationDataService annotationDataService,
            GeneService geneService,
            PhenotypeDataService phenotypeDataService) {
        this.genomicAssembly = genomicAssembly;
        this.dataSource = dataSource;
        this.svannaProperties = svannaProperties;
        this.svannaDataResolver = svannaDataResolver;
        this.annotationDataService = annotationDataService;
        this.geneService = geneService;
        this.phenotypeDataService = phenotypeDataService;
    }

    private Dispatcher prepareDispatcher() {
        // If we ever want to evaluate variants using TADs again, this is the place to start.
//        Dispatcher dispatcher = getTadDispatcher();
        return getGeneDispatcher();
    }

    private GeneDispatcher getGeneDispatcher() {
        return new GeneDispatcher(geneService);
    }

    private Dispatcher getTadDispatcher() {
        TadBoundaryDao tadBoundaryDao = new TadBoundaryDao(dataSource, genomicAssembly, svannaProperties.dataParameters().tadStabilityThresholdAsFraction());
        DispatchOptions dispatchOptions = DispatchOptions.of(svannaProperties.prioritizationParameters().forceTadEvaluation());
        LogUtils.logDebug(LOGGER, "Forcing TAD evaluation: {}", dispatchOptions.forceEvaluateTad());
        return new TadAwareDispatcher(geneService, tadBoundaryDao, dispatchOptions);
    }

    @Override
    public SvPrioritizer<SvPriority> getPrioritizer(SvPrioritizerType type, Collection<TermId> phenotypeTerms) {
        LogUtils.logDebug(LOGGER, "Preparing top-level enhancer phenotype terms for the input terms");
        Set<TermId> topLevelEnhancerTerms = annotationDataService.enhancerPhenotypeAssociations();
        Set<TermId> enhancerRelevantAncestors = phenotypeDataService.getRelevantAncestors(phenotypeTerms, topLevelEnhancerTerms);

        Dispatcher dispatcher = prepareDispatcher();

        //noinspection SwitchStatementWithTooFewBranches
        switch (type) {
            case ADDITIVE:
                RouteDataService<RouteDataGE> dbRouteDataService = new DbRouteDataServiceGE(annotationDataService, geneService);

                SvannaProperties.PrioritizationParameters prioritizationParameters = svannaProperties.prioritizationParameters();
                SequenceImpactCalculator<Gene> geneImpactCalculator = new GeneSequenceImpactCalculator(prioritizationParameters.geneFactor(), prioritizationParameters.promoterLength(), prioritizationParameters.promoterFitnessGain());
                GeneWeightCalculator geneWeightCalculator = configureGeneWeightCalculator(phenotypeTerms, phenotypeDataService, svannaDataResolver);

                SequenceImpactCalculator<Enhancer> enhancerImpactCalculator = new EnhancerSequenceImpactCalculator(prioritizationParameters.enhancerFactor());
                EnhancerGeneRelevanceCalculator enhancerGeneRelevanceCalculator = PhenotypeEnhancerGeneRelevanceCalculator.of(enhancerRelevantAncestors);

                LogUtils.logDebug(LOGGER, "Preparing ADDITIVE SV prioritizer");
                RouteDataEvaluator<RouteDataGE, GranularRouteResult> granularEvaluator = new GranularRouteDataEvaluatorGE(geneImpactCalculator, geneWeightCalculator, enhancerImpactCalculator, enhancerGeneRelevanceCalculator);
                return AdditiveGranularSvPrioritizer.builder()
                        .dispatcher(dispatcher)
                        .routeDataService(dbRouteDataService)
                        .routeDataEvaluator(granularEvaluator)
                        .build();

            default:
                throw new IllegalArgumentException("Unknown SvPrioritizerType " + type);
        }
    }

    private static GeneWeightCalculator configureGeneWeightCalculator(Collection<TermId> patientFeatures,
                                                                      PhenotypeDataService phenotypeDataService,
                                                                      SvannaDataResolver svannaDataResolver) {
        Ontology ontology = phenotypeDataService.ontology();


        String hpoaPath = svannaDataResolver.phenotypeHpoaPath().toString();
        List<String> databases = List.of("OMIM"); // restrict ourselves to OMIM entries
        Map<TermId, HpoDisease> diseaseMap = HpoDiseaseAnnotationParser.loadDiseaseMap(hpoaPath, ontology, databases);


        Map<TermId, Collection<TermId>> diseaseIdToTermIds = new HashMap<>();

        for (TermId diseaseId : diseaseMap.keySet()) {
            HpoDisease disease = diseaseMap.get(diseaseId);
            List<TermId> hpoTerms = disease.getPhenotypicAbnormalityTermIdList();
            diseaseIdToTermIds.putIfAbsent(diseaseId, new HashSet<>());

            // add term ancestors
            Set<TermId> inclAncestorTermIds = TermIds.augmentWithAncestors(ontology, Sets.newHashSet(hpoTerms), true);
            for (TermId tid : inclAncestorTermIds) {
                diseaseIdToTermIds.get(diseaseId).add(tid);
            }
        }

        return new TermSimilarityGeneWeightCalculator(phenotypeDataService, patientFeatures, diseaseIdToTermIds);
    }

}
