package org.jax.svanna.core;

import org.jax.svanna.model.landscape.Enhancer;
import org.jax.svanna.model.landscape.EnhancerSource;
import org.jax.svanna.model.landscape.EnhancerTissueSpecificity;
import org.monarchinitiative.svart.*;

import java.util.Objects;
import java.util.Set;

public class TestEnhancer extends BaseGenomicRegion<TestEnhancer> implements Enhancer {

    private final String id;

    private final EnhancerSource enhancerSource;

    private final boolean isDevelopmental;

    private final double tau;

    private final Set<EnhancerTissueSpecificity> specificities;

    public static TestEnhancer of(String id, Contig contig, Strand strand, CoordinateSystem coordinateSystem, int start, int end) {
        return of(id, contig, strand, Coordinates.of(coordinateSystem, start, end), EnhancerSource.UNKNOWN, false, 0.5, Set.of());
    }

    public static TestEnhancer of(String id, Contig contig, Strand strand, Coordinates coordinates,
                                  EnhancerSource enhancerSource, boolean isDevelopmental, double tau, Set<EnhancerTissueSpecificity> specificities) {
        return new TestEnhancer(id, contig, strand, coordinates,
                enhancerSource, isDevelopmental, tau, Set.copyOf(specificities));
    }

    protected TestEnhancer(String id, Contig contig, Strand strand, Coordinates coordinates,
                           EnhancerSource enhancerSource, boolean isDevelopmental, double tau, Set<EnhancerTissueSpecificity> specificities) {
        super(contig, strand, coordinates);
        this.id = id;
        this.enhancerSource = enhancerSource;
        this.isDevelopmental = isDevelopmental;
        this.tau = tau;
        this.specificities = specificities;
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
    protected TestEnhancer newRegionInstance(Contig contig, Strand strand, Coordinates coordinates) {
        return new TestEnhancer(id, contig, strand, coordinates, enhancerSource, isDevelopmental, tau, specificities);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        TestEnhancer that = (TestEnhancer) o;
        return isDevelopmental == that.isDevelopmental && Double.compare(that.tau, tau) == 0 && Objects.equals(id, that.id) && enhancerSource == that.enhancerSource && Objects.equals(specificities, that.specificities);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), id, enhancerSource, isDevelopmental, tau, specificities);
    }

    @Override
    public String toString() {
        return "TestEnhancer{" +
                "id='" + id + '\'' +
                ", enhancerSource=" + enhancerSource +
                ", isDevelopmental=" + isDevelopmental +
                ", tau=" + tau +
                ", specificities=" + specificities +
                '}';
    }
}
