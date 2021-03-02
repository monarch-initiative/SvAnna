package org.jax.svanna.core;

import org.jax.svanna.core.landscape.Enhancer;
import org.jax.svanna.core.landscape.EnhancerSource;
import org.jax.svanna.core.landscape.EnhancerTissueSpecificity;
import org.monarchinitiative.svart.*;

import java.util.Objects;
import java.util.Set;

public class TestEnhancer extends BaseGenomicRegion<TestEnhancer> implements Enhancer {

    private final String id;

    public static TestEnhancer of(String id, Contig contig, Strand strand, CoordinateSystem coordinateSystem, int start, int end) {
        return new TestEnhancer(id, contig, strand, coordinateSystem, Position.of(start), Position.of(end));
    }


    protected TestEnhancer(String id, Contig contig, Strand strand, CoordinateSystem coordinateSystem, Position startPosition, Position endPosition) {
        super(contig, strand, coordinateSystem, startPosition, endPosition);
        this.id = id;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public EnhancerSource enhancerSource() {
        return EnhancerSource.UNKNOWN;
    }

    @Override
    public boolean isDevelopmental() {
        return false;
    }

    @Override
    public Set<EnhancerTissueSpecificity> tissueSpecificity() {
        return Set.of();
    }

    @Override
    public double tau() {
        return 0.5;
    }

    @Override
    protected TestEnhancer newRegionInstance(Contig contig, Strand strand, CoordinateSystem coordinateSystem, Position startPosition, Position endPosition) {
        return new TestEnhancer(id, contig, strand, coordinateSystem, startPosition, endPosition);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        TestEnhancer that = (TestEnhancer) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), id);
    }

    @Override
    public String toString() {
        return "TestEnhancer{" +
                "id='" + id + '\'' +
                '}';
    }
}
