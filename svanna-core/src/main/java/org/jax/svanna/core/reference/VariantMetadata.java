package org.jax.svanna.core.reference;

public interface VariantMetadata {

    /**
     * Depth is set to -1 when the information is not available.
     */
    int MISSING_DEPTH_PLACEHOLDER = -1;

    /**
     * @return minimum number of reads covering the variant call
     */
    int minDepthOfCoverage();

    /**
     * @return variant zygosity
     */
    Zygosity zygosity();

}
