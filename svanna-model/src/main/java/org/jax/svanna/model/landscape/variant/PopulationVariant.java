package org.jax.svanna.model.landscape.variant;

import org.monarchinitiative.svart.VariantType;
import xyz.ielis.silent.genes.model.Located;

public interface PopulationVariant extends Located {

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
