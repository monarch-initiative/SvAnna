package org.jax.svanna.io;

import htsjdk.variant.variantcontext.VariantContext;
import org.jax.svanna.core.reference.SvannaVariant;

public interface FullSvannaVariant extends SvannaVariant {

    VariantContext variantContext();

}
