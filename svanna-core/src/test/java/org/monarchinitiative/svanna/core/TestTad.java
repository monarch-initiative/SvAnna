package org.monarchinitiative.svanna.core;

import org.monarchinitiative.svanna.model.landscape.tad.TadBoundary;
import org.monarchinitiative.svart.*;

import java.util.Objects;

public class TestTad implements TadBoundary {

    private final GenomicRegion location;

    private final String id;

    protected TestTad(GenomicRegion location, String id) {
        this.location = location;
        this.id = id;
    }

    public static TestTad of(String id, Contig contig, Strand strand, CoordinateSystem coordinateSystem, int start, int end) {
        return new TestTad(GenomicRegion.of(contig, strand, Coordinates.of(coordinateSystem, start, end)), id);
    }

    @Override
    public GenomicRegion location() {
        return location;
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TestTad testTad = (TestTad) o;
        return Objects.equals(location, testTad.location) && Objects.equals(id, testTad.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(location, id);
    }

    @Override
    public String toString() {
        return "TestTad{" +
                "location=" + location +
                ", id='" + id + '\'' +
                '}';
    }
}
