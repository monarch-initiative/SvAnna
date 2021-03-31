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
import java.util.stream.Stream;

@SuppressWarnings("DuplicatedCode") // TODO - evaluate whether to make an abstract base class for this and RouteDataEvaluatorGE
public class GranularRouteDataEvaluatorGE implements RouteDataEvaluator<RouteDataGE, GranularRouteResult> {

    private static final Logger LOGGER = LoggerFactory.getLogger(GranularRouteDataEvaluatorGE.class);
    private static final double CLOSE_TO_ZERO = 1E-9;

    private final SequenceImpactCalculator<Gene> geneImpactCalculator;
    private final GeneWeightCalculator geneWeightCalculator;

    private final SequenceImpactCalculator<Enhancer> enhancerImpactCalculator;
    private final EnhancerGeneRelevanceCalculator enhancerGeneRelevanceCalculator;

    public GranularRouteDataEvaluatorGE(SequenceImpactCalculator<Gene> geneImpactCalculator,
                                        GeneWeightCalculator geneWeightCalculator,
                                        SequenceImpactCalculator<Enhancer> enhancerImpactCalculator,
                                        EnhancerGeneRelevanceCalculator enhancerGeneRelevanceCalculator) {
        this.geneImpactCalculator = Objects.requireNonNull(geneImpactCalculator);
        this.geneWeightCalculator = Objects.requireNonNull(geneWeightCalculator);
        this.enhancerImpactCalculator = Objects.requireNonNull(enhancerImpactCalculator);
        this.enhancerGeneRelevanceCalculator = Objects.requireNonNull(enhancerGeneRelevanceCalculator);
    }

    @Override
    public GranularRouteResult evaluate(RouteDataGE routeData) {
        Routes route = routeData.route();

        Map<String, Double> referenceScores = evaluateReference(route.reference(), routeData.refTadBoundaries(), routeData.refGenes(), routeData.refEnhancers());
        Map<String, Double> alternateScores = evaluateAlternate(route.alternate(), routeData.altTadBoundaries(), routeData.altGenes(), routeData.altEnhancers());

        Set<String> geneAccessions = Stream.concat(routeData.refGenes().stream(), routeData.altGenes().stream())
                .map(g -> g.accessionId().getValue())
                .collect(Collectors.toSet());


        Map<String, Double> scoreMap = new HashMap<>();
        for (String geneAccession : geneAccessions) {
            double refScore = referenceScores.getOrDefault(geneAccession, 0.D);
            double altScore = alternateScores.getOrDefault(geneAccession, 0.D);

            double score = Math.abs(refScore - altScore);
            if (score > CLOSE_TO_ZERO)
                scoreMap.put(geneAccession, score);
        }
        return GranularRouteResult.of(Map.copyOf(scoreMap));
    }

    private Map<String, Double> evaluateReference(GenomicRegion reference,
                                                  List<TadBoundary> tadBoundaries,
                                                  Collection<Gene> genes,
                                                  Collection<Enhancer> enhancers) {
        // partition the genes and enhancers into TAD regions
        List<GenomicRegion> tadRegions = prepareReferenceTadRegions(tadBoundaries, reference);
        Map<GenomicRegion, List<Gene>> genesByTad = partitionItemsByRegion(genes, tadRegions);
        Map<GenomicRegion, List<Enhancer>> enhancersByTad = partitionItemsByRegion(enhancers, tadRegions);

        Map<String, Double> scoreMap = new HashMap<>(genes.size());
        for (GenomicRegion tadRegion : tadRegions) {
            List<Enhancer> tadEnhancers = enhancersByTad.get(tadRegion);
            for (Gene gene : genesByTad.get(tadRegion)) {
                double geneImpact = geneImpactCalculator.noImpact();
                double geneRelevance = Math.exp(geneWeightCalculator.calculateRelevance(gene));
                double enhancerRelevance = 0.;
                for (Enhancer enhancer : tadEnhancers) {
                    enhancerRelevance += enhancerImpactCalculator.noImpact() * enhancerGeneRelevanceCalculator.calculateRelevance(enhancer);
                }
                double score = geneImpact * geneRelevance + enhancerRelevance;
                scoreMap.put(gene.accessionId().getValue(), score);
            }
        }

        return scoreMap;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Double> evaluateAlternate(Route alternate,
                                                  List<TadBoundary> tadBoundaries,
                                                  Collection<Gene> genes,
                                                  Collection<Enhancer> enhancers) {
        LinkedList<Projection<? extends GenomicRegion>> projections = new LinkedList<>();

        for (TadBoundary boundary : tadBoundaries) {
            projections.addAll(Projections.project(boundary, alternate));
        }

        for (Gene gene : genes) {
            projections.addAll(Projections.project(gene, alternate));
        }

        for (Enhancer enhancer : enhancers) {
            projections.addAll(Projections.project(enhancer, alternate));
        }

        projections.sort(GenomicRegion::compare);

        Map<String, Double> results = new HashMap<>();

        if (projections.isEmpty())
            return results;

        // remove TADs that are either first or last projections
        if (projections.getFirst().source() instanceof TadBoundary)
            projections.removeFirst();
        if (projections.isEmpty())
            return results;

        if (projections.getLast().source() instanceof TadBoundary)
            projections.removeLast();
        if (projections.isEmpty())
            return results;

        List<Integer> tadIndices = computeTadBoundaryIndices(projections);


        int tadStart = 0;
        int tadEnd = tadIndices.isEmpty() ? projections.size() : tadIndices.get(0);
        List<Projection<Gene>> genesToEvaluate = new LinkedList<>();
        List<Projection<Enhancer>> enhancersToEvaluate = new LinkedList<>();

        Iterator<Integer> tadIndicesIterator = tadIndices.iterator();

        while (true) {
            List<Projection<? extends GenomicRegion>> tadElements = projections.subList(tadStart, tadEnd);

            // prepare TAD elements
            for (Projection<? extends GenomicRegion> tadElement : tadElements) {
                if (tadElement.source() instanceof Gene)
                    genesToEvaluate.add((Projection<Gene>) tadElement);
                else if (tadElement.source() instanceof Enhancer)
                    enhancersToEvaluate.add((Projection<Enhancer>) tadElement);
                else
                    LogUtils.logWarn(LOGGER, "Skipping evaluation of an unknown element `{}`", tadElement.source().getClass().getSimpleName());
            }

            // process TAD elements
            for (Projection<Gene> gene : genesToEvaluate) {
                double geneImpact = geneImpactCalculator.projectImpact(gene);
                if (geneImpact < CLOSE_TO_ZERO)
                    // Loss of function
                    continue;
                double geneRelevance = Math.exp(geneWeightCalculator.calculateRelevance(gene.source()));

                double enhancerRelevance = 0.;
                for (Projection<Enhancer> enhancer : enhancersToEvaluate) {
                    enhancerRelevance += enhancerImpactCalculator.projectImpact(enhancer) * enhancerGeneRelevanceCalculator.calculateRelevance(enhancer.source());
                }
                if (results.containsKey(gene.source().accessionId().getValue()))
                    // TODO - remove once it never happens
                    LogUtils.logWarn(LOGGER, "BUG - Processing gene {}(`{}`) for the second time",
                            gene.source().accessionId().getValue(), gene.source().geneSymbol());

                double score = geneImpact * geneRelevance + enhancerRelevance;

                results.put(gene.source().accessionId().getValue(), score);
            }

            genesToEvaluate.clear();
            enhancersToEvaluate.clear();
            // --
            tadStart = tadEnd + 1; // do not include the TAD into evaluation
            if (tadIndicesIterator.hasNext())
                tadEnd = tadIndicesIterator.next();
            else
                if (tadEnd == projections.size())
                    break;
                else
                    tadEnd = projections.size();
        }

        return results;
    }

    private static List<Integer> computeTadBoundaryIndices(List<Projection<? extends GenomicRegion>> projections) {
        List<Integer> tadIndices = new ArrayList<>(3);
        int i = 0;
        for (Projection<? extends GenomicRegion> projection : projections) {
            if (projection.source() instanceof TadBoundary)
                tadIndices.add(i);
            i++;
        }
        return tadIndices;
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
