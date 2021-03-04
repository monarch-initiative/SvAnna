package org.jax.svanna.core.priority.additive.ge;

import org.jax.svanna.core.exception.LogUtils;
import org.jax.svanna.core.landscape.Enhancer;
import org.jax.svanna.core.landscape.TadBoundary;
import org.jax.svanna.core.priority.additive.*;
import org.jax.svanna.core.reference.Gene;
import org.monarchinitiative.svart.GenomicRegion;
import org.monarchinitiative.svart.Position;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class RouteDataEvaluatorGE implements RouteDataEvaluator<RouteDataGE> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RouteDataEvaluatorGE.class);

    private final SequenceImpactCalculator<Gene> geneImpactCalculator;
    private final GeneWeightCalculator geneWeightCalculator;

    private final SequenceImpactCalculator<Enhancer> enhancerImpactCalculator;
    private final EnhancerGeneRelevanceCalculator enhancerGeneRelevanceCalculator;

    public RouteDataEvaluatorGE(SequenceImpactCalculator<Gene> geneImpactCalculator, GeneWeightCalculator geneWeightCalculator,
                                SequenceImpactCalculator<Enhancer> enhancerImpactCalculator, EnhancerGeneRelevanceCalculator enhancerGeneRelevanceCalculator) {
        this.geneImpactCalculator = geneImpactCalculator;
        this.geneWeightCalculator = geneWeightCalculator;
        this.enhancerImpactCalculator = enhancerImpactCalculator;
        this.enhancerGeneRelevanceCalculator = enhancerGeneRelevanceCalculator;
    }

    @Override
    public double evaluate(RouteDataGE routeData) {
        Routes routes = routeData.route();

        double reference = evaluateReference(routes.reference(), routeData.refTadBoundaries(), routeData.refGenes(), routeData.refEnhancers());
        double alternate = evaluateAlternate(routes.alternate(), routeData.altTadBoundaries(), routeData.altGenes(), routeData.altEnhancers());

        return Math.abs(reference - alternate);
    }


    private double evaluateReference(GenomicRegion reference,
                                     List<TadBoundary> tadBoundaries,
                                     Collection<Gene> genes,
                                     Collection<Enhancer> enhancers) {
        // partition the genes and enhancers into TAD regions
        List<GenomicRegion> tadRegions = prepareReferenceTadRegions(tadBoundaries, reference);
        Map<GenomicRegion, List<Gene>> genesByTad = partitionItemsByRegion(genes, tadRegions);
        Map<GenomicRegion, List<Enhancer>> enhancersByTad = partitionItemsByRegion(enhancers, tadRegions);

        return tadRegions.stream()
                .mapToDouble(tadRegion -> evaluateReferenceTad(genesByTad.get(tadRegion), enhancersByTad.get(tadRegion)))
                .sum();
    }

    private double evaluateReferenceTad(List<Gene> genes, List<Enhancer> enhancers) {
        double score = 0.;
        for (Gene gene : genes) {
            double geneImpact = geneImpactCalculator.noImpact();
            double geneRelevance = geneWeightCalculator.calculateRelevance(gene);
            double enhancerContribution = 0.;
            for (Enhancer enhancer : enhancers) {
                enhancerContribution += enhancerImpactCalculator.noImpact() * enhancerGeneRelevanceCalculator.calculateRelevance(gene, enhancer);
            }
            score += geneImpact * geneRelevance + enhancerContribution;
        }
        return score;
    }

    @SuppressWarnings("unchecked")
    private double evaluateAlternate(Route alternate,
                                     List<TadBoundary> tadBoundaries,
                                     Collection<Gene> genes,
                                     Collection<Enhancer> enhancers) {
        SortedSet<Projection<? extends GenomicRegion>> projections = new TreeSet<>(GenomicRegion::compare);

        for (TadBoundary boundary : tadBoundaries) {
            projections.addAll(Projections.projectAll(boundary, alternate));
        }

        for (Gene gene : genes) {
            projections.addAll(Projections.projectAll(gene, alternate));
        }

        for (Enhancer enhancer : enhancers) {
            projections.addAll(Projections.projectAll(enhancer, alternate));
        }

        double score = 0.0;
        List<Projection<Gene>> genesToEvaluate = new ArrayList<>();
        List<Projection<Enhancer>> enhancersToEvaluate = new ArrayList<>();

        for (Projection<? extends GenomicRegion> projection : projections) {

            if (projection.source() instanceof TadBoundary) {
                score += evaluateTad(genesToEvaluate, enhancersToEvaluate);
                genesToEvaluate.clear();
                enhancersToEvaluate.clear();

            } else if (projection.source() instanceof Gene) {
                genesToEvaluate.add((Projection<Gene>) projection);

            } else if (projection.source() instanceof Enhancer) {
                enhancersToEvaluate.add((Projection<Enhancer>) projection);

            } else
                LogUtils.logWarn(LOGGER, "Skipping evaluation of an unknown element `{}`", projection.source().getClass().getSimpleName());

        }
        score += evaluateTad(genesToEvaluate, enhancersToEvaluate);
        return score;
    }

    private double evaluateTad(List<Projection<Gene>> genes, List<Projection<Enhancer>> enhancers) {
        double score = 0.;
        for (Projection<Gene> gene : genes) {
            double geneImpact = geneImpactCalculator.projectImpact(gene);
            if (geneImpact < 1E-12)
                // LoF
                continue;
            double geneRelevance = geneWeightCalculator.calculateRelevance(gene.source());

            double enhancerRelevance = 0.;
            for (Projection<Enhancer> enhancer : enhancers) {
                double enhancerImpact = enhancerImpactCalculator.projectImpact(enhancer);
                double enhancerGeneRelevance = enhancerGeneRelevanceCalculator.calculateRelevance(gene.source(), enhancer.source());
                enhancerRelevance += enhancerImpact * enhancerGeneRelevance;
            }
            score += geneImpact * geneRelevance + enhancerRelevance;
        }
        return score;
    }

    private static List<GenomicRegion> prepareReferenceTadRegions(List<TadBoundary> tadBoundaries, GenomicRegion reference) {
        if (tadBoundaries.isEmpty())
            return List.of(reference);
        else if (tadBoundaries.size() == 1) {
            Position tadPosition = tadBoundaries.get(0).withStrand(reference.strand()).asPosition();
            return List.of(
                    GenomicRegion.of(reference.contig(), reference.strand(), reference.coordinateSystem(), reference.startPosition(), tadPosition),
                    GenomicRegion.of(reference.contig(), reference.strand(), reference.coordinateSystem(), tadPosition, reference.endPosition())
            );
        } else {
            List<GenomicRegion> regions = new ArrayList<>(tadBoundaries.size() - 1);
            for (int i = 1; i < tadBoundaries.size(); i++) {
                TadBoundary previous = tadBoundaries.get(i - 1);
                TadBoundary current = tadBoundaries.get(i);
                regions.add(GenomicRegion.of(current.contig(), current.strand(), current.coordinateSystem(), previous.asPosition(), current.asPosition()));
            }
            return regions;
        }
    }

    private static <T extends GenomicRegion> Map<GenomicRegion, List<T>> partitionItemsByRegion(Collection<T> items, List<GenomicRegion> regions) {
        Map<GenomicRegion, List<T>> map = new HashMap<>();
        for (GenomicRegion region : regions) {
            List<T> partition = items.stream()
                    .filter(t -> t.overlapsWith(region))
                    .collect(Collectors.toList());
            map.put(region, partition);
        }

        return map;
    }

}
