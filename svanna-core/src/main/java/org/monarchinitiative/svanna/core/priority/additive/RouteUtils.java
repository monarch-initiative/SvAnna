package org.monarchinitiative.svanna.core.priority.additive;

import org.monarchinitiative.svanna.core.LogUtils;
import org.monarchinitiative.svanna.core.reference.SvannaVariant;
import org.monarchinitiative.svart.*;

import java.util.LinkedList;
import java.util.List;

public class RouteUtils {

    private static final CoordinateSystem CS = CoordinateSystem.zeroBased();

    private RouteUtils(){}

    /**
     * @param upstreamBound   zero based start coordinate of the upstream region
     * @param downstreamBound zero based end coordinate of the downstream region
     * @param variants        list of variants to build the route from
     * @return route
     */
    public static Route buildRoute(int upstreamBound, int downstreamBound, List<? extends GenomicVariant> variants) {
        GenomicVariant first = variants.get(0);
        int firstStart = first.startWithCoordinateSystem(CS);

        List<Segment> segments = new LinkedList<>();

        Segment firstSegment = Segment.of(first.contig(), first.strand(), Coordinates.of(CS, upstreamBound, firstStart),
                "upstream", Event.GAP, 1);

        segments.add(firstSegment);
        segments.addAll(makeVariantSegment(first, first.strand()));


        for (int i = 1; i < variants.size(); i++) {
            GenomicVariant previous = variants.get(i - 1);
            GenomicVariant current = variants.get(i);
            if (!previous.contig().equals(current.contig()))
                throw new DispatchException("Different contigs (" + previous.contigName() + " vs. " + current.contigName() + " )" +
                        " in variants " + LogUtils.variantSummary(previous) + " and " + LogUtils.variantSummary(current));

            int gapStart = previous.endOnStrandWithCoordinateSystem(previous.strand(), CS);
            int gapEnd = current.startOnStrandWithCoordinateSystem(previous.strand(), CS);
            Segment gap = Segment.of(previous.contig(), previous.strand(), Coordinates.of(CS, gapStart, gapEnd), "gap-" + i, Event.GAP, 1);
            segments.add(gap);

            segments.addAll(makeVariantSegment(current, previous.strand()));
        }


        GenomicVariant last = variants.get(variants.size() - 1);
        Strand lastStrand;
        int lastEnd;
        if (last instanceof GenomicBreakendVariant) {
            GenomicBreakend right = ((GenomicBreakendVariant) last).right();
            lastStrand = right.strand();
            lastEnd = right.endWithCoordinateSystem(CS);
        } else {
            lastStrand = last.strand();
            lastEnd = last.endWithCoordinateSystem(CS);
        }

        segments.add(Segment.of(last.contig(), lastStrand, Coordinates.of(CS, lastEnd, downstreamBound),
                "downstream", Event.GAP, 1));


        return Route.of(segments);
    }

    private static <T extends GenomicVariant> List<Segment> makeVariantSegment(T variant, Strand previous) {
        List<Segment> segments;
        // variant coordinates are always on the Strand.POSITIVE, except for breakends
        int start = variant.startOnStrandWithCoordinateSystem(previous, CS);
        int end = variant.endOnStrandWithCoordinateSystem(previous, CS);
        switch (variant.variantType().baseType()) {
            case SNV:
                segments = List.of(Segment.of(variant.contig(), previous, Coordinates.of(CS, start, end),
                        variant.id(), Event.SNV, 1));
                break;
            case INV:
                segments = List.of(Segment.of(variant.contig(), previous, Coordinates.of(CS, start, end),
                        variant.id(), Event.INVERSION, 1));
                break;
            case DEL:
                segments = List.of(Segment.of(variant.contig(), previous, Coordinates.of(CS, start, end),
                        variant.id(), Event.DELETION, 0));
                break;
            case DUP:
                segments = List.of(Segment.of(variant.contig(), previous, Coordinates.of(CS, start, end),
                        variant.id(), Event.DUPLICATION, 2));
                break;
            case INS:
                segments = List.of(Segment.insertion(variant.contig(), previous, Coordinates.of(CS, start, end),
                        variant.id(), variant.changeLength()));
                break;
            case BND:
                try {
                    GenomicBreakendVariant bnd = (GenomicBreakendVariant) variant;
                    GenomicBreakend left = bnd.left();
                    GenomicBreakend right = bnd.right();
                    segments = List.of(
                            Segment.of(left.contig(), left.strand(), Coordinates.of(CS, left.startWithCoordinateSystem(CS), left.endWithCoordinateSystem(CS)),
                                    left.id(), Event.BREAKEND, 1),
                            Segment.of(right.contig(), right.strand(), Coordinates.of(CS, right.startWithCoordinateSystem(CS), right.endWithCoordinateSystem(CS)),
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

                    segments = List.of(Segment.of(variant.contig(), previous, Coordinates.of(CS, start, end),
                            variant.id(), Event.DELETION, copyNumber - 1));
                    break;
                }
            default:
                throw new DispatchException("Unsupported variant type " + variant.variantType());
        }
        return segments;
    }
}
