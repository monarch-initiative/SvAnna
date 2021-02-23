package org.jax.svanna.core.landscape;

import org.monarchinitiative.svart.CoordinateSystem;
import org.monarchinitiative.svart.GenomicRegion;
import org.monarchinitiative.svart.Position;
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

    default Position asPosition() {
        int halfLength = length() / 2;
        int median = start() + halfLength;
        return Position.of(median, -halfLength, halfLength);
    }

}
