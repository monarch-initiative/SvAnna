package org.jax.svanna.core.landscape;

import org.monarchinitiative.svart.GenomicRegion;
import org.monarchinitiative.svart.Position;

public interface TadBoundary extends GenomicRegion {

    /**
     * @return a unique TAD boundary id.
     */
    String id();

    /**
     * @return value in range [0,1] representing the stability of the boundary across the analyzed tissues/cell lines.
     */
    float stability();

    default Position median() {
        return startPosition().shift(length() / 2);
    }

}
