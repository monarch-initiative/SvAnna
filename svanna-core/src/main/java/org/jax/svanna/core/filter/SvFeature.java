package org.jax.svanna.core.filter;

import org.monarchinitiative.svart.GenomicRegion;
import org.monarchinitiative.svart.VariantType;

/**
 * This interface represents public SV information, such as neutral/pathogenic variants observed in the population.
 */
public interface SvFeature extends GenomicRegion {

    VariantType variantType();

    SVFeatureOrigin getOrigin();

    float frequency();
}
