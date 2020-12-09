package org.jax.svann.genomicreg;

import org.jax.svann.reference.CoordinatePair;
import org.jax.svann.reference.GenomicPosition;

import java.util.Objects;
import java.util.StringJoiner;

public class TestCoordinatePair implements CoordinatePair {
    private final GenomicPosition start, end;

    private TestCoordinatePair(GenomicPosition start, GenomicPosition end) {
        this.start = start;
        this.end = end;
    }

    public static TestCoordinatePair of(GenomicPosition start, GenomicPosition end) {
        return new TestCoordinatePair(start, end);
    }

    @Override
    public GenomicPosition getStart() {
        return start;
    }

    @Override
    public GenomicPosition getEnd() {
        return end;
    }

    public TestCoordinatePair toOppositeStrand() {
        // !switch begin and end!
        return new TestCoordinatePair(end.toOppositeStrand(), start.toOppositeStrand());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TestCoordinatePair that = (TestCoordinatePair) o;
        return Objects.equals(start, that.start) &&
                Objects.equals(end, that.end);
    }

    @Override
    public int hashCode() {
        return Objects.hash(start, end);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", TestCoordinatePair.class.getSimpleName() + "[", "]")
                .add("start=" + start)
                .add("end=" + end)
                .toString();
    }
}
