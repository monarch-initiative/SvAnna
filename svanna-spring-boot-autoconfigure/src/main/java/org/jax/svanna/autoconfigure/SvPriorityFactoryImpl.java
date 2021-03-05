package org.jax.svanna.autoconfigure;

import com.google.common.collect.Sets;
import org.jax.svanna.core.exception.LogUtils;
import org.jax.svanna.core.hpo.HpoDiseaseSummary;
import org.jax.svanna.core.hpo.PhenotypeDataService;
import org.jax.svanna.core.landscape.AnnotationDataService;
import org.jax.svanna.core.landscape.Enhancer;
import org.jax.svanna.core.priority.SvPrioritizer;
import org.jax.svanna.core.priority.SvPrioritizerType;
import org.jax.svanna.core.priority.SvPriority;
import org.jax.svanna.core.priority.SvPriorityFactory;
import org.jax.svanna.core.priority.additive.*;
import org.jax.svanna.core.priority.additive.ge.RouteDataEvaluatorGE;
import org.jax.svanna.core.priority.additive.ge.RouteDataGE;
import org.jax.svanna.core.reference.Gene;
import org.jax.svanna.core.reference.GeneService;
import org.jax.svanna.core.reference.SvannaVariant;
import org.jax.svanna.core.reference.TranscriptService;
import org.jax.svanna.db.additive.DbRouteDataServiceGE;
import org.jax.svanna.db.additive.dispatch.DispatcherDb;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.annotations.obo.hpo.HpoDiseaseAnnotationParser;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.phenol.ontology.data.TermIds;
import org.monarchinitiative.svart.GenomicAssembly;
import org.monarchinitiative.svart.Variant;

import javax.sql.DataSource;
import java.util.*;

public class SvPriorityFactoryImpl implements SvPriorityFactory {

    private final GenomicAssembly genomicAssembly;
    private final DataSource dataSource;
    private final SvannaProperties svannaProperties;
    private final SvannaDataResolver svannaDataResolver;
    private final AnnotationDataService annotationDataService;
    private final TranscriptService transcriptService;
    private final GeneService geneService;
    private final PhenotypeDataService phenotypeDataService;


    public SvPriorityFactoryImpl(
            GenomicAssembly genomicAssembly,
            DataSource dataSource,
            SvannaProperties svannaProperties,
            SvannaDataResolver svannaDataResolver, AnnotationDataService annotationDataService,
            TranscriptService transcriptService,
            GeneService geneService, PhenotypeDataService phenotypeDataService) {
        this.genomicAssembly = genomicAssembly;
        this.dataSource = dataSource;
        this.svannaProperties = svannaProperties;
        this.svannaDataResolver = svannaDataResolver;
        this.annotationDataService = annotationDataService;
        this.transcriptService = transcriptService;
        this.geneService = geneService;
        this.phenotypeDataService = phenotypeDataService;
    }


    @Override
    public <V extends Variant, P extends SvPriority> SvPrioritizer<V, P> getPrioritizer(SvPrioritizerType type, List<TermId> patientTerms) {
        switch (type) {
            case PROTOTYPE:
                LogUtils.logDebug(LOGGER, "Preparing top-level enhancer phenotype terms for the input terms");
                Set<TermId> enhancerTerms = annotationDataService.enhancerPhenotypeAssociations();
                Set<TermId> enhancerRelevantAncestors = phenotypeDataService.getRelevantAncestors(enhancerTerms, patientTerms);

                LogUtils.logDebug(LOGGER, "Preparing gene and disease data");
                Map<TermId, Set<HpoDiseaseSummary>> relevantGenesAndDiseases = phenotypeDataService.getRelevantGenesAndDiseases(patientTerms);
//                return new StrippedSvPrioritizer(annotationDataService,
//                        new SvAnnOverlapper(transcriptService.getChromosomeMap()),
//                        phenotypeDataService.geneBySymbol(),
//                    topLevelHpoTermsAndLabels.keySet(),
//                        enhancerRelevantAncestors,
//                        relevantGenesAndDiseases,
//                        MAX_GENES); // get from svannaProperties if needed
                return null;

            case ADDITIVE:
                LogUtils.logDebug(LOGGER, "Preparing dispatcher");
                Dispatcher dispatcher = new DispatcherDb(dataSource, genomicAssembly, svannaProperties.dataParameters().tadStabilityThreshold());
                LogUtils.logDebug(LOGGER, "Preparing route data service");
                RouteDataService<RouteDataGE> dbRouteDataService = new DbRouteDataServiceGE(annotationDataService, geneService);
                LogUtils.logDebug(LOGGER, "Preparing route data evaluator");
                RouteDataEvaluator<RouteDataGE> routeDataEvaluator = configureRouteDataEvaluator(patientTerms, svannaDataResolver);

                return (SvPrioritizer<V, P>) AdditiveSvPrioritizer.<SvannaVariant, RouteDataGE>builder()
                        .dispatcher(dispatcher)
                        .routeDataService(dbRouteDataService)
                        .routeDataEvaluator(routeDataEvaluator)
                        .build();
            default:
                throw new IllegalArgumentException("Unknown SvPrioritizerType " + type);
        }
    }

    private RouteDataEvaluatorGE configureRouteDataEvaluator(List<TermId> patientFeatures, SvannaDataResolver svannaDataResolver) {
        SequenceImpactCalculator<Gene> geneImpactCalculator = new GeneSequenceImpactCalculator();

        GeneWeightCalculator geneWeightCalculator = configureGeneWeightCalculator(phenotypeDataService, svannaDataResolver, patientFeatures);

        SequenceImpactCalculator<Enhancer> enhancerImpactCalculator = new EnhancerSequenceImpactCalculator();

        // TODO - provide real implementation
        EnhancerGeneRelevanceCalculator enhancerGeneRelevanceCalculator = EnhancerGeneRelevanceCalculator.defaultCalculator();

        return new RouteDataEvaluatorGE(geneImpactCalculator, geneWeightCalculator, enhancerImpactCalculator, enhancerGeneRelevanceCalculator);
    }


    private static GeneWeightCalculator configureGeneWeightCalculator(PhenotypeDataService phenotypeDataService, SvannaDataResolver svannaDataResolver, List<TermId> patientFeatures) {
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
