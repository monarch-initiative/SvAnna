package org.jax.svanna.core.priority.additive;

import org.monarchinitiative.svart.*;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;


public class Projection<T extends GenomicRegion> extends BaseGenomicRegion<Projection<T>> {

    private final T source;

    private final Route route;

    private Projection(Contig contig, Strand strand, CoordinateSystem coordinateSystem, Position startPosition, Position endPosition,
                       T source, Route route, Location startLocation, Location endLocation, Set<Location> spannedLocations) {
        super(contig, strand, coordinateSystem, startPosition, endPosition);
        this.source = source;
        this.route = route;
        this.startLocation = startLocation;
        this.endLocation = endLocation;
        this.spannedLocations = spannedLocations;
    }
    private final Location startLocation;
    private final Location endLocation;
    private final Set<Location> spannedLocations;

    private Projection(Builder<T> builder) {
        this(builder.contig, builder.strand, builder.coordinateSystem, builder.start, builder.end,
                Objects.requireNonNull(builder.source),
                Objects.requireNonNull(builder.route),
                Objects.requireNonNull(builder.startLocation),
                Objects.requireNonNull(builder.endLocation),
                Set.copyOf(Objects.requireNonNull(builder.spannedLocations)));
    }

    public T source() {
        return source;
    }

    public Route route() {
        return route;
    }

    public Location startLocation() {
        return startLocation;
    }

    public Event startEvent() {
        return startLocation.event();
    }

    public Location endLocation() {
        return endLocation;
    }

    public Event endEvent() {
        return endLocation.event();
    }

    public Set<Location> spannedLocations() {
        return spannedLocations;
    }

    public Set<Event> spannedEvents() {
        return spannedLocations.stream().map(Location::event).collect(Collectors.toSet());
    }

    public boolean isIntraSegment() {
        return startLocation.segmentIdx == endLocation.segmentIdx;
    }

    public boolean isDeleted() {
        return isIntraSegment() && startEvent() == Event.DELETION && endEvent() == Event.DELETION;
    }

    // TODO - evaluate the usefulness
    public boolean isTruncated() {
        if (isIntraSegment()) {
            return startEvent() == Event.DELETION && endEvent() == Event.DELETION;
        }
        return startEvent() == Event.DELETION || startEvent() == Event.INVERSION || startEvent() == Event.BREAKEND
                || endEvent() == Event.DELETION || endEvent() == Event.INVERSION || endEvent() == Event.BREAKEND;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Projection<?> that = (Projection<?>) o;
        return Objects.equals(source, that.source) && Objects.equals(route, that.route) && Objects.equals(startLocation, that.startLocation) && Objects.equals(endLocation, that.endLocation) && Objects.equals(spannedLocations, that.spannedLocations);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), source, route, startLocation, endLocation, spannedLocations);
    }

    @Override
    public String toString() {
        return "Projection{" +
                "source=" + source +
                ", route=" + route +
                ", startLocation=" + startLocation +
                ", endLocation=" + endLocation +
                ", spannedLocations=" + spannedLocations +
                '}';
    }

    @Override
    protected Projection<T> newRegionInstance(Contig contig, Strand strand, CoordinateSystem coordinateSystem, Position startPosition, Position endPosition) {
        return new Projection<>(contig, strand, coordinateSystem, startPosition, endPosition, source, route, startLocation, endLocation, spannedLocations);
    }

    static <T extends GenomicRegion> Builder<T> builder(Route route, T source, Contig contig, Strand strand, CoordinateSystem coordinateSystem) {
        return new Builder<>(source, route, contig, strand, coordinateSystem);
    }

    static class Builder<T extends GenomicRegion> {

        private final T source;

        private final Route route;

        private final Contig contig;

        private final Strand strand;

        private final CoordinateSystem coordinateSystem;

        private final Set<Location> spannedLocations = new HashSet<>(4);

        private Position start, end;

        private Location startLocation;
        private Location endLocation;

        private Builder(T source, Route route, Contig contig, Strand strand, CoordinateSystem coordinateSystem) {
            this.source = source;
            this.route = route;
            this.contig = contig;
            this.strand = strand;
            this.coordinateSystem = coordinateSystem;
        }

        public Location startEvent() {
            return startLocation;
        }

        public Location endEvent() {
            return endLocation;
        }

        public Builder<T> start(Position start) {
            this.start = start;
            return self();
        }

        public boolean startFound() {
            return start != null;
        }

        public Builder<T> end(Position end) {
            this.end = end;
            return self();
        }

        public boolean endFound() {
            return end != null;
        }

        public Builder<T> setStartEvent(Location startLocation) {
            this.startLocation = startLocation;
            return self();
        }

        public Builder<T> setEndEvent(Location endLocation) {
            this.endLocation = endLocation;
            return self();
        }


        public Builder<T> addAllSpannedEvents(Collection<Location> locations) {
            this.spannedLocations.addAll(locations);
            return self();
        }

        public Builder<T> spannedEvent(Location location) {
            this.spannedLocations.add(location);
            return self();
        }

        public boolean isComplete() {
            return startFound() && endFound();
        }

        public Projection<T> build() {
            return new Projection<>(self());
        }


        protected Builder<T> self() {
            return this;
        }
    }

    public static class Location {
        private final int segmentIdx;
        private final Event event;

        public static Location of(int segmentIdx, Event event) {
            return new Location(segmentIdx, event);
        }

        private Location(int segmentIdx, Event event) {
            this.segmentIdx = segmentIdx;
            this.event = event;
        }

        public int segmentIdx() {
            return segmentIdx;
        }

        public Event event() {
            return event;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Location location = (Location) o;
            return segmentIdx == location.segmentIdx && event == location.event;
        }

        @Override
        public int hashCode() {
            return Objects.hash(segmentIdx, event);
        }

        @Override
        public String toString() {
            return "Location{" +
                    "segmentIdx=" + segmentIdx +
                    ", event=" + event +
                    '}';
        }
    }

}
