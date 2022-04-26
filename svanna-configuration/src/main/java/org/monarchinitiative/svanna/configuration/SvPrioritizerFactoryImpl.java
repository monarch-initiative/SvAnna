package org.monarchinitiative.svanna.configuration;

import org.monarchinitiative.svanna.core.configuration.PrioritizationProperties;
import org.monarchinitiative.svanna.core.configuration.SvAnnaProperties;
import org.monarchinitiative.svanna.core.hpo.SimilarityScoreCalculator;
import org.monarchinitiative.svanna.core.priority.SvPrioritizer;
import org.monarchinitiative.svanna.core.priority.SvPrioritizerFactory;
import org.monarchinitiative.svanna.core.priority.SvPriority;
import org.monarchinitiative.svanna.core.priority.additive.*;
import org.monarchinitiative.svanna.core.priority.additive.evaluator.ge.GranularRouteDataEvaluatorGE;
import org.monarchinitiative.svanna.core.priority.additive.evaluator.ge.RouteDataGE;
import org.monarchinitiative.svanna.core.priority.additive.impact.EnhancerSequenceImpactCalculator;
import org.monarchinitiative.svanna.core.priority.additive.impact.GeneSequenceImpactCalculator;
import org.monarchinitiative.svanna.core.priority.additive.impact.SequenceImpactCalculator;
import org.monarchinitiative.svanna.core.service.AnnotationDataService;
import org.monarchinitiative.svanna.core.service.GeneService;
import org.monarchinitiative.svanna.core.service.PhenotypeDataService;
import org.monarchinitiative.svanna.db.additive.RouteDataServiceFactory;
import org.monarchinitiative.svanna.db.additive.dispatch.DispatchOptions;
import org.monarchinitiative.svanna.db.additive.dispatch.GeneDispatcher;
import org.monarchinitiative.svanna.db.additive.dispatch.TadAwareDispatcher;
import org.monarchinitiative.svanna.db.landscape.TadBoundaryDao;
import org.monarchinitiative.svanna.model.landscape.enhancer.Enhancer;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.svart.assembly.GenomicAssembly;
import org.monarchinitiative.sgenes.model.Gene;

import javax.sql.DataSource;
import java.util.*;

class SvPrioritizerFactoryImpl implements SvPrioritizerFactory {

    private final GenomicAssembly genomicAssembly;
    private final DataSource dataSource;
    private final SvAnnaProperties svAnnaProperties;
    private final AnnotationDataService annotationDataService;
    private final GeneService geneService;
    private final PhenotypeDataService phenotypeDataService;
    private final SimilarityScoreCalculator similarityScoreCalculator;


    SvPrioritizerFactoryImpl(
            GenomicAssembly genomicAssembly,
            DataSource dataSource,
            SvAnnaProperties svAnnaProperties,
            AnnotationDataService annotationDataService,
            GeneService geneService,
            PhenotypeDataService phenotypeDataService,
            SimilarityScoreCalculator similarityScoreCalculator) {
        this.genomicAssembly = genomicAssembly;
        this.dataSource = dataSource;
        this.svAnnaProperties = svAnnaProperties;
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
        TadBoundaryDao tadBoundaryDao = new TadBoundaryDao(dataSource, genomicAssembly, svAnnaProperties.dataProperties().tadStabilityThresholdAsFraction());
//        DispatchOptions dispatchOptions = DispatchOptions.of(svannaProperties.prioritization().forceTadEvaluation());
        DispatchOptions dispatchOptions = DispatchOptions.of(false);
        LOGGER.debug("Forcing TAD evaluation: {}", dispatchOptions.forceEvaluateTad());
        return new TadAwareDispatcher(geneService, tadBoundaryDao, dispatchOptions);
    }

    @Override
    public SvPrioritizer<SvPriority> getPrioritizer(Collection<TermId> phenotypeTerms) {
        LOGGER.debug("Preparing top-level enhancer phenotype terms for the input terms");
        Set<TermId> topLevelEnhancerTerms = annotationDataService.enhancerPhenotypeAssociations();
        Set<TermId> enhancerRelevantAncestors = phenotypeDataService.getRelevantAncestors(phenotypeTerms, topLevelEnhancerTerms);

        Dispatcher dispatcher = prepareDispatcher();
        RouteDataServiceFactory fct = new RouteDataServiceFactory(annotationDataService, geneService);
//        RouteDataService<RouteDataGETad> dbRouteDataService = fct.getService(RouteDataGETad.class);
        RouteDataService<RouteDataGE> dbRouteDataService = fct.getService(RouteDataGE.class);

        PrioritizationProperties prioritizationProperties = svAnnaProperties.prioritizationProperties();
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
