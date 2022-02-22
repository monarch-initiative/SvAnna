package org.jax.svanna.core.reference;

import org.monarchinitiative.svart.GenomicVariant;

public interface VariantAware {

    GenomicVariant genomicVariant();

    default String id() {
        return genomicVariant().id();
    }
}
