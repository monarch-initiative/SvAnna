package org.jax.svanna.db.additive.dispatch;

import org.jax.svanna.core.exception.LogUtils;
import org.jax.svanna.core.landscape.TadBoundary;
import org.jax.svanna.core.priority.additive.*;
import org.monarchinitiative.svart.*;

import javax.sql.DataSource;
import java.util.LinkedList;
import java.util.List;

public class DispatcherDb implements Dispatcher {

    private static final CoordinateSystem CS = CoordinateSystem.zeroBased();

    private final PaddingTadBoundaryDao dao;

    public DispatcherDb(DataSource dataSource, GenomicAssembly genomicAssembly, double stabilityThreshold) {
        this.dao = new PaddingTadBoundaryDao(dataSource, genomicAssembly, stabilityThreshold);
    }

    @Override
    public <V extends Variant> Routes assembleRoutes(List<V> variants) throws DispatchException {
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
        int left, right;
        // TODO - this needs to be tested, right now I expect variants to be always on POSITIVE strand
        if (first.strand().isPositive()) {
            left = first.startWithCoordinateSystem(CoordinateSystem.zeroBased());
            right = last.endWithCoordinateSystem(CoordinateSystem.zeroBased());
            TadBoundaryPair tadPair = dao.getBoundaryPair(first.contig(), left, right);
            return Neighborhood.of(tadPair.upstream(), tadPair.downstream(), tadPair.downstream());
        } else {
            left = last.startOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased());
            right = first.endOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased());
            TadBoundaryPair tadPair = dao.getBoundaryPair(first.contig(), left, right);
            return Neighborhood.of(tadPair.downstream(), tadPair.upstream(), tadPair.upstream());
        }
    }

    private <V extends Variant> Neighborhood interchromosomalNeighborhood(VariantArrangement<V> arrangement) {
        LinkedList<V> variants = arrangement.variants();

        BreakendVariant bv = (BreakendVariant) variants.get(arrangement.breakendIndex());
        Breakend left = bv.left();
        Breakend right = bv.right();

        Position upstreamBound = dao.upstreamOf(variants.getFirst())
                .map(TadBoundary::asPosition)
                .orElse(Position.of(0));
        GenomicRegion upstream = GenomicRegion.of(left.contig(), left.strand(), CoordinateSystem.zeroBased(), upstreamBound, upstreamBound);

        Position downstreamRefBound = dao.downstreamOf(left)
                .map(TadBoundary::asPosition)
                .orElse(Position.of(left.contig().length()));
        GenomicRegion downstreamRef = GenomicRegion.of(left.contig(), left.strand(), CoordinateSystem.zeroBased(), downstreamRefBound, downstreamRefBound);

        Position downstreamAltBound = dao.downstreamOf(variants.getLast())
                .map(TadBoundary::asPosition)
                .orElse(Position.of(right.contig().length()));
        GenomicRegion downstreamAlt = GenomicRegion.of(right.contig(), right.strand(), right.coordinateSystem(), downstreamAltBound, downstreamAltBound);

        return Neighborhood.of(upstream, downstreamRef, downstreamAlt);
    }


    static <T extends Variant> Routes buildRoutes(Neighborhood neighborhood, List<T> variants) {
        GenomicRegion reference = buildReferencePath(neighborhood.upstream(), neighborhood.downstreamRef(), variants);
        Route alternate = buildAltRoute(neighborhood.upstream(), neighborhood.downstreamAlt(), variants);
        return Routes.of(reference, alternate);
    }

    private static <T extends Variant> GenomicRegion buildReferencePath(GenomicRegion upstream, GenomicRegion downstream, List<T> variants) {
        validateReferenceInput(upstream, downstream, variants);

        return GenomicRegion.of(upstream.contig(), upstream.strand(), upstream.coordinateSystem(),
                upstream.start(),
                downstream.endOnStrandWithCoordinateSystem(downstream.strand(), downstream.coordinateSystem()));
    }

    private static <T extends Variant> void validateReferenceInput(GenomicRegion upstream, GenomicRegion downstream, List<T> variants) {
        if (upstream == null)
            throw new IllegalArgumentException("Upstream region cannot be null");

        if (downstream == null)
            throw new IllegalArgumentException("Downstream region cannot be null");

        if (variants.isEmpty())
            throw new IllegalArgumentException("Variants must not be empty");

        if (!upstream.contig().equals(downstream.contig()))
            throw new IllegalArgumentException("Upstream and downstream segments must be on the same contig for the reference path");

        if (variants.stream().anyMatch(v -> !v.contig().equals(upstream.contig())))
            throw new IllegalArgumentException("Variant contigs must be the same as the upstream/downstream segment for the reference path");
    }

    private static <T extends Variant> Route buildAltRoute(GenomicRegion upstream, GenomicRegion downstream, List<T> variants) {
        if (variants.isEmpty())
            throw new DispatchException("Variants must not be empty");

        T first = variants.get(0);
        if (!upstream.contig().equals(first.contig()))
            throw new DispatchException("Upstream must be on the same contig as the first variant");
        int upstreamBound = Math.max(upstream.startOnStrandWithCoordinateSystem(first.strand(), CS), upstream.endOnStrandWithCoordinateSystem(first.strand(), CS));
        int firstStart = first.startWithCoordinateSystem(CS);
        if (upstreamBound > firstStart)
            throw new DispatchException("Upstream region must be upstream of the first variant");

        List<Segment> segments = new LinkedList<>();

        int upstreamStart = Math.min(upstream.startOnStrandWithCoordinateSystem(first.strand(), CS), upstream.endOnStrandWithCoordinateSystem(first.strand(), CS));
        segments.add(Segment.of(upstream.contig(), first.strand(), CS, Position.of(upstreamStart), Position.of(firstStart),
                "upstream", Event.GAP, 1));
        segments.addAll(makeVariantSegment(first, first.strand()));


        for (int i = 1; i < variants.size(); i++) {
            T previous = variants.get(i-1);
//            Segment previousSegment = segments.getLast();
            T variant = variants.get(i);
            if (!previous.contig().equals(variant.contig()))
                throw new DispatchException("Different contigs (" + previous.contigName() + " vs. " + variant.contigName() + " )" +
                        " in variants " + LogUtils.variantSummary(previous) + " and " + LogUtils.variantSummary(variant));

            Strand strand = previous.variantType().baseType() != VariantType.INV
                    ? previous.strand()
//                    : previous.strand().opposite();
                    : previous.strand();

            int gapStart = previous.endOnStrandWithCoordinateSystem(strand, CS);
//            int gapStart = previousSegment.endOnStrandWithCoordinateSystem(strand, CS);
            int gapEnd = variant.startOnStrandWithCoordinateSystem(strand, CS);
            Segment gap = Segment.of(previous.contig(), strand, CS, Position.of(gapStart), Position.of(gapEnd), "gap-" + i, Event.GAP, 1);
            segments.add(gap);

            segments.addAll(makeVariantSegment(variant, strand));
        }


        T last = variants.get(variants.size() - 1);
        Contig lastContig;
        Strand lastStrand;
        int lastEnd;
        if (last instanceof BreakendVariant) {
            Breakend right = ((BreakendVariant) last).right();
            lastContig = right.contig();
            lastStrand = right.strand();
            lastEnd = right.endWithCoordinateSystem(CS);
        } else{
            lastContig = last.contig();
            lastStrand = last.strand();
            lastEnd = last.endWithCoordinateSystem(CS);
        }
        if (!downstream.contig().equals(lastContig))
            throw new DispatchException("Downstream segment must be on the same contig as the last variant");
        int downstreamBound = Math.min(downstream.startOnStrandWithCoordinateSystem(lastStrand, CS), downstream.startOnStrandWithCoordinateSystem(lastStrand, CS));

        if (downstreamBound < lastEnd)
            throw new DispatchException("Downstream region must be downstream of the last variant");

        int downstreamEnd = Math.max(downstream.startOnStrandWithCoordinateSystem(lastStrand, CS), downstream.endOnStrandWithCoordinateSystem(lastStrand, CS));
        segments.add(Segment.of(downstream.contig(), lastStrand, CS, Position.of(lastEnd), Position.of(downstreamEnd),
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
                int startInv = variant.startOnStrandWithCoordinateSystem(previous, CS);
                int endInv = variant.endOnStrandWithCoordinateSystem(previous, CS);
                segments = List.of(Segment.of(variant.contig(), previous, CS,
                        Position.of(startInv), Position.of(endInv),
                        variant.id(), Event.INVERSION, 1));
                // TODO - check as this might have been fixed in a wrong way
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
