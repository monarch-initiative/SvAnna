package org.jax.svanna.core.priority.additive;

import org.jax.svanna.model.landscape.enhancer.Enhancer;
import org.jax.svanna.model.landscape.tad.TadBoundary;
import org.monarchinitiative.sgenes.model.Gene;
import org.monarchinitiative.sgenes.model.Located;
import org.monarchinitiative.svart.Contig;
import org.monarchinitiative.svart.CoordinateSystem;
import org.monarchinitiative.svart.GenomicRegion;
import org.monarchinitiative.svart.Strand;

import java.util.*;
import java.util.stream.Collectors;

public class EvaluatorUtils {

    public static final double CLOSE_TO_ZERO = 1E-9;

    private static final Comparator<? super Projection<?>> COMPARATOR = prepareComparator();

    private EvaluatorUtils() {
    }

    private static Comparator<? super Projection<?>> prepareComparator() {
        return Comparator.comparingInt(GenomicRegion::contigId)
                .thenComparingInt(p -> p.startOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased()))
                .thenComparingInt(p -> p.endOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased()));
    }

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
            GenomicRegion region = tadBoundaries.get(0).location();
            int mid = region.endOnStrandWithCoordinateSystem(reference.strand(), reference.coordinateSystem()) - (region.length() / 2);
            return List.of(
                    GenomicRegion.of(reference.contig(), reference.strand(), reference.coordinateSystem(), reference.start(), mid),
                    GenomicRegion.of(reference.contig(), reference.strand(), reference.coordinateSystem(), mid, reference.end())
            );
        } else {
            List<TadBoundary> sortedTads = tadBoundaries.stream()
                    .sorted(Comparator.comparingInt(tb -> {
                        GenomicRegion region = tb.location();
                        return region.endOnStrandWithCoordinateSystem(reference.strand(), reference.coordinateSystem()) - (region.length() / 2);
                    }))
                    .collect(Collectors.toList());

            LinkedList<GenomicRegion> regions = new LinkedList<>();
            GenomicRegion tadRegion = sortedTads.get(0).location();
            int previousTadMidpoint = tadRegion.endOnStrandWithCoordinateSystem(reference.strand(), CoordinateSystem.zeroBased()) - (tadRegion.length() / 2);
            for (int i = 1; i < sortedTads.size(); i++) {
                GenomicRegion currentRegion = sortedTads.get(i).location();
                int currentTadMidpoint = currentRegion.endOnStrandWithCoordinateSystem(reference.strand(), CoordinateSystem.zeroBased()) - (currentRegion.length() / 2);
                regions.add(GenomicRegion.of(reference.contig(), reference.strand(), reference.coordinateSystem(), previousTadMidpoint, currentTadMidpoint));
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

    public static <T extends Located> Map<GenomicRegion, List<T>> groupItemsByRegion(List<GenomicRegion> regions, Collection<T> items) {
        Map<GenomicRegion, List<T>> map = new HashMap<>(regions.size());
        for (GenomicRegion region : regions) {
            List<T> partition = items.stream()
                    .filter(t -> t.location().overlapsWith(region))
                    .collect(Collectors.toUnmodifiableList());
            map.put(region, partition);
        }

        return map;
    }

    public static List<Projection<? extends Located>> projectGenesEnhancers(Route alternate,
                                                                            Collection<Gene> genes,
                                                                            Collection<Enhancer> enhancers) {
        List<Projection<? extends Located>> projections = new ArrayList<>(genes.size() + enhancers.size());

        for (Gene gene : genes) {
            projections.addAll(Projections.project(gene, alternate));
        }

        for (Enhancer enhancer : enhancers) {
            projections.addAll(Projections.project(enhancer, alternate));
        }

        return projections;
    }

    public static LinkedList<Projection<? extends Located>> projectGenesEnhancersTads(Route alternate,
                                                                                      Collection<Gene> genes,
                                                                                      Collection<Enhancer> enhancers,
                                                                                      Collection<TadBoundary> tadBoundaries) {
        LinkedList<Projection<? extends Located>> projections = new LinkedList<>();

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

    public static List<Integer> computeTadBoundaryIndices(List<Projection<? extends Located>> projections) {
        List<Integer> tadIndices = new LinkedList<>();
        int i = 0;
        for (Projection<? extends Located> projection : projections) {
            if (projection.source() instanceof TadBoundary)
                tadIndices.add(i);
            i++;
        }
        return tadIndices;
    }
}
