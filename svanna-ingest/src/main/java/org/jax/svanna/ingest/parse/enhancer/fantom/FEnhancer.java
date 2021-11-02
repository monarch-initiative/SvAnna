package org.jax.svanna.ingest.parse.enhancer.fantom;

import org.jax.svanna.db.landscape.BaseEnhancer;
import org.jax.svanna.model.landscape.enhancer.EnhancerSource;
import org.jax.svanna.model.landscape.enhancer.EnhancerTissueSpecificity;
import org.monarchinitiative.svart.Contig;
import org.monarchinitiative.svart.CoordinateSystem;
import org.monarchinitiative.svart.Coordinates;
import org.monarchinitiative.svart.Strand;

import java.util.Objects;
import java.util.Set;

class FEnhancer extends BaseEnhancer {

    private static EnhancerSource FANTOM_ENHANCER = EnhancerSource.FANTOM5;

    private final double totalReadCpm;

    static FEnhancer of(Contig contig,
                        Strand strand,
                        CoordinateSystem coordinateSystem,
                        int start,
                        int end,
                        String id,
                        boolean isDevelopmental,
                        double tau,
                        Set<EnhancerTissueSpecificity> specificities,
                        double totalReadCounts) {
        Coordinates coordinates = Coordinates.of(coordinateSystem, start, end);
        return new FEnhancer(contig, strand, coordinates, id, isDevelopmental, tau, specificities, totalReadCounts);
    }

    private FEnhancer(Contig contig,
                      Strand strand,
                      Coordinates coordinates,
                      String id,
                      boolean isDevelopmental,
                      double tau,
                      Set<EnhancerTissueSpecificity> specificities,
                      double totalReadCpm) {
        super(contig, strand, coordinates, id, FANTOM_ENHANCER, isDevelopmental, tau, specificities);
        this.totalReadCpm = totalReadCpm;
    }

    public double totalReadCounts() {
        return totalReadCpm;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        FEnhancer fEnhancer = (FEnhancer) o;
        return totalReadCpm == fEnhancer.totalReadCpm;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), totalReadCpm);
    }

    @Override
    public String toString() {
        return "FEnhancer{" +
                "totalReadCpm=" + totalReadCpm +
                "} " + super.toString();
    }
}
