package org.jax.svanna.core.landscape;

import org.monarchinitiative.svart.CoordinateSystem;
import org.monarchinitiative.svart.Coordinates;
import org.monarchinitiative.svart.GenomicRegion;
import org.monarchinitiative.svart.Strand;

public interface TadBoundary extends GenomicRegion {

    /**
     * @return a unique TAD boundary id.
     */
    String id();

    /**
     * @return value in range [0,1] representing the stability of the boundary across the analyzed tissues/cell lines.
     */
    float stability();

    @Override
    TadBoundary withStrand(Strand other);

    @Override
    TadBoundary withCoordinateSystem(CoordinateSystem coordinateSystem);

    // TODO - this is actually a bad idea
    @Deprecated
    default Coordinates midpoint() {
        int halfLength = length() / 2;
        // we determine the median using 0-based system, but we return the coordinates in TAD's system
        int median = startWithCoordinateSystem(CoordinateSystem.zeroBased()) + halfLength;

        int start = median + CoordinateSystem.zeroBased().startDelta(coordinateSystem());
//        int end = median + CoordinateSystem.zeroBased().endDelta(coordinateSystem()) + 1; // one base downstream
        int end = median + CoordinateSystem.zeroBased().endDelta(coordinateSystem());
        return Coordinates.of(coordinateSystem(), start, startConfidenceInterval(), end, endConfidenceInterval());
    }

}
