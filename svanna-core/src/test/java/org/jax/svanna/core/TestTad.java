package org.jax.svanna.core;

import org.jax.svanna.core.landscape.TadBoundary;
import org.monarchinitiative.svart.*;

import java.util.Objects;

public class TestTad extends BaseGenomicRegion<TestTad> implements TadBoundary {

    private final String id;

    public static TestTad of(String id, Contig contig, Strand strand, CoordinateSystem coordinateSystem, int startPosition, int endPosition) {
        return new TestTad(contig, strand, coordinateSystem, Position.of(startPosition), Position.of(endPosition), id);
    }

    protected TestTad(Contig contig, Strand strand, CoordinateSystem coordinateSystem, Position startPosition, Position endPosition, String id) {
        super(contig, strand, coordinateSystem, startPosition, endPosition);
        this.id = id;
    }


    @Override
    public String id() {
        return id;
    }

    @Override
    public float stability() {
        return .5f;
    }

    @Override
    protected TestTad newRegionInstance(Contig contig, Strand strand, CoordinateSystem coordinateSystem, Position startPosition, Position endPosition) {
        return new TestTad(contig, strand, coordinateSystem, startPosition, endPosition, id);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        TestTad testTad = (TestTad) o;
        return Objects.equals(id, testTad.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), id);
    }

    @Override
    public String toString() {
        return "TestTad{" +
                "id='" + id + '\'' +
                '}';
    }
}
