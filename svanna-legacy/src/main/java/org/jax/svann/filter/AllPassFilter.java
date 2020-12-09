package org.jax.svann.filter;

import org.jax.svann.reference.StructuralVariant;

/**
 * Dummy filter that actually lets all the variants pass.
 */
public class AllPassFilter implements Filter<StructuralVariant> {

    private static final FilterType FILTER_TYPE = FilterType.FREQUENCY_FILTER;
    private static final FilterResult PASS = FilterResult.pass(FILTER_TYPE);

    @Override
    public FilterType getFilterType() {
        return FILTER_TYPE;
    }

    @Override
    public FilterResult runFilter(StructuralVariant filterable) {
        filterable.addFilterResult(PASS);

        return PASS;
    }
}
