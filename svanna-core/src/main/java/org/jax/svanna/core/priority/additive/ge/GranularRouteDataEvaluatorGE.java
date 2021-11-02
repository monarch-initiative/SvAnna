package org.jax.svanna.core.priority.additive.ge;

import org.jax.svanna.core.LogUtils;
import org.jax.svanna.core.priority.additive.*;
import org.jax.svanna.core.priority.additive.impact.SequenceImpactCalculator;
import org.jax.svanna.model.gene.Gene;
import org.jax.svanna.model.landscape.enhancer.Enhancer;
import org.jax.svanna.model.landscape.tad.TadBoundary;
import org.monarchinitiative.svart.Contig;
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

    @Override
    public GranularRouteResult evaluate(RouteDataGE routeData) {
        Routes routes = routeData.route();

        Map<String, Double> referenceScores = evaluateReference(routes.references(), routeData.genes(), routeData.enhancers(), routeData.tadBoundaries());
        Map<String, Double> alternateScores = evaluateAlternate(routes.alternates(), routeData.genes(), routeData.enhancers(), routeData.tadBoundaries());

        Set<String> geneAccessions = routeData.genes().stream()
                .map(g -> g.accessionId().getValue())
                .collect(Collectors.toUnmodifiableSet());

        Map<String, Double> scoreMap = GranularEvaluatorUtils.calculateDeltas(geneAccessions, referenceScores, alternateScores);

        return GranularRouteResult.of(scoreMap);
    }

    private Map<String, Double> evaluateReference(Set<GenomicRegion> references,
                                                  Set<Gene> genes,
                                                  Set<Enhancer> enhancers,
                                                  List<TadBoundary> tadBoundaries) {
        Map<Contig, GenomicRegion> referenceByContig = new HashMap<>(references.size());
        for (GenomicRegion reference : references) {
            if (referenceByContig.put(reference.contig(), reference) != null)
                // Although there might be a legitimate situation for having two reference regions on the same contig,
                // we do not support this at the moment
                throw new EvaluationException("Saw two reference regions for the same contig " + reference.contigName());
        }

        // Group TAD boundaries, genes, and enhancers by contig
        Map<Contig, List<TadBoundary>> tadsByContig = tadBoundaries.stream()
                .distinct()
                .collect(Collectors.groupingBy(GenomicRegion::contig, Collectors.toUnmodifiableList()));
        Map<Contig, Set<Gene>> genesByContig = genes.stream()
                .collect(Collectors.groupingBy(GenomicRegion::contig, Collectors.toUnmodifiableSet()));
        Map<Contig, Set<Enhancer>> enhancersByContig = enhancers.stream()
                .collect(Collectors.groupingBy(GenomicRegion::contig, Collectors.toUnmodifiableSet()));

        // Prepare TAD regions
        Map<Contig, List<GenomicRegion>> evaluationRegionsByContig = EvaluatorUtils.prepareEvaluationRegions(referenceByContig, tadsByContig);

        int nGenes = genesByContig.values().stream().mapToInt(Collection::size).sum();
        Map<String, Double> results = new HashMap<>(nGenes);

        // Process each reference
        for (Contig contig : referenceByContig.keySet()) {
            List<GenomicRegion> evaluationRegions = evaluationRegionsByContig.get(contig);

            Set<Gene> genesOnContig = genesByContig.getOrDefault(contig, Set.of());
            Map<GenomicRegion, List<Gene>> genesByRegion = EvaluatorUtils.groupItemsByRegion(evaluationRegions, genesOnContig);

            Set<Enhancer> enhancersOnContig = enhancersByContig.getOrDefault(contig, Set.of());
            Map<GenomicRegion, List<Enhancer>> enhancersByRegion = EvaluatorUtils.groupItemsByRegion(evaluationRegions, enhancersOnContig);

            // Score within TADs
            for (GenomicRegion evaluationRegion : evaluationRegions) {
                List<Enhancer> tadEnhancers = enhancersByRegion.get(evaluationRegion);

                for (Gene gene : genesByRegion.get(evaluationRegion)) {
                    double geneImpact = geneImpactCalculator.noImpact();
                    double geneRelevance = Math.exp(geneWeightCalculator.calculateRelevance(gene));
                    double enhancerRelevance = 0.;
                    for (Enhancer enhancer : tadEnhancers) {
                        enhancerRelevance += enhancerImpactCalculator.noImpact() * enhancerGeneRelevanceCalculator.calculateRelevance(enhancer);
                    }
                    double score = geneImpact * geneRelevance + enhancerRelevance;
                    // a score for a gene might already be in the results map if the event duplicates the entire gene
                    results.merge(gene.accessionId().getValue(), score, Double::sum);
                }
            }
        }

        return results;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Double> evaluateAlternate(Set<Route> routes,
                                                  Set<Gene> genes,
                                                  Set<Enhancer> enhancers,
                                                  List<TadBoundary> tadBoundaries) {
        Map<String, Double> results = new HashMap<>(genes.size());

        for (Route route : routes) {
            LinkedList<Projection<? extends GenomicRegion>> projections = EvaluatorUtils.projectGenesEnhancersTads(route, genes, enhancers, tadBoundaries);

            if (projections.isEmpty())
                continue;

            // remove TADs that are either first or last projections
            if (projections.getFirst().source() instanceof TadBoundary)
                projections.removeFirst();
            if (projections.isEmpty())
                continue;

            if (projections.getLast().source() instanceof TadBoundary)
                projections.removeLast();
            if (projections.isEmpty())
                continue;

            List<Integer> tadIndices = EvaluatorUtils.computeTadBoundaryIndices(projections);


            int tadStart = 0;
            Iterator<Integer> tadIndicesIterator = tadIndices.iterator();
            int tadEnd = tadIndices.isEmpty() ? projections.size() : tadIndicesIterator.next();
            List<Projection<Gene>> intraTadGenes = new LinkedList<>();
            List<Projection<Enhancer>> intraTadEnhancers = new LinkedList<>();


            while (true) {
                List<Projection<? extends GenomicRegion>> intraTadProjections = projections.subList(tadStart, tadEnd);

                // prepare TAD elements
                for (Projection<? extends GenomicRegion> projection : intraTadProjections) {
                    if (projection.source() instanceof Gene)
                        intraTadGenes.add((Projection<Gene>) projection);
                    else if (projection.source() instanceof Enhancer)
                        intraTadEnhancers.add((Projection<Enhancer>) projection);
                    else
                        LogUtils.logWarn(LOGGER, "Skipping evaluation of an unknown projection `{}`", projection.source().getClass().getSimpleName());
                }

                // process TAD elements
                for (Projection<Gene> gene : intraTadGenes) {
                    double geneImpact = geneImpactCalculator.projectImpact(gene);
                    if (geneImpact < EvaluatorUtils.CLOSE_TO_ZERO)
                        // Loss of function
                        continue;
                    double geneRelevance = Math.exp(geneWeightCalculator.calculateRelevance(gene.source()));

                    double enhancerRelevance = 0.;
                    for (Projection<Enhancer> enhancer : intraTadEnhancers) {
                        enhancerRelevance += enhancerImpactCalculator.projectImpact(enhancer) * enhancerGeneRelevanceCalculator.calculateRelevance(enhancer.source());
                    }

                    double score = geneImpact * geneRelevance + enhancerRelevance;
                    // a score for a gene might already be in the results map if the event duplicates the entire gene
                    results.merge(gene.source().accessionId().getValue(), score, Double::sum);
                }

                intraTadGenes.clear();
                intraTadEnhancers.clear();

                // ----- ----- ----- ----- ----- ----- ----- ----- ----- -----
                tadStart = tadEnd + 1; // do not include the TAD into evaluation
                if (tadIndicesIterator.hasNext())
                    tadEnd = tadIndicesIterator.next();
                else
                if (tadEnd == projections.size())
                    break;
                else
                    tadEnd = projections.size();
            }

        }

        return results;
    }

}
