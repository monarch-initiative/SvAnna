package org.jax.svanna.core.reference;

import org.monarchinitiative.svart.GenomicRegion;
import org.monarchinitiative.svart.VariantType;

public interface PopulationVariant extends GenomicRegion {

    String id();

    VariantType variantType();

    /**
     * @return database where the variant has been reported
     */
    PopulationVariantOrigin origin();

    /**
     * @return frequency as percentage, e.g. 33.33 if the variant is observed in one third of the population
     */
    float alleleFrequency();

}
