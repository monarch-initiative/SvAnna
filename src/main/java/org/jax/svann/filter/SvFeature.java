package org.jax.svann.filter;

import org.jax.svann.reference.GenomicRegion;

/**
 * This interface represents public SV information, such as neutral/pathogenic variants observed in the population.
 */
public interface SvFeature extends GenomicRegion {

    SVFeatureOrigin getOrigin();

}
