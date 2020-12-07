package org.jax.svanna.core.reference;

import org.jax.svanna.core.filter.Filterable;
import org.monarchinitiative.variant.api.CoordinateSystem;
import org.monarchinitiative.variant.api.Variant;

public interface AnnotatedVariant extends Filterable, Variant {

    @Override
    AnnotatedVariant withCoordinateSystem(CoordinateSystem other);

    /**
     * @return minimum number of reads covering the variant call
     */
    int minDepthOfCoverage();

    /**
     * @return variant zygosity
     */
    Zygosity zygosity();

}
