package org.jax.svanna.autoconfigure;

import org.jax.svanna.autoconfigure.configuration.PrioritizationProperties;
import org.jax.svanna.autoconfigure.configuration.SvannaProperties;
import org.jax.svanna.core.LogUtils;
import org.jax.svanna.core.hpo.SimilarityScoreCalculator;
import org.jax.svanna.core.priority.SvPrioritizer;
import org.jax.svanna.core.priority.SvPrioritizerFactory;
import org.jax.svanna.core.priority.SvPriority;
import org.jax.svanna.core.priority.additive.*;
import org.jax.svanna.core.priority.additive.evaluator.ge.GranularRouteDataEvaluatorGE;
import org.jax.svanna.core.priority.additive.evaluator.ge.RouteDataGE;
import org.jax.svanna.core.priority.additive.impact.EnhancerSequenceImpactCalculator;
import org.jax.svanna.core.priority.additive.impact.GeneSequenceImpactCalculator;
import org.jax.svanna.core.priority.additive.impact.SequenceImpactCalculator;
import org.jax.svanna.core.service.AnnotationDataService;
import org.jax.svanna.core.service.GeneService;
import org.jax.svanna.core.service.PhenotypeDataService;
import org.jax.svanna.db.additive.RouteDataServiceFactory;
import org.jax.svanna.db.additive.dispatch.DispatchOptions;
import org.jax.svanna.db.additive.dispatch.GeneDispatcher;
import org.jax.svanna.db.additive.dispatch.TadAwareDispatcher;
import org.jax.svanna.db.landscape.TadBoundaryDao;
import org.jax.svanna.model.landscape.enhancer.Enhancer;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.svart.GenomicAssembly;
import xyz.ielis.silent.genes.model.Gene;

import javax.sql.DataSource;
import java.util.*;

class SvPrioritizerFactoryImpl implements SvPrioritizerFactory {

    private final GenomicAssembly genomicAssembly;
    private final DataSource dataSource;
    private final SvannaProperties svannaProperties;
    private final AnnotationDataService annotationDataService;
    private final GeneService geneService;
    private final PhenotypeDataService phenotypeDataService;
    private final SimilarityScoreCalculator similarityScoreCalculator;


    SvPrioritizerFactoryImpl(
            GenomicAssembly genomicAssembly,
            DataSource dataSource,
            SvannaProperties svannaProperties,
            AnnotationDataService annotationDataService,
            GeneService geneService,
            PhenotypeDataService phenotypeDataService,
            SimilarityScoreCalculator similarityScoreCalculator) {
        this.genomicAssembly = genomicAssembly;
        this.dataSource = dataSource;
        this.svannaProperties = svannaProperties;
        this.annotationDataService = annotationDataService;
        this.geneService = geneService;
        this.phenotypeDataService = phenotypeDataService;
        this.similarityScoreCalculator = similarityScoreCalculator;
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
//        DispatchOptions dispatchOptions = DispatchOptions.of(svannaProperties.prioritization().forceTadEvaluation());
        DispatchOptions dispatchOptions = DispatchOptions.of(false);
        LogUtils.logDebug(LOGGER, "Forcing TAD evaluation: {}", dispatchOptions.forceEvaluateTad());
        return new TadAwareDispatcher(geneService, tadBoundaryDao, dispatchOptions);
    }

    @Override
    public SvPrioritizer<SvPriority> getPrioritizer(Collection<TermId> phenotypeTerms) {
        LogUtils.logDebug(LOGGER, "Preparing top-level enhancer phenotype terms for the input terms");
        Set<TermId> topLevelEnhancerTerms = annotationDataService.enhancerPhenotypeAssociations();
        Set<TermId> enhancerRelevantAncestors = phenotypeDataService.getRelevantAncestors(phenotypeTerms, topLevelEnhancerTerms);

        Dispatcher dispatcher = prepareDispatcher();
        RouteDataServiceFactory fct = new RouteDataServiceFactory(annotationDataService, geneService);
//        RouteDataService<RouteDataGETad> dbRouteDataService = fct.getService(RouteDataGETad.class);
        RouteDataService<RouteDataGE> dbRouteDataService = fct.getService(RouteDataGE.class);

        PrioritizationProperties prioritizationProperties = svannaProperties.prioritization();
        SequenceImpactCalculator<Gene> geneImpactCalculator = new GeneSequenceImpactCalculator(prioritizationProperties.geneFactor(), prioritizationProperties.promoterLength(), prioritizationProperties.promoterFitnessGain());
        GeneWeightCalculator geneWeightCalculator = configureGeneWeightCalculator(phenotypeDataService, similarityScoreCalculator, phenotypeTerms);

        SequenceImpactCalculator<Enhancer> enhancerImpactCalculator = new EnhancerSequenceImpactCalculator(prioritizationProperties.enhancerFactor());
        EnhancerGeneRelevanceCalculator enhancerGeneRelevanceCalculator = PhenotypeEnhancerGeneRelevanceCalculator.of(enhancerRelevantAncestors);

//        RouteDataEvaluator<RouteDataGETad, GranularRouteResult> granularEvaluator = new GranularRouteDataEvaluatorGETad(geneImpactCalculator, geneWeightCalculator, enhancerImpactCalculator, enhancerGeneRelevanceCalculator);
        RouteDataEvaluator<RouteDataGE, GranularRouteResult> granularEvaluator = new GranularRouteDataEvaluatorGE(geneImpactCalculator, geneWeightCalculator, enhancerImpactCalculator, enhancerGeneRelevanceCalculator);

        return AdditiveGranularSvPrioritizer.<RouteDataGE>builder()
                .dispatcher(dispatcher)
                .routeDataService(dbRouteDataService)
                .routeDataEvaluator(granularEvaluator)
                .build();

    }

    private static GeneWeightCalculator configureGeneWeightCalculator(PhenotypeDataService phenotypeDataService,
                                                                      SimilarityScoreCalculator similarityScoreCalculator,
                                                                      Collection<TermId> patientFeatures) {
        return new TermSimilarityGeneWeightCalculator(phenotypeDataService, similarityScoreCalculator, patientFeatures);
    }

}
