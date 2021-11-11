package org.jax.svanna.core;

import org.jax.svanna.model.landscape.enhancer.Enhancer;
import org.jax.svanna.model.landscape.enhancer.EnhancerSource;
import org.jax.svanna.model.landscape.enhancer.EnhancerTissueSpecificity;
import org.monarchinitiative.svart.*;

import java.util.Objects;
import java.util.Set;

public class TestEnhancer implements Enhancer {

    private final GenomicRegion location;

    private final String id;

    private final EnhancerSource enhancerSource;

    private final boolean isDevelopmental;

    private final double tau;

    private final Set<EnhancerTissueSpecificity> specificities;

    public static TestEnhancer of(Contig contig, Strand strand, CoordinateSystem coordinateSystem, int start, int end, String id) {
        GenomicRegion location = GenomicRegion.of(contig, strand, Coordinates.of(coordinateSystem, start, end));
        return new TestEnhancer(location, id, EnhancerSource.UNKNOWN, false, 0.5, Set.of());
    }


    protected TestEnhancer(GenomicRegion location,
                           String id,
                           EnhancerSource enhancerSource,
                           boolean isDevelopmental,
                           double tau,
                           Set<EnhancerTissueSpecificity> specificities) {
        this.location = location;
        this.id = id;
        this.enhancerSource = enhancerSource;
        this.isDevelopmental = isDevelopmental;
        this.tau = tau;
        this.specificities = specificities;
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
    public EnhancerSource enhancerSource() {
        return enhancerSource;
    }

    @Override
    public boolean isDevelopmental() {
        return isDevelopmental;
    }

    @Override
    public Set<EnhancerTissueSpecificity> tissueSpecificity() {
        return specificities;
    }

    @Override
    public double tau() {
        return tau;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TestEnhancer that = (TestEnhancer) o;
        return isDevelopmental == that.isDevelopmental && Double.compare(that.tau, tau) == 0 && Objects.equals(location, that.location) && Objects.equals(id, that.id) && enhancerSource == that.enhancerSource && Objects.equals(specificities, that.specificities);
    }

    @Override
    public int hashCode() {
        return Objects.hash(location, id, enhancerSource, isDevelopmental, tau, specificities);
    }

    @Override
    public String toString() {
        return "TestEnhancer{" +
                "location=" + location +
                ", id='" + id + '\'' +
                ", enhancerSource=" + enhancerSource +
                ", isDevelopmental=" + isDevelopmental +
                ", tau=" + tau +
                ", specificities=" + specificities +
                '}';
    }
}
