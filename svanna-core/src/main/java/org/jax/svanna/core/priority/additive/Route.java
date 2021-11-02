package org.jax.svanna.core.priority.additive;

import org.monarchinitiative.svart.Contig;
import org.monarchinitiative.svart.CoordinateSystem;
import org.monarchinitiative.svart.GenomicRegion;
import org.monarchinitiative.svart.Strand;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Route {

    private static final AtomicInteger CONTIG_ID_COUNTER = new AtomicInteger();

    private final List<Segment> segments;

    private final Set<Contig> segmentContigs;

    private final Contig neoContig;

    private final Comparator<? super GenomicRegion> comparator;

    public static Route of(List<Segment> segments) {
        if (segments.isEmpty())
            throw new IllegalArgumentException("Segment list must not be empty");
        // TODO - normalize coordinate systems, strands
        return new Route(segments);
    }

    private Route(List<Segment> segments) {
        this.segments = segments;
        this.comparator = featureComparator(segments);
        int length = 0;
        this.segmentContigs = new HashSet<>(2);
        for (Segment segment : segments) {
            length += segment.contributingBases();
            segmentContigs.add(segment.contig());
        }
        int contigId = CONTIG_ID_COUNTER.decrementAndGet();
        this.neoContig = NeoContig.of(contigId, "neo-contig" + contigId, length);
    }

    public List<Segment> segments() {
        return segments;
    }

    public List<GenomicRegion> metaSegments() {
        List<GenomicRegion> metaSegments = new ArrayList<>(segmentContigs.size());
        Segment previous = segments.get(0);
        Strand strand = previous.strand();
        int start = previous.start();
        for (int i = 1; i < segments.size(); i++) {
            Segment current = segments.get(i);
            if (!current.contig().equals(previous.contig())) {
                GenomicRegion metaSegment = GenomicRegion.of(previous.contig(), strand, previous.coordinateSystem(), start, previous.end());
                metaSegments.add(metaSegment);
                start = current.start();
                strand = current.strand();
            }
            previous = current;
        }
        GenomicRegion metaSegment = GenomicRegion.of(previous.contig(), strand, previous.coordinateSystem(), start, previous.end());
        metaSegments.add(metaSegment);
        return metaSegments;
    }

    public Contig neoContig() {
        return neoContig;
    }

    public Set<Contig> segmentContigs() {
        return segmentContigs;
    }

    public Comparator<? super GenomicRegion> featureComparator() {
        return comparator;
    }

    private static Comparator<? super GenomicRegion> featureComparator(List<Segment> segments) {
        List<Integer> contigIds = new ArrayList<>(3);
        Map<Integer, Strand> contigStrands = new HashMap<>();
        for (Segment segment : segments) {
            if (!contigIds.contains(segment.contigId()))
                contigIds.add(segment.contigId());
            if (!contigStrands.containsKey(segment.contigId()))
                contigStrands.put(segment.contigId(), segment.strand());
        }

        return (l, r) -> {
            int leftContigIndex = contigIds.indexOf(l.contigId());
            int rightContigIndex = contigIds.indexOf(r.contigId());
            if (leftContigIndex != rightContigIndex)
                return leftContigIndex < rightContigIndex ? -1 : 1;

            Strand strand = contigStrands.get(l.contigId());
            int leftStart = l.startOnStrandWithCoordinateSystem(strand, CoordinateSystem.zeroBased());
            int rightStart = r.startOnStrandWithCoordinateSystem(strand, CoordinateSystem.zeroBased());
            if (leftStart != rightStart)
                return leftStart < rightStart ? -1 : 1;

            int leftEnd = l.endOnStrandWithCoordinateSystem(strand, CoordinateSystem.zeroBased());
            int rightEnd = r.endOnStrandWithCoordinateSystem(strand, CoordinateSystem.zeroBased());
            return Integer.compare(leftEnd, rightEnd);
        };
    }
}
