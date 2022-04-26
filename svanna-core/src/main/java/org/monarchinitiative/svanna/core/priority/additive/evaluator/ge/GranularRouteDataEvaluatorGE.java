package org.monarchinitiative.svanna.core.priority.additive.evaluator.ge;

import org.monarchinitiative.svanna.core.LogUtils;
import org.monarchinitiative.svanna.core.priority.additive.*;
import org.monarchinitiative.svanna.core.priority.additive.impact.SequenceImpactCalculator;
import org.monarchinitiative.svanna.model.landscape.enhancer.Enhancer;
import org.monarchinitiative.sgenes.model.Gene;
import org.monarchinitiative.sgenes.model.Identified;
import org.monarchinitiative.sgenes.model.Located;
import org.monarchinitiative.svart.GenomicRegion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class GranularRouteDataEvaluatorGE implements RouteDataEvaluator<RouteDataGE, GranularRouteResult> {

    private static final Logger LOGGER = LoggerFactory.getLogger(GranularRouteDataEvaluatorGE.class);

    private final SequenceImpactCalculator<Gene> geneImpactCalculator;
    private final GeneWeightCalculator geneWeightCalculator;

    private final SequenceImpactCalculator<Enhancer> enhancerImpactCalculator;
    private final EnhancerGeneRelevanceCalculator enhancerGeneRelevanceCalculator;

    public GranularRouteDataEvaluatorGE(SequenceImpactCalculator<Gene> geneImpactCalculator,
                                        GeneWeightCalculator geneWeightCalculator,
                                        SequenceImpactCalculator<Enhancer> enhancerImpactCalculator,
                                        EnhancerGeneRelevanceCalculator enhancerGeneRelevanceCalculator) {
        this.geneImpactCalculator = geneImpactCalculator;
        this.geneWeightCalculator = geneWeightCalculator;
        this.enhancerImpactCalculator = enhancerImpactCalculator;
        this.enhancerGeneRelevanceCalculator = enhancerGeneRelevanceCalculator;
    }

    private static <T, U extends Collection<T>> int countItemsInCollections(Map<?, U> items) {
        int count = 0;
        for (U genes : items.values()) {
            count += genes.size();
        }
        return count;
    }

    @Override
    public GranularRouteResult evaluate(RouteDataGE routeData) {
        Routes routes = routeData.route();

        Map<String, Double> referenceScores = evaluateReference(routes.references(), routeData.genes(), routeData.enhancers());
        Map<String, Double> alternateScores = evaluateAlternate(routes.alternates(), routeData.genes(), routeData.enhancers());

        Set<String> geneAccessions = routeData.genes().stream()
                .map(Identified::accession)
                .collect(Collectors.toUnmodifiableSet());

        Map<String, Double> scoreMap = GranularEvaluatorUtils.calculateDeltas(geneAccessions, referenceScores, alternateScores);

        return GranularRouteResult.of(scoreMap);
    }

    private Map<String, Double> evaluateReference(List<GenomicRegion> references,
                                                  Set<Gene> genes,
                                                  Set<Enhancer> enhancers) {
        Map<Integer, GenomicRegion> referenceByContig = new HashMap<>(references.size());
        for (GenomicRegion reference : references) {
            if (referenceByContig.put(reference.contigId(), reference) != null)
                // Although there might be a legitimate situation for having two reference regions on the same contig,
                // we do not support this at the moment
                throw new EvaluationException("Saw two reference regions for the same contig " + reference.contigName());
        }

        // Group genes, and enhancers by contig
        Map<Integer, List<Gene>> genesByContig = genes.stream()
                .collect(Collectors.groupingBy(Located::contigId, Collectors.toUnmodifiableList()));
        Map<Integer, List<Enhancer>> enhancersByContig = enhancers.stream()
                .collect(Collectors.groupingBy(Located::contigId, Collectors.toUnmodifiableList()));

        int nGenes = countItemsInCollections(genesByContig);
        Map<String, Double> results = new HashMap<>(nGenes);

        // Process each reference
        for (Integer contig : referenceByContig.keySet()) {
            // Score within reference
            List<Enhancer> enhancersOnContig = enhancersByContig.getOrDefault(contig, List.of());
            for (Gene gene : genesByContig.getOrDefault(contig, List.of())) {
                double geneImpact = geneImpactCalculator.noImpact();
                double geneRelevance = Math.exp(geneWeightCalculator.calculateRelevance(gene));
                double enhancerRelevance = 0.;
                for (Enhancer enhancer : enhancersOnContig) {
                    enhancerRelevance += enhancerImpactCalculator.noImpact() * enhancerGeneRelevanceCalculator.calculateRelevance(enhancer);
                }
                double score = geneImpact * geneRelevance + enhancerRelevance;
                // A score for a gene might already be in the results map if the event duplicates the entire gene
                results.merge(gene.accession(), score, Double::sum);
            }
        }

        return results;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Double> evaluateAlternate(List<Route> routes,
                                                  Set<Gene> genes,
                                                  Set<Enhancer> enhancers) {
        Map<String, Double> results = new HashMap<>(genes.size());

        for (Route route : routes) {
            // Choose genes & enhancers that can be projected on the current route
            List<Projection<? extends Located>> projections = EvaluatorUtils.projectGenesEnhancers(route, genes, enhancers);
            if (projections.isEmpty()) continue; // shortcut


            // Find indices of genes and enhancers of the route
            List<Integer> geneIndices = new LinkedList<>();
            List<Integer> enhancerIndices = new LinkedList<>();

            for (int i = 0; i < projections.size(); i++) {
                Projection<? extends Located> projection = projections.get(i);
                if (projection.source() instanceof Gene)
                    geneIndices.add(i);
                else if (projection.source() instanceof Enhancer)
                    enhancerIndices.add(i);
                else
                    LogUtils.logWarn(LOGGER, "Skipping evaluation of an unknown projection `{}`", projection.source().getClass().getSimpleName());
            }

            // Evaluate all genes of the route
            for (int geneIdx : geneIndices) {
                Projection<Gene> gene = (Projection<Gene>) projections.get(geneIdx);
                double geneImpact = geneImpactCalculator.projectImpact(gene);
                if (geneImpact < EvaluatorUtils.CLOSE_TO_ZERO)
                    // Loss of function
                    continue;
                double geneRelevance = Math.exp(geneWeightCalculator.calculateRelevance(gene.source()));

                double enhancerRelevance = 0.;
                for (int enhancerIdx : enhancerIndices) {
                    Projection<Enhancer> enhancer = (Projection<Enhancer>) projections.get(enhancerIdx);
                    enhancerRelevance += enhancerImpactCalculator.projectImpact(enhancer) * enhancerGeneRelevanceCalculator.calculateRelevance(enhancer.source());
                }

                double score = geneImpact * geneRelevance + enhancerRelevance;
                // A score for the gene can already be in the results map if the event duplicates the entire gene
                results.merge(gene.source().accession(), score, Double::sum);
            }

        }

        return results;
    }

}
