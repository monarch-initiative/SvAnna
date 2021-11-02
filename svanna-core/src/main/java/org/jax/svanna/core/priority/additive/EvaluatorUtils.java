package org.jax.svanna.core.priority.additive;

import org.jax.svanna.model.gene.Gene;
import org.jax.svanna.model.landscape.enhancer.Enhancer;
import org.jax.svanna.model.landscape.tad.TadBoundary;
import org.monarchinitiative.svart.*;

import java.util.*;
import java.util.stream.Collectors;

public class EvaluatorUtils {

    public static final double CLOSE_TO_ZERO = 1E-9;

    private static final Comparator<? super Projection<?>> COMPARATOR = prepareComparator();

    private static Comparator<? super Projection<?>> prepareComparator() {
        return Comparator.comparingInt(GenomicRegion::contigId)
                .thenComparingInt(p -> p.startOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased()))
                .thenComparingInt(p -> p.endOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased()));
    }

    private EvaluatorUtils() {}

    public static Map<Contig, List<GenomicRegion>> prepareEvaluationRegions(Map<Contig, GenomicRegion> referenceRegions,
                                                                            Map<Contig, List<TadBoundary>> tadBoundaries) {
        Map<Contig, List<GenomicRegion>> result = new HashMap<>(referenceRegions.size());

        for (Contig contig : referenceRegions.keySet()) {
            GenomicRegion reference = referenceRegions.get(contig);
            List<TadBoundary> boundaries = tadBoundaries.getOrDefault(contig, List.of());
            result.put(contig, prepareEvaluationRegions(reference, boundaries));
        }

        return result;
    }

    public static List<GenomicRegion> prepareEvaluationRegions(GenomicRegion reference, List<TadBoundary> tadBoundaries) {
        if (tadBoundaries.isEmpty())
            return List.of(reference);
        else if (tadBoundaries.size() == 1) {
            Coordinates tadMidpoint = tadBoundaries.get(0).withStrand(reference.strand()).midpoint();
            return List.of(
                    // TODO - double check the correctness
                    GenomicRegion.of(reference.contig(), reference.strand(), reference.coordinateSystem(), reference.start(), tadMidpoint.start()),
                    GenomicRegion.of(reference.contig(), reference.strand(), reference.coordinateSystem(), tadMidpoint.end(), reference.end())
            );
        } else {
            List<TadBoundary> sortedTads = tadBoundaries.stream()
                    .sorted(Comparator.comparingInt(tb -> tb.withStrand(reference.strand()).midpoint().start()))
                    .collect(Collectors.toList());

            LinkedList<GenomicRegion> regions = new LinkedList<>();
            Coordinates previousTadMidpoint = sortedTads.get(0).withStrand(reference.strand()).midpoint();
            for (int i = 1; i < sortedTads.size(); i++) {
                Coordinates currentTadMidpoint = sortedTads.get(i).withStrand(reference.strand()).midpoint();
                regions.add(GenomicRegion.of(reference.contig(), reference.strand(), reference.coordinateSystem(), previousTadMidpoint.end(), currentTadMidpoint.start()));
                previousTadMidpoint = currentTadMidpoint;
            }

            GenomicRegion first = regions.removeFirst();
            int firstStart = first.startOnStrandWithCoordinateSystem(reference.strand(), reference.coordinateSystem());
            if (firstStart > reference.start())
                // TODO - double check
                regions.addFirst(GenomicRegion.of(reference.contig(), reference.strand(), reference.coordinateSystem(), reference.start(), first.start()));
            else
                regions.addFirst(first);

            GenomicRegion last = regions.removeLast();
            int lastEnd = last.endOnStrandWithCoordinateSystem(reference.strand(), reference.coordinateSystem());
            if (lastEnd < reference.end())
                // TODO - double check
                regions.addLast(GenomicRegion.of(reference.contig(), reference.strand(), reference.coordinateSystem(), last.end(), reference.end()));
            else
                regions.addLast(last);

            return regions;
        }
    }

    public static <T extends GenomicRegion> Map<GenomicRegion, List<T>> groupItemsByRegion(List<GenomicRegion> regions, Collection<T> items) {
        Map<GenomicRegion, List<T>> map = new HashMap<>();
        for (GenomicRegion region : regions) {
            List<T> partition = items.stream()
                    .filter(t -> t.overlapsWith(region))
                    .collect(Collectors.toList());
            map.put(region, partition);
        }

        return map;
    }

    public static LinkedList<Projection<? extends GenomicRegion>> projectGenesEnhancersTads(Route alternate,
                                                                                            Collection<Gene> genes,
                                                                                            Collection<Enhancer> enhancers,
                                                                                            Collection<TadBoundary> tadBoundaries) {
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

        projections.sort(COMPARATOR);

        return projections;
    }

    public static List<Integer> computeTadBoundaryIndices(List<Projection<? extends GenomicRegion>> projections) {
        List<Integer> tadIndices = new LinkedList<>();
        int i = 0;
        for (Projection<? extends GenomicRegion> projection : projections) {
            if (projection.source() instanceof TadBoundary)
                tadIndices.add(i);
            i++;
        }
        return tadIndices;
    }
}
