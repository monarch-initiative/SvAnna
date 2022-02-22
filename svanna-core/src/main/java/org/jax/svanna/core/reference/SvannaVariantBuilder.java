package org.jax.svanna.core.reference;

import org.jax.svanna.core.filter.FilterResult;
import org.jax.svanna.core.filter.FilterType;
import org.monarchinitiative.svart.GenomicVariant;

import java.util.HashSet;
import java.util.Set;

public abstract class SvannaVariantBuilder<
        VARIANT extends SvannaVariant,
        BUILDER extends SvannaVariantBuilder<VARIANT, ?>
        > {

    protected final GenomicVariant variant;
    protected final Set<FilterType> passedFilterTypes = new HashSet<>();
    protected final Set<FilterType> failedFilterTypes = new HashSet<>();
    protected VariantCallAttributes variantCallAttributes;

    protected SvannaVariantBuilder(GenomicVariant variant) {
        this.variant = variant;
    }

    public abstract BUILDER self();

    public abstract VARIANT build();

    public BUILDER variantCallAttributes(VariantCallAttributes variantCallAttributes) {
        this.variantCallAttributes = variantCallAttributes;
        return self();
    }

    public BUILDER addFilterResult(FilterResult filterResult) {
        if (filterResult.wasRun()) {
            FilterType filterType = filterResult.getFilterType();
            if (filterResult.passed()) {
                passedFilterTypes.add(filterType);
            } else if (filterResult.failed()) {
                failedFilterTypes.add(filterType);
            }
        }
        return self();
    }
}
