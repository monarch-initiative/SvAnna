package org.monarchinitiative.svanna.db.landscape;

import org.monarchinitiative.svanna.model.landscape.enhancer.Enhancer;
import org.monarchinitiative.svanna.model.landscape.enhancer.EnhancerSource;
import org.monarchinitiative.svanna.model.landscape.enhancer.EnhancerTissueSpecificity;
import org.monarchinitiative.svart.*;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class BaseEnhancer implements Enhancer {

    private final GenomicRegion location;

    private final String id;

    private final EnhancerSource enhancerSource;

    private final boolean isDevelopmental;

    private final double tau;

    private final Set<EnhancerTissueSpecificity> specificities;

    public static BaseEnhancer of(Contig contig, Strand strand, CoordinateSystem coordinateSystem, int start, int end,
                                  String id, EnhancerSource enhancerSource, boolean isDevelopmental, double tau, Set<EnhancerTissueSpecificity> specificities) {
        return new BaseEnhancer(
                GenomicRegion.of(contig, strand, Coordinates.of(coordinateSystem, start, end)),
                id, enhancerSource, isDevelopmental, tau, Set.copyOf(specificities));
    }

    protected BaseEnhancer(GenomicRegion location,
                           String id, EnhancerSource enhancerSource, boolean isDevelopmental, double tau, Set<EnhancerTissueSpecificity> specificities) {
        this.location = Objects.requireNonNull(location, "Location must not be null");
        this.id = Objects.requireNonNull(id);
        this.enhancerSource = enhancerSource;
        this.isDevelopmental = isDevelopmental;
        this.tau = tau;
        this.specificities = Objects.requireNonNull(specificities);
    }

    protected BaseEnhancer(Builder builder) {
        this(builder.location, builder.id, builder.enhancerSource, builder.isDevelopmental, builder.tau, builder.specificities);
    }

    public static Builder builder() {
        return new Builder();
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
    public double tau() {
        return tau;
    }

    @Override
    public Set<EnhancerTissueSpecificity> tissueSpecificity() {
        return specificities;
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

    public static class Builder {

        private GenomicRegion location;

        private String id;

        private EnhancerSource enhancerSource = EnhancerSource.UNKNOWN;

        private boolean isDevelopmental = false;

        private double tau = Double.NaN;

        private final Set<EnhancerTissueSpecificity> specificities = new HashSet<>();

        public Builder location(GenomicRegion location) {
            this.location = location;
            return self();
        }

        public Builder id(String id) {
            this.id = id;
            return self();
        }
        public Builder enhancerSource(EnhancerSource enhancerSource) {
            this.enhancerSource = enhancerSource;
            return self();
        }

        public Builder isDevelopmental(boolean isDevelopmental) {
            this.isDevelopmental = isDevelopmental;
            return self();
        }

        public Builder tau(double tau) {
            this.tau = tau;
            return self();
        }

        public Builder addSpecificity(EnhancerTissueSpecificity specificity) {
            this.specificities.add(specificity);
            return self();
        }

        public BaseEnhancer build() {
            return new BaseEnhancer(self());
        }

        protected Builder self() {
            return this;
        }
    }

}
