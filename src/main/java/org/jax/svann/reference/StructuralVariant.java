package org.jax.svann.reference;

import org.jax.svann.filter.Filterable;

/**
 * This interface describes domain object for representing genomic variation that we use across the app.
 */
public interface StructuralVariant extends Filterable, SequenceRearrangement {

    /**
     * @return variant zygosity
     */
    Zygosity zygosity();

}
