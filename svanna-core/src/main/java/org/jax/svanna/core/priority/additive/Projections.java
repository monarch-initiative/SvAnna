package org.jax.svanna.core.priority.additive;

import org.jax.svanna.core.exception.LogUtils;
import org.monarchinitiative.svart.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Projections {

    private static final Logger LOGGER = LoggerFactory.getLogger(Projections.class);

    private static final Strand STRAND = Strand.POSITIVE;
    private static final CoordinateSystem CS = CoordinateSystem.zeroBased();

    private Projections() {
    }

    public static <T extends GenomicRegion> List<Projection<T>> projectAll(T query, Route route) {
        if (!route.segmentContigs().contains(query.contig()))
            return List.of();

        int startSegmentIdx = -1, endSegmentIdx = -1;

        List<Segment> segments = route.segments();
        for (int i = 0; i < segments.size(); i++) {
            Segment segment = segments.get(i);

            if (segment.contig().equals(query.contig())) {
                // we need to work on 1-based if we investigate start and end as integers separately
                int queryStart = query.startWithCoordinateSystem(CoordinateSystem.oneBased());
                if (segment.contains(queryStart)) {
                    startSegmentIdx = i;
                }
                int queryEnd = query.endWithCoordinateSystem(CoordinateSystem.oneBased());
                if (segment.contains(queryEnd)) {
                    endSegmentIdx = i;
                }
                if (startSegmentIdx > 0 && endSegmentIdx > 0)
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

        if (startSegmentIdx == endSegmentIdx) {
            return processIntraSegmentEvent(query, startSegmentIdx, route);
        } else {
            return processInterSegmentEvent(query, startSegmentIdx, endSegmentIdx, route);
        }
    }

    private static <T extends GenomicRegion> List<Projection<T>> processIntraSegmentEvent(T query,
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

            Projection<T> first = Projection.builder(route.neoContig(), STRAND, CS, query)
                    .start(Position.of(firstStart)).setStartEvent(Projection.Location.of(segmentIdx, Event.DUPLICATION))
                    .end(Position.of(firstEnd)).setEndEvent(Projection.Location.of(segmentIdx, Event.DUPLICATION))
                    .build();
            Projection<T> second = Projection.builder(route.neoContig(), STRAND, CS, query)
                    .start(Position.of(firstStart + segment.length())).setStartEvent(Projection.Location.of(segmentIdx, Event.DUPLICATION))
                    .end(Position.of(firstEnd + segment.length())).setEndEvent(Projection.Location.of(segmentIdx, Event.DUPLICATION))
                    .build();
            return List.of(first, second);

        } else if (segment.event() == Event.INVERSION) {
            int nBasesInPreviousSegments = countBasesInPreviousSegments(segments, segmentIdx);
            int start = query.startOnStrandWithCoordinateSystem(segment.strand(), segment.coordinateSystem()) - segment.start();
            int end = query.endOnStrandWithCoordinateSystem(segment.strand(), segment.coordinateSystem()) - segment.start();
            int invertedEnd = Coordinates.invertPosition(CS, route.neoContig(), nBasesInPreviousSegments + start);
            int invertedStart = Coordinates.invertPosition(CS, route.neoContig(), nBasesInPreviousSegments + end);

            return List.of(Projection.builder(route.neoContig(), STRAND.opposite(), CS, query)
                    .start(Position.of(invertedStart)).setStartEvent(Projection.Location.of(segmentIdx, Event.INVERSION))
                    .end(Position.of(invertedEnd)).setEndEvent(Projection.Location.of(segmentIdx, Event.INVERSION))
                    .build());

        } else if (segment.event() == Event.GAP) {
            int nBasesInPreviousSegments = countBasesInPreviousSegments(segments, segmentIdx);
            int start = query.startOnStrandWithCoordinateSystem(segment.strand(), segment.coordinateSystem()) - segment.start() + nBasesInPreviousSegments;
            int end = query.endOnStrandWithCoordinateSystem(segment.strand(), segment.coordinateSystem()) - segment.start() + nBasesInPreviousSegments;
            return List.of(Projection.builder(route.neoContig(), STRAND, CS, query)
                    .start(Position.of(start)).setStartEvent(Projection.Location.of(segmentIdx, Event.GAP))
                    .end(Position.of(end)).setEndEvent(Projection.Location.of(segmentIdx, Event.GAP))
                    .build());
        }

        LogUtils.logWarn(LOGGER, "Unexpected query `{}`", query);
        return List.of();
    }

    private static <T extends GenomicRegion> List<Projection<T>> processInterSegmentEvent(T query,
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
            return List.of(Projection.builder(route.neoContig(), STRAND, CS, query)
                    .start(Position.of(start)).setStartEvent(Projection.Location.of(startSegmentIdx, startEvent))
                    .end(Position.of(end)).setEndEvent(Projection.Location.of(endSegmentIdx, endEvent))
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
                    return List.of(Projection.builder(route.neoContig(), STRAND, CS, query)
                            .start(Position.of(start)).setStartEvent(Projection.Location.of(startSegmentIdx, startEvent))
                            .end(Position.of(end)).setEndEvent(Projection.Location.of(endSegmentIdx, endEvent))
                            .addAllSpannedEvents(spannedLocations)
                            .build());
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
                    return List.of(Projection.builder(route.neoContig(), STRAND, CS, query)
                            .start(Position.of(start)).setStartEvent(Projection.Location.of(startSegmentIdx, startEvent))
                            .end(Position.of(end)).setEndEvent(Projection.Location.of(endSegmentIdx, endEvent))
                            .addAllSpannedEvents(spannedLocations)
                            .build());
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


    @Deprecated
    public static <T extends GenomicRegion> Optional<Projection<T>> project(T query, Route route) {
        if (!route.segmentContigs().contains(query.contig()))
            return Optional.empty();

        Projection.Builder<T> builder = Projection.builder(route.neoContig(), STRAND, CS, query);

        int basesInPassedSegments = 0;
        int segmentIdx = 0;
        Strand strand = null;
        Segment previousSegment = null;
        boolean startFoundInTheCurrentSegment = false;
        for (Segment segment : route.segments()) {
            if (previousSegment == null) {
                previousSegment = segment;
                strand = segment.strand();
            } else if (!previousSegment.contig().equals(segment.contig())) {
                strand = segment.strand();
            }

            if (segment.contig().equals(query.contig())) {
                if (!builder.startFound()) {
                    int start = query.strand().equals(segment.strand())
                            ? query.startWithCoordinateSystem(segment.coordinateSystem())
                            : Coordinates.invertPosition(segment.coordinateSystem(), query.contig(), query.start());
                    if (segment.contains(start)) {
                        Position startPosition;
                        Event startEvent;
                        if (segmentIsDeletion(segment)) {
                            startPosition = Position.of(basesInPassedSegments);
                            startEvent = Event.DELETION;
                        } else if (segmentIsDuplication(segment)) {
                            int intraSegmentBases = start - segment.start();
                            startPosition = Position.of(basesInPassedSegments + intraSegmentBases);
                            startEvent = Event.DUPLICATION;
                        } else if (segmentIsInversion(segment, strand)) {
                            int intraSegmentBases = segment.length() - (start - segment.start());
                            startPosition = Position.of(basesInPassedSegments + intraSegmentBases);
                            startEvent = Event.INVERSION;
                        } else {
                            int intraSegmentBases = start - segment.start();
                            startPosition = Position.of(basesInPassedSegments + intraSegmentBases);
                            startEvent = Event.GAP;
                        }
                        builder.start(startPosition)
                                .setStartEvent(Projection.Location.of(segmentIdx, startEvent));
                        startFoundInTheCurrentSegment = true;
                    }
                }

                if (!builder.endFound()) {
                    int end = query.strand().equals(segment.strand())
                            ? query.endWithCoordinateSystem(segment.coordinateSystem())
                            : Coordinates.invertPosition(segment.coordinateSystem(), query.contig(), query.end());
                    if (segment.contains(end)) {
                        Position endPosition;
                        Event endEvent;
                        if (segmentIsDeletion(segment)) {
                            endPosition = Position.of(basesInPassedSegments);
                            endEvent = Event.DELETION;
                        } else if (segmentIsDuplication(segment)) {
                            int intraSegmentBases = end - segment.start();
                            endPosition = Position.of(basesInPassedSegments + intraSegmentBases);
                            endEvent = Event.DUPLICATION;
                        } else if (segmentIsInversion(segment, strand)) {
                            int intraSegmentBases = segment.length() - (end - segment.start());
                            endPosition = Position.of(basesInPassedSegments + intraSegmentBases);
                            endEvent = Event.INVERSION;
                        } else {
                            int intraSegmentBases = end - segment.start();
                            endPosition = Position.of(basesInPassedSegments + intraSegmentBases);
                            endEvent = Event.GAP;
                        }
                        builder.end(endPosition)
                                .setEndEvent(Projection.Location.of(segmentIdx, endEvent));
                    }
                }

                if (builder.startFound() && !builder.endFound() && !startFoundInTheCurrentSegment) {
                    if (segmentIsDeletion(segment))
                        builder.spannedEvent(Projection.Location.of(segmentIdx, Event.DELETION));
                    else if (segmentIsInversion(segment, strand))
                        builder.spannedEvent(Projection.Location.of(segmentIdx, Event.INVERSION));
                    else if (segmentIsDuplication(segment))
                        builder.spannedEvent(Projection.Location.of(segmentIdx, Event.DUPLICATION));
                    else if (segmentIsInsertion(segment))
                        builder.spannedEvent(Projection.Location.of(segmentIdx, Event.INSERTION));
                }
                startFoundInTheCurrentSegment = false;

                if (builder.isComplete()) {
                    return Optional.of(builder.build());
                }
            }

            basesInPassedSegments += segment.length() * segment.copies();
            segmentIdx++;
        }


        if (builder.startFound() && !builder.endFound()) {
            // salvage the end that was likely truncated
            Projection.Location startLocation = builder.startEvent();
            int bases = route.segments().subList(0, startLocation.segmentIdx() + 1).stream()
                    .mapToInt(s -> s.length() * s.copies())
                    .sum();

            builder.end(Position.of(bases))
                    .setEndEvent(Projection.Location.of(startLocation.segmentIdx(), Event.BREAKEND));
        } else if (!builder.startFound() && builder.endFound()) {
            // salvage the start that was likely truncated
            Projection.Location endLocation = builder.endEvent();
            int bases = route.segments().subList(0, endLocation.segmentIdx()).stream()
                    .mapToInt(segment -> segment.length() * segment.copies())
                    .sum();

            builder.start(Position.of(bases))
                    .setStartEvent(Projection.Location.of(endLocation.segmentIdx(), Event.BREAKEND));
        } else {
            return Optional.empty();
        }
        return Optional.of(builder.build());
    }


    private static boolean segmentIsInsertion(Segment segment) {
        return segment.endWithCoordinateSystem(CS) - segment.startWithCoordinateSystem(CS) == 0 && segment.length() > 0;
    }

    private static boolean segmentIsDuplication(Segment segment) {
        return segment.copies() == 2;
    }

    private static boolean segmentIsDeletion(Segment segment) {
        return segment.copies() == 0;
    }

    private static boolean segmentIsInversion(Segment segment, Strand strand) {
        return strand != segment.strand();
    }

}
