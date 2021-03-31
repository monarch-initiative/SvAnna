package org.jax.svanna.db.additive.dispatch;

import org.jax.svanna.core.exception.LogUtils;
import org.jax.svanna.core.priority.additive.*;
import org.jax.svanna.core.reference.SvannaVariant;
import org.monarchinitiative.svart.*;

import java.util.LinkedList;
import java.util.List;

public class DispatcherDb implements Dispatcher {

    private static final CoordinateSystem CS = CoordinateSystem.zeroBased();

    private final NeighborhoodBuilder neighborhoodBuilder;

    public DispatcherDb(NeighborhoodBuilder neighborhoodBuilder) {
        this.neighborhoodBuilder = neighborhoodBuilder;
    }

    @Override
    public <V extends Variant> Routes assembleRoutes(List<V> variants) throws DispatchException {
        // variants are sorted and put to the same strand - either POSITIVE or NEGATIVE
        VariantArrangement<V> sortedVariants = RouteAssembly.assemble(variants);

        Neighborhood neighborhood = sortedVariants.hasBreakend()
                ? neighborhoodBuilder.interchromosomalNeighborhood(sortedVariants)
                : neighborhoodBuilder.intrachromosomalNeighborhood(sortedVariants);

        return buildRoutes(neighborhood, variants);
    }

    static <T extends Variant> Routes buildRoutes(Neighborhood neighborhood, List<T> variants) {
        GenomicRegion reference = buildReferencePath(neighborhood.upstream(), neighborhood.downstreamRef());
        Route alternate = buildAltRoute(neighborhood.upstream(), neighborhood.downstreamAlt(), variants);
        return Routes.of(reference, alternate);
    }

    private static GenomicRegion buildReferencePath(GenomicRegion upstream, GenomicRegion downstream) {
        validateReferenceInput(upstream, downstream);

        return GenomicRegion.of(upstream.contig(), upstream.strand(), upstream.coordinateSystem(),
                upstream.start(),
                downstream.endOnStrandWithCoordinateSystem(upstream.strand(), upstream.coordinateSystem()));
    }

    private static void validateReferenceInput(GenomicRegion upstream, GenomicRegion downstream) {
        if (upstream == null)
            throw new IllegalArgumentException("Upstream region cannot be null");

        if (downstream == null)
            throw new IllegalArgumentException("Downstream region cannot be null");

        if (!upstream.contig().equals(downstream.contig()))
            throw new IllegalArgumentException("Upstream and downstream segments must be on the same contig for the reference path");
    }

    private static <T extends Variant> Route buildAltRoute(GenomicRegion upstream, GenomicRegion downstream, List<T> variants) {
        T first = variants.get(0);
        if (!upstream.contig().equals(first.contig()))
            throw new DispatchException("Upstream must be on the same contig as the first variant");
        int firstStart = first.startWithCoordinateSystem(CS);

        List<Segment> segments = new LinkedList<>();

        int upstreamStart = Math.min(upstream.startOnStrandWithCoordinateSystem(first.strand(), CS), upstream.endOnStrandWithCoordinateSystem(first.strand(), CS));
        Segment firstSegment = Segment.of(upstream.contig(), first.strand(), CS,
                Position.of(upstreamStart), Position.of(firstStart),
                "upstream", Event.GAP, 1);
        segments.add(firstSegment);
        segments.addAll(makeVariantSegment(first, first.strand()));


        for (int i = 1; i < variants.size(); i++) {
            T previous = variants.get(i - 1);
            T current = variants.get(i);
            if (!previous.contig().equals(current.contig()))
                throw new DispatchException("Different contigs (" + previous.contigName() + " vs. " + current.contigName() + " )" +
                        " in variants " + LogUtils.variantSummary(previous) + " and " + LogUtils.variantSummary(current));

            int gapStart = previous.endOnStrandWithCoordinateSystem(previous.strand(), CS);
            int gapEnd = current.startOnStrandWithCoordinateSystem(previous.strand(), CS);
            Segment gap = Segment.of(previous.contig(), previous.strand(), CS, Position.of(gapStart), Position.of(gapEnd), "gap-" + i, Event.GAP, 1);
            segments.add(gap);

            segments.addAll(makeVariantSegment(current, previous.strand()));
        }


        T last = variants.get(variants.size() - 1);
        Strand lastStrand;
        int lastEnd;
        if (last instanceof BreakendVariant) {
            Breakend right = ((BreakendVariant) last).right();
            lastStrand = right.strand();
            lastEnd = right.endWithCoordinateSystem(CS);
        } else{
            lastStrand = last.strand();
            lastEnd = last.endWithCoordinateSystem(CS);
        }

        int downstreamEnd = downstream.endOnStrandWithCoordinateSystem(lastStrand, CS);
        segments.add(Segment.of(downstream.contig(), lastStrand, CS,
                Position.of(lastEnd), Position.of(downstreamEnd),
                "downstream", Event.GAP, 1));


        return Route.of(segments);
    }

    private static <T extends Variant> List<Segment> makeVariantSegment(T variant, Strand previous) {
        List<Segment> segments;
        // variant coordinates are always on the Strand.POSITIVE, except for breakends
        int start = variant.startOnStrandWithCoordinateSystem(previous, CS);
        int end = variant.endOnStrandWithCoordinateSystem(previous, CS);
        switch (variant.variantType().baseType()) {
            case SNV:
                segments = List.of(Segment.of(variant.contig(), previous, CS, Position.of(start), Position.of(end),
                        variant.id(), Event.SNV, 1));
                break;
            case INV:
                segments = List.of(Segment.of(variant.contig(), previous, CS,
                        Position.of(start), Position.of(end),
                        variant.id(), Event.INVERSION, 1));
                break;
            case DEL:
                segments = List.of(Segment.of(variant.contig(), previous, CS, Position.of(start), Position.of(end),
                        variant.id(), Event.DELETION, 0));
                break;
            case DUP:
                segments = List.of(Segment.of(variant.contig(), previous, CS, Position.of(start), Position.of(end),
                        variant.id(), Event.DUPLICATION, 2));
                break;
            case INS:
                segments = List.of(Segment.insertion(variant.contig(), previous, CS, Position.of(start), Position.of(end),
                        variant.id(), variant.changeLength()));
                break;
            case BND:
                try {
                    BreakendVariant bnd = (BreakendVariant) variant;
                    Breakend left = bnd.left();
                    Breakend right = bnd.right();
                    segments = List.of(
                            Segment.of(left.contig(), left.strand(), CS,
                                    left.startPositionWithCoordinateSystem(CS), left.endPositionWithCoordinateSystem(CS),
                                    left.id(), Event.BREAKEND, 1),
                            Segment.of(right.contig(), right.strand(), CS,
                                    right.startPositionWithCoordinateSystem(CS), right.endPositionWithCoordinateSystem(CS),
                                    right.id(), Event.BREAKEND, 1));
                    break;
                } catch (ClassCastException e) {
                    throw new DispatchException("Should have been breakend but was " + variant.getClass().getSimpleName());
                }
            case CNV:
                if (variant instanceof SvannaVariant) {
                    SvannaVariant sv = (SvannaVariant) variant;
                    int copyNumber = sv.copyNumber();
                    if (copyNumber < 1 || copyNumber == 2)
                        throw new DispatchException("Copy number was `" + copyNumber + "`");

                    segments = List.of(Segment.of(variant.contig(), previous, CS, Position.of(start), Position.of(end),
                            variant.id(), Event.DELETION, copyNumber - 1));
                    break;
                }
            default:
                throw new DispatchException("Unsupported variant type " + variant.variantType());
        }
        return segments;
    }

}
