package org.monarchinitiative.svanna.ingest.parse.enhancer.fantom;

import org.monarchinitiative.svanna.db.landscape.BaseEnhancer;
import org.monarchinitiative.svanna.model.landscape.enhancer.EnhancerSource;
import org.monarchinitiative.svanna.model.landscape.enhancer.EnhancerTissueSpecificity;
import org.monarchinitiative.svart.*;

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
        GenomicRegion location = GenomicRegion.of(contig, strand, Coordinates.of(coordinateSystem, start, end));
        return new FEnhancer(location, id, isDevelopmental, tau, specificities, totalReadCounts);
    }

    private FEnhancer(GenomicRegion location,
                      String id,
                      boolean isDevelopmental,
                      double tau,
                      Set<EnhancerTissueSpecificity> specificities,
                      double totalReadCpm) {
        super(location, id, FANTOM_ENHANCER, isDevelopmental, tau, specificities);
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
