package org.jax.svanna.autoconfigure;

import com.google.common.collect.Sets;
import org.jax.svanna.core.exception.LogUtils;
import org.jax.svanna.core.hpo.PhenotypeDataService;
import org.jax.svanna.core.landscape.AnnotationDataService;
import org.jax.svanna.core.landscape.Enhancer;
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
import org.jax.svanna.core.reference.Gene;
import org.jax.svanna.core.reference.GeneService;
import org.jax.svanna.db.additive.DbRouteDataServiceGE;
import org.jax.svanna.db.additive.dispatch.DispatcherDb;
import org.jax.svanna.db.landscape.TadBoundaryDao;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.annotations.obo.hpo.HpoDiseaseAnnotationParser;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.phenol.ontology.data.TermIds;
import org.monarchinitiative.svart.GenomicAssembly;

import javax.sql.DataSource;
import java.util.*;

public class SvPrioritizerFactoryImpl implements SvPrioritizerFactory {

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


    @Override
    public SvPrioritizer<SvPriority> getPrioritizer(SvPrioritizerType type, Collection<TermId> phenotypeTerms) {
        LogUtils.logDebug(LOGGER, "Preparing top-level enhancer phenotype terms for the input terms");
        Set<TermId> topLevelEnhancerTerms = annotationDataService.enhancerPhenotypeAssociations();
        Set<TermId> enhancerRelevantAncestors = phenotypeDataService.getRelevantAncestors(phenotypeTerms, topLevelEnhancerTerms);

        switch (type) {
            case PROTOTYPE:
                LogUtils.logWarn(LOGGER, "PROTOTYPE SvPrioritizer is not currently supported");

//                LogUtils.logDebug(LOGGER, "Preparing gene and disease data");
//                Map<TermId, Set<HpoDiseaseSummary>> relevantGenesAndDiseases = phenotypeDataService.getRelevantGenesAndDiseases(patientTerms);
//                return new StrippedSvPrioritizer(annotationDataService,
//                        new SvAnnOverlapper(transcriptService.getChromosomeMap()),
//                        phenotypeDataService.geneBySymbol(),
//                    topLevelHpoTermsAndLabels.keySet(),
//                        enhancerRelevantAncestors,
//                        relevantGenesAndDiseases,
//                        MAX_GENES); // get from svannaProperties if needed
                return null;

            case ADDITIVE:
                TadBoundaryDao tadBoundaryDao = new TadBoundaryDao(dataSource, genomicAssembly, svannaProperties.dataParameters().tadStabilityThresholdAsFraction());
                Dispatcher dispatcher = new DispatcherDb(geneService, tadBoundaryDao);
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
