package org.jax.svann.reference;

import java.util.Objects;
import java.util.StringJoiner;

/**
 * Package-private implementation of {@link CoordinatePair}.
 */
class SimpleCoordinatePair implements CoordinatePair {

    private final GenomicPosition start, end;

    private SimpleCoordinatePair(GenomicPosition start, GenomicPosition end) {
        this.start = start;
        this.end = end;
    }

    static SimpleCoordinatePair of(GenomicPosition start, GenomicPosition end) {
        return new SimpleCoordinatePair(start, end);
    }

    @Override
    public GenomicPosition getStart() {
        return start;
    }

    @Override
    public GenomicPosition getEnd() {
        return end;
    }

    public SimpleCoordinatePair toOppositeStrand() {
        // !switch begin and end!
        return new SimpleCoordinatePair(end.toOppositeStrand(), start.toOppositeStrand());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SimpleCoordinatePair that = (SimpleCoordinatePair) o;
        return Objects.equals(start, that.start) &&
                Objects.equals(end, that.end);
    }

    @Override
    public int hashCode() {
        return Objects.hash(start, end);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", SimpleCoordinatePair.class.getSimpleName() + "[", "]")
                .add("start=" + start)
                .add("end=" + end)
                .toString();
    }
}
