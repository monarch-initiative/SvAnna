package org.jax.svanna.core.priority.additive.simple;

import org.monarchinitiative.svart.GenomicRegion;
import org.monarchinitiative.svart.Variant;

@FunctionalInterface
public interface ImpactCalculator<T extends GenomicRegion> {

    static <T extends GenomicRegion> ImpactCalculator<T> defaultImpactCalculator(double lof, double unchanged) {
        return (v, t) -> t.overlapsWith(v) ? lof : unchanged;
    }

    double calculateImpact(Variant variant, T gene);
}
