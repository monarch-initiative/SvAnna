package org.jax.svanna.core.priority.additive.ge;

import org.jax.svanna.core.exception.LogUtils;
import org.jax.svanna.core.landscape.Enhancer;
import org.jax.svanna.core.landscape.TadBoundary;
import org.jax.svanna.core.priority.additive.*;
import org.jax.svanna.core.priority.additive.impact.SequenceImpactCalculator;
import org.jax.svanna.core.reference.Gene;
import org.monarchinitiative.svart.GenomicRegion;
import org.monarchinitiative.svart.Position;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class RouteDataEvaluatorGE implements RouteDataEvaluator<RouteDataGE, RouteResult> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RouteDataEvaluatorGE.class);

    private final SequenceImpactCalculator<Gene> geneImpactCalculator;
    private final GeneWeightCalculator geneWeightCalculator;

    private final SequenceImpactCalculator<Enhancer> enhancerImpactCalculator;
    private final EnhancerGeneRelevanceCalculator enhancerGeneRelevanceCalculator;

    public RouteDataEvaluatorGE(SequenceImpactCalculator<Gene> geneImpactCalculator, GeneWeightCalculator geneWeightCalculator,
                                SequenceImpactCalculator<Enhancer> enhancerImpactCalculator, EnhancerGeneRelevanceCalculator enhancerGeneRelevanceCalculator) {
        this.geneImpactCalculator = Objects.requireNonNull(geneImpactCalculator);
        this.geneWeightCalculator = Objects.requireNonNull(geneWeightCalculator);
        this.enhancerImpactCalculator = Objects.requireNonNull(enhancerImpactCalculator);
        this.enhancerGeneRelevanceCalculator = Objects.requireNonNull(enhancerGeneRelevanceCalculator);
    }

    @Override
    public RouteResult evaluate(RouteDataGE routeData) {
        Routes routes = routeData.route();

        double reference = evaluateReference(routes.reference(), routeData.refTadBoundaries(), routeData.refGenes(), routeData.refEnhancers());
        double alternate = evaluateAlternate(routes.alternate(), routeData.altTadBoundaries(), routeData.altGenes(), routeData.altEnhancers());

        double priority = Math.abs(reference - alternate);
        return new SimpleRouteResult(priority);
    }


    private double evaluateReference(GenomicRegion reference,
                                     List<TadBoundary> tadBoundaries,
                                     Collection<Gene> genes,
                                     Collection<Enhancer> enhancers) {
        // partition the genes and enhancers into TAD regions
        List<GenomicRegion> tadRegions = prepareReferenceTadRegions(tadBoundaries, reference);
        Map<GenomicRegion, List<Gene>> genesByTad = partitionItemsByRegion(genes, tadRegions);
        Map<GenomicRegion, List<Enhancer>> enhancersByTad = partitionItemsByRegion(enhancers, tadRegions);

        double score = 0.;
        for (GenomicRegion tadRegion : tadRegions) {
            double tadScore = evaluateReferenceTad(genesByTad.get(tadRegion), enhancersByTad.get(tadRegion));
            score += tadScore;
        }

        return score;
    }

    private double evaluateReferenceTad(List<Gene> genes, List<Enhancer> enhancers) {
        double score = 0.;
        for (Gene gene : genes) {
            double geneImpact = geneImpactCalculator.noImpact();
            double geneRelevance = Math.exp(geneWeightCalculator.calculateRelevance(gene));
            double enhancerRelevance = 0.;
            for (Enhancer enhancer : enhancers) {
                enhancerRelevance += enhancerImpactCalculator.noImpact() * enhancerGeneRelevanceCalculator.calculateRelevance(enhancer);
            }
            score += geneImpact * geneRelevance + enhancerRelevance;
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
            projections.addAll(Projections.project(boundary, alternate));
        }

        for (Gene gene : genes) {
            projections.addAll(Projections.project(gene, alternate));
        }

        for (Enhancer enhancer : enhancers) {
            projections.addAll(Projections.project(enhancer, alternate));
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
            double geneRelevance = Math.exp(geneWeightCalculator.calculateRelevance(gene.source()));

            double enhancerRelevance = 0.;
            for (Projection<Enhancer> enhancer : enhancers) {
                enhancerRelevance += enhancerImpactCalculator.projectImpact(enhancer) * enhancerGeneRelevanceCalculator.calculateRelevance(enhancer.source());
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
            List<TadBoundary> sortedTads = tadBoundaries.stream()
                    .sorted(Comparator.comparingInt(tb -> tb.withStrand(reference.strand()).asPosition().pos()))
                    .collect(Collectors.toList());

            LinkedList<GenomicRegion> regions = new LinkedList<>();
            Position previous = sortedTads.get(0).withStrand(reference.strand()).asPosition();
            for (int i = 1; i < sortedTads.size(); i++) {
                Position current = sortedTads.get(i).withStrand(reference.strand()).asPosition();
                regions.add(GenomicRegion.of(reference.contig(), reference.strand(), reference.coordinateSystem(), previous, current));
                previous = current;
            }

            GenomicRegion first = regions.removeFirst();
            int firstStart = first.startOnStrandWithCoordinateSystem(reference.strand(), reference.coordinateSystem());
            if (firstStart > reference.start())
                regions.addFirst(GenomicRegion.of(reference.contig(), reference.strand(), reference.coordinateSystem(), reference.startPosition(), first.endPosition()));
            else
                regions.addFirst(first);

            GenomicRegion last = regions.removeLast();
            int lastEnd = last.endOnStrandWithCoordinateSystem(reference.strand(), reference.coordinateSystem());
            if (lastEnd < reference.end())
                regions.addLast(GenomicRegion.of(reference.contig(), reference.strand(), reference.coordinateSystem(), last.startPosition(), reference.endPosition()));
            else
                regions.addLast(last);

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
