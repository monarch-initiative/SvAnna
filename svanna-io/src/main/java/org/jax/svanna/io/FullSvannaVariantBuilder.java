package org.jax.svanna.io;

import htsjdk.variant.variantcontext.VariantContext;
import org.jax.svanna.core.reference.SvannaVariantBuilder;
import org.monarchinitiative.svart.GenomicVariant;

public class FullSvannaVariantBuilder extends SvannaVariantBuilder<FullSvannaVariant, FullSvannaVariantBuilder> {

    private VariantContext variantContext;

    protected FullSvannaVariantBuilder(GenomicVariant variant) {
        super(variant);
    }

    public FullSvannaVariantBuilder variantContext(VariantContext variantContext) {
        this.variantContext = variantContext;
        return self();
    }

    @Override
    public FullSvannaVariantBuilder self() {
        return this;
    }

    @Override
    public FullSvannaVariant build() {
        return FullSvannaVariantDefault.of(variant, variantCallAttributes, passedFilterTypes, failedFilterTypes, null, variantContext);
    }
}
