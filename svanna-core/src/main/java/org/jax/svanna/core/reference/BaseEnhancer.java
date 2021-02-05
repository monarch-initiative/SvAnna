package org.jax.svanna.core.reference;

import org.monarchinitiative.svart.*;

import java.util.Objects;
import java.util.Set;

public class BaseEnhancer extends BaseGenomicRegion<BaseEnhancer> implements Enhancer {

    private final String id;

    private final boolean isDevelopmental;

    private final double tau;

    private final Set<EnhancerTissueSpecificity> specificities;

    static BaseEnhancer of(Contig contig, Strand strand, CoordinateSystem coordinateSystem, Position startPosition, Position endPosition,
                           String id, boolean isDevelopmental, double tau, Set<EnhancerTissueSpecificity> specificities) {
        return new BaseEnhancer(contig, strand, coordinateSystem, startPosition, endPosition, id, isDevelopmental, tau, Set.copyOf(specificities));
    }

    protected BaseEnhancer(Contig contig, Strand strand, CoordinateSystem coordinateSystem, Position startPosition, Position endPosition,
                           String id, boolean isDevelopmental, double tau, Set<EnhancerTissueSpecificity> specificities) {
        super(contig, strand, coordinateSystem, startPosition, endPosition);
        this.id = Objects.requireNonNull(id);
        this.isDevelopmental = isDevelopmental;
        this.tau = tau;
        this.specificities = Objects.requireNonNull(specificities);
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public boolean isDevelopmental() {
        return isDevelopmental;
    }

    @Override
    public double tau() {
        return tau;
    }

    @Override
    public Set<EnhancerTissueSpecificity> tissueSpecificity() {
        return specificities;
    }

    @Override
    protected BaseEnhancer newRegionInstance(Contig contig, Strand strand, CoordinateSystem coordinateSystem, Position startPosition, Position endPosition) {
        return new BaseEnhancer(contig, strand, coordinateSystem, startPosition, endPosition, id, isDevelopmental, tau, specificities);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        BaseEnhancer that = (BaseEnhancer) o;
        return isDevelopmental == that.isDevelopmental && Double.compare(that.tau, tau) == 0 && Objects.equals(id, that.id) && Objects.equals(specificities, that.specificities);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), id, isDevelopmental, specificities, tau);
    }

    @Override
    public String toString() {
        return "DefaultEnhancer{" +
                "id='" + id + '\'' +
                ", isDevelopmental=" + isDevelopmental +
                ", specificities=" + specificities +
                ", tau=" + tau +
                "} " + super.toString();
    }
}
