package org.jax.svanna.model.landscape.tad;

import org.monarchinitiative.svart.CoordinateSystem;
import org.monarchinitiative.svart.Coordinates;
import xyz.ielis.silent.genes.model.Located;

public interface TadBoundary extends Located {

    /**
     * @return a unique TAD boundary id.
     */
    String id();

    /**
     * @return value in range [0,1] representing the stability of the boundary across the analyzed tissues/cell lines.
     */
    float stability();

    // TODO - this is actually a bad idea
    @Deprecated
    default Coordinates midpoint() {
        int halfLength = location().length() / 2;
        // we determine the median using 0-based system, but we return the coordinates in TAD's system
        int median = startWithCoordinateSystem(CoordinateSystem.zeroBased()) + halfLength;

        int start = median + CoordinateSystem.zeroBased().startDelta(coordinateSystem());
//        int end = median + CoordinateSystem.zeroBased().endDelta(coordinateSystem()) + 1; // one base downstream
        int end = median + CoordinateSystem.zeroBased().endDelta(coordinateSystem());
        return Coordinates.of(coordinateSystem(), start, location().startConfidenceInterval(), end, location().endConfidenceInterval());
    }

}
