package org.jax.svanna.core.reference;

import org.jax.svanna.core.filter.Filterable;
import org.jax.svanna.core.priority.Prioritized;
import org.monarchinitiative.svart.CoordinateSystem;
import org.monarchinitiative.svart.GenomicVariant;
import org.monarchinitiative.svart.Strand;

/**
 * A unit of work in SvAnna.
 */
public interface SvannaVariant extends GenomicVariant, Filterable, VariantMetadata, Prioritized {

    @Override
    SvannaVariant withCoordinateSystem(CoordinateSystem other);

    @Override
    SvannaVariant withStrand(Strand other);

}
