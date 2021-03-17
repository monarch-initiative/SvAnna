package org.jax.svanna.core.filter;

import org.monarchinitiative.svart.CoordinateSystem;
import org.monarchinitiative.svart.GenomicRegion;

class FilterUtils {

    static float reciprocalOverlap(GenomicRegion first, GenomicRegion second) {
        if (!first.overlapsWith(second)) {
            return 0;
        }
        int maxStart = Math.max(first.startWithCoordinateSystem(CoordinateSystem.zeroBased()),
                second.startOnStrandWithCoordinateSystem(first.strand(), CoordinateSystem.zeroBased()));
        int minEnd = Math.min(first.endWithCoordinateSystem(CoordinateSystem.zeroBased()),
                second.endOnStrandWithCoordinateSystem(first.strand(), CoordinateSystem.zeroBased()));

        float intersection = minEnd - maxStart;
        return Math.min(intersection / first.length(), intersection / second.length());
    }


    static float fractionShared(GenomicRegion query, GenomicRegion target) {
        float overlap = target.overlapLength(query);
        return overlap / target.length();
    }

}
