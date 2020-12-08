package org.jax.svanna.core.reference;

import org.jax.svanna.core.filter.Filterable;
import org.monarchinitiative.variant.api.CoordinateSystem;
import org.monarchinitiative.variant.api.Strand;
import org.monarchinitiative.variant.api.Variant;

public interface SvannaVariant extends Variant, Filterable, VariantMetadata {

    @Override
    SvannaVariant withCoordinateSystem(CoordinateSystem other);

    @Override
    SvannaVariant withStrand(Strand other);

}
