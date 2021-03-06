package org.jax.svanna.db.additive.dispatch;

import org.jax.svanna.core.exception.LogUtils;
import org.jax.svanna.core.landscape.TadBoundary;
import org.jax.svanna.core.priority.additive.*;
import org.jax.svanna.db.landscape.TadBoundaryDao;
import org.monarchinitiative.svart.*;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public class DispatcherDb implements Dispatcher {

    private static final CoordinateSystem CS = CoordinateSystem.zeroBased();

    private final TadBoundaryDao tadBoundaryDao;

    public DispatcherDb(TadBoundaryDao tadBoundaryDao) {
        this.tadBoundaryDao = tadBoundaryDao;
    }

    @Override
    public <V extends Variant> Routes assembleRoutes(List<V> variants) throws DispatchException {
        // variants are sorted and put to the same strand - either POSITIVE or NEGATIVE
        VariantArrangement<V> sortedVariants = RouteAssembly.assemble(variants);

        Neighborhood neighborhood = sortedVariants.hasBreakend()
                ? interchromosomalNeighborhood(sortedVariants)
                : intrachromosomalNeighborhood(sortedVariants);

        return buildRoutes(neighborhood, variants);
    }

    private <V extends Variant> Neighborhood intrachromosomalNeighborhood(VariantArrangement<V> arrangement) {
        LinkedList<V> variants = arrangement.variants();
        V first = variants.getFirst();
        V last = variants.getLast();
        if (first.strand() != last.strand())
            throw new DispatchException("First and last variants must be on the same strand");

        Optional<TadBoundary> upstreamTad = tadBoundaryDao.upstreamOf(first);
        // empty when there is no TAD upstream, just the end of the chromosome
        GenomicRegion upstream = upstreamTad.isPresent()
                ? upstreamTad.get()
                : GenomicRegion.of(first.contig(), first.strand(), CoordinateSystem.zeroBased(), Position.of(0), Position.of(1));

        Optional<TadBoundary> downstreamTad = tadBoundaryDao.downstreamOf(last);
        // empty when there is no TAD downstream, just the end of the chromosome
        GenomicRegion downstream = downstreamTad.isPresent()
                ? downstreamTad.get()
                : GenomicRegion.of(last.contig(), last.strand(), CoordinateSystem.zeroBased(), last.contig().length() - 1, last.contig().length());

        return Neighborhood.of(upstream, downstream, downstream);
    }

    private <V extends Variant> Neighborhood interchromosomalNeighborhood(VariantArrangement<V> arrangement) {
        LinkedList<V> variants = arrangement.variants();

        BreakendVariant bv = (BreakendVariant) variants.get(arrangement.breakendIndex());
        Breakend left = bv.left();
        Breakend right = bv.right();

        V first = variants.getFirst();
        GenomicRegion leftmost = (first instanceof BreakendVariant)
                ? ((BreakendVariant) first).left()
                : first;

        Position upstreamBound = tadBoundaryDao.upstreamOf(leftmost)
                .map(TadBoundary::asPosition)
                .orElse(Position.of(0));
        GenomicRegion upstream = GenomicRegion.of(left.contig(), left.strand(), CoordinateSystem.zeroBased(), upstreamBound, upstreamBound);

        Position downstreamRefBound = tadBoundaryDao.downstreamOf(left)
                .map(TadBoundary::asPosition)
                .orElse(Position.of(left.contig().length()));
        GenomicRegion downstreamRef = GenomicRegion.of(left.contig(), left.strand(), CoordinateSystem.zeroBased(), downstreamRefBound, downstreamRefBound);

        V last = variants.getLast();
        GenomicRegion rightmost= (last instanceof BreakendVariant)
                ? ((BreakendVariant) last).right()
                : last;
        Position downstreamAltBound = tadBoundaryDao.downstreamOf(rightmost)
                .map(TadBoundary::asPosition)
                .orElse(Position.of(right.contig().length()));
        GenomicRegion downstreamAlt = GenomicRegion.of(right.contig(), right.strand(), right.coordinateSystem(), downstreamAltBound, downstreamAltBound);

        return Neighborhood.of(upstream, downstreamRef, downstreamAlt);
    }


    static <T extends Variant> Routes buildRoutes(Neighborhood neighborhood, List<T> variants) {
        GenomicRegion reference = buildReferencePath(neighborhood.upstream(), neighborhood.downstreamRef());
        Route alternate = buildAltRoute(neighborhood.upstream(), neighborhood.downstreamAlt(), variants);
        return Routes.of(reference, alternate);
    }

    private static <T extends Variant> GenomicRegion buildReferencePath(GenomicRegion upstream, GenomicRegion downstream) {
        validateReferenceInput(upstream, downstream);

        return GenomicRegion.of(upstream.contig(), upstream.strand(), upstream.coordinateSystem(),
                upstream.start(),
                downstream.endOnStrandWithCoordinateSystem(upstream.strand(), upstream.coordinateSystem()));
    }

    private static <T extends Variant> void validateReferenceInput(GenomicRegion upstream, GenomicRegion downstream) {
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
                                    right.id(), Event.BREAKEND,  1));
                    break;
                } catch (ClassCastException e) {
                    throw new DispatchException("Should have been breakend but was " + variant.getClass().getSimpleName());
                }
            default:
                throw new DispatchException("Unsupported variant type " + variant.variantType());
        }
        return segments;
    }

}
