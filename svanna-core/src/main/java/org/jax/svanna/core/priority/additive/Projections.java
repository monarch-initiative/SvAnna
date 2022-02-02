package org.jax.svanna.core.priority.additive;

import org.jax.svanna.core.LogUtils;
import org.monarchinitiative.svart.CoordinateSystem;
import org.monarchinitiative.svart.Coordinates;
import org.monarchinitiative.svart.Strand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.ielis.silent.genes.model.Located;

import java.util.ArrayList;
import java.util.List;

public class Projections {

    private static final Logger LOGGER = LoggerFactory.getLogger(Projections.class);

    private static final Strand STRAND = Strand.POSITIVE;
    private static final CoordinateSystem CS = CoordinateSystem.zeroBased();

    private Projections() {
    }

    public static <T extends Located> List<Projection<T>> project(T query, Route route) {
        if (!route.segmentContigs().contains(query.contig()))
            return List.of();

        int startSegmentIdx = -1, endSegmentIdx = -1;

        List<Segment> segments = route.segments();
        for (int i = 0; i < segments.size(); i++) {
            Segment segment = segments.get(i);

            if (segment.contig().equals(query.contig())) {
                // we need to work on 1-based if we investigate start and end as integers separately
                int queryStart = query.startOnStrandWithCoordinateSystem(segment.strand(), CoordinateSystem.oneBased());
                if (segment.contains(queryStart)) {
                    startSegmentIdx = i;
                }
                int queryEnd = query.endOnStrandWithCoordinateSystem(segment.strand(), CoordinateSystem.oneBased());
                if (segment.contains(queryEnd)) {
                    endSegmentIdx = i;
                }
                if (startSegmentIdx >= 0 && endSegmentIdx >= 0)
                    break; // stop the search
            }
        }

        if (startSegmentIdx < 0 && endSegmentIdx < 0) {
            // on Route's contigs, but not in on the Route
            return List.of();
        }
        if (startSegmentIdx < 0 || endSegmentIdx < 0) {
            return List.of();
        }

        if (query.location().length() == 0) {
            if (startSegmentIdx == endSegmentIdx - 1)
                startSegmentIdx++; // TODO - is this dirty hack working?
            else if (startSegmentIdx - 1 == endSegmentIdx)
                endSegmentIdx++;
        }

        if (startSegmentIdx == endSegmentIdx) {
            return processIntraSegmentEvent(query, startSegmentIdx, route);
        } else {
            return processInterSegmentEvent(query, startSegmentIdx, endSegmentIdx, route);
        }
    }

    private static <T extends Located> List<Projection<T>> processIntraSegmentEvent(T query,
                                                                                    int segmentIdx,
                                                                                    Route route) {
        List<Segment> segments = route.segments();
        Segment segment = segments.get(segmentIdx);
        if (segment.event() == Event.DELETION)
            return List.of();

        else if (segment.event() == Event.DUPLICATION) {
            int nBasesInPreviousSegments = countBasesInPreviousSegments(segments, segmentIdx);
            int firstStart = query.startOnStrandWithCoordinateSystem(segment.strand(), segment.coordinateSystem()) - segment.start() + nBasesInPreviousSegments;
            int firstEnd = query.endOnStrandWithCoordinateSystem(segment.strand(), segment.coordinateSystem()) - segment.start() + nBasesInPreviousSegments;

            Projection<T> first = Projection.builder(route, query, route.neoContig(), STRAND, CS)
                    .start(firstStart).setStartEvent(Projection.Location.of(segmentIdx, Event.DUPLICATION))
                    .end(firstEnd).setEndEvent(Projection.Location.of(segmentIdx, Event.DUPLICATION))
                    .build();
            Projection<T> second = Projection.builder(route, query, route.neoContig(), STRAND, CS)
                    .start(firstStart + segment.length()).setStartEvent(Projection.Location.of(segmentIdx, Event.DUPLICATION))
                    .end(firstEnd + segment.length()).setEndEvent(Projection.Location.of(segmentIdx, Event.DUPLICATION))
                    .build();
            return List.of(first, second);

        } else if (segment.event() == Event.INVERSION) {
            int nBasesInPreviousSegments = countBasesInPreviousSegments(segments, segmentIdx);
            int start = query.startOnStrandWithCoordinateSystem(segment.strand(), segment.coordinateSystem()) - segment.start();
            int end = query.endOnStrandWithCoordinateSystem(segment.strand(), segment.coordinateSystem()) - segment.start();
            int invertedEnd = Coordinates.invertCoordinate(CS, route.neoContig(), nBasesInPreviousSegments + start);
            int invertedStart = Coordinates.invertCoordinate(CS, route.neoContig(), nBasesInPreviousSegments + end);

            return List.of(Projection.builder(route, query, route.neoContig(), STRAND.opposite(), CS)
                    .start(invertedStart).setStartEvent(Projection.Location.of(segmentIdx, Event.INVERSION))
                    .end(invertedEnd).setEndEvent(Projection.Location.of(segmentIdx, Event.INVERSION))
                    .build());

        } else if (segment.event() == Event.GAP) {
            int nBasesInPreviousSegments = countBasesInPreviousSegments(segments, segmentIdx);
            int start = query.startOnStrandWithCoordinateSystem(segment.strand(), segment.coordinateSystem()) - segment.start() + nBasesInPreviousSegments;
            int end = query.endOnStrandWithCoordinateSystem(segment.strand(), segment.coordinateSystem()) - segment.start() + nBasesInPreviousSegments;
            return List.of(Projection.builder(route, query, route.neoContig(), STRAND, CS)
                    .start(start).setStartEvent(Projection.Location.of(segmentIdx, Event.GAP))
                    .end(end).setEndEvent(Projection.Location.of(segmentIdx, Event.GAP))
                    .build());
        }

        LogUtils.logWarn(LOGGER, "Unexpected query `{}`", query);
        return List.of();
    }

    private static <T extends Located> List<Projection<T>> processInterSegmentEvent(T query,
                                                                                    int startSegmentIdx,
                                                                                    int endSegmentIdx,
                                                                                    Route route) {
        List<Segment> segments = route.segments();
        Segment startSegment = segments.get(startSegmentIdx);
        Segment endSegment = segments.get(endSegmentIdx);

        List<Segment> spanned = segments.subList(startSegmentIdx + 1, endSegmentIdx);
        List<Projection.Location> spannedLocations = new ArrayList<>(spanned.size());
        for (int i = 0; i < spanned.size(); i++) {
            Segment segment = spanned.get(i);
            spannedLocations.add(Projection.Location.of(startSegmentIdx + 1 + i, segment.event()));
        }
        Event startEvent = startSegment.event();
        Event endEvent = endSegment.event();
        if (startEvent == Event.GAP && endEvent == Event.GAP) {
            int startPrevious = countBasesInPreviousSegments(segments, startSegmentIdx);
            int start = query.startOnStrandWithCoordinateSystem(startSegment.strand(), startSegment.coordinateSystem()) - startSegment.start() + startPrevious;
            int endPrevious = countBasesInPreviousSegments(segments, endSegmentIdx);
            int end = query.endOnStrandWithCoordinateSystem(startSegment.strand(), startSegment.coordinateSystem()) - endSegment.start() + endPrevious;
            return List.of(Projection.builder(route, query, route.neoContig(), STRAND, CS)
                    .start(start).setStartEvent(Projection.Location.of(startSegmentIdx, startEvent))
                    .end(end).setEndEvent(Projection.Location.of(endSegmentIdx, endEvent))
                    .addAllSpannedEvents(spannedLocations)
                    .build());
        }
        if (startEvent == Event.GAP) {
            switch (endEvent) {
                case DELETION:
                case INVERSION:
                    return List.of();
                case DUPLICATION:
                    int startPrevious = countBasesInPreviousSegments(segments, startSegmentIdx);
                    int start = query.startOnStrandWithCoordinateSystem(startSegment.strand(), startSegment.coordinateSystem()) - startSegment.start() + startPrevious;
                    int endPrevious = countBasesInPreviousSegments(segments, endSegmentIdx);
                    int end = query.endOnStrandWithCoordinateSystem(startSegment.strand(), startSegment.coordinateSystem()) - endSegment.start() + endPrevious;
                    return List.of(Projection.builder(route, query, route.neoContig(), STRAND, CS)
                            .start(start).setStartEvent(Projection.Location.of(startSegmentIdx, startEvent))
                            .end(end).setEndEvent(Projection.Location.of(endSegmentIdx, endEvent))
                            .addAllSpannedEvents(spannedLocations)
                            .build());
                case INSERTION:
                    // TODO - fix
                default:
                    LogUtils.logWarn(LOGGER, "Unexpected end event `{}`", endEvent);
                    return List.of();
            }
        }
        if (endEvent == Event.GAP) {
            switch (startEvent) {
                case DELETION:
                case INVERSION:
                    return List.of();
                case DUPLICATION:
                    int startPrevious = countBasesInPreviousSegments(segments, startSegmentIdx);
                    int penultimateBases = startSegment.length() * (startSegment.copies() - 1);
                    int start = query.startOnStrandWithCoordinateSystem(startSegment.strand(), startSegment.coordinateSystem()) - startSegment.start() + startPrevious + penultimateBases;
                    int endPrevious = countBasesInPreviousSegments(segments, endSegmentIdx);
                    int end = query.endOnStrandWithCoordinateSystem(startSegment.strand(), startSegment.coordinateSystem()) - endSegment.start() + endPrevious;
                    return List.of(Projection.builder(route, query, route.neoContig(), STRAND, CS)
                            .start(start).setStartEvent(Projection.Location.of(startSegmentIdx, startEvent))
                            .end(end).setEndEvent(Projection.Location.of(endSegmentIdx, endEvent))
                            .addAllSpannedEvents(spannedLocations)
                            .build());
                case INSERTION:
                    // TODO - fix
                default:
                    LogUtils.logWarn(LOGGER, "Unexpected start event `{}`", startEvent);
                    return List.of();
            }
        }

        LogUtils.logWarn(LOGGER, "Unexpected query `{}`", query);
        return List.of();
    }

    private static int countBasesInPreviousSegments(List<Segment> segments, int segmentIdx) {
        int count = 0;
        for (int i = 0; i < segmentIdx; i++) {
            count += segments.get(i).contributingBases();
        }
        return count;
    }

}
