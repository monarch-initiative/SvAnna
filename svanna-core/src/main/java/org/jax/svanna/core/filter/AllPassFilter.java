package org.jax.svanna.core.filter;

import org.jax.svanna.core.reference.AnnotatedVariant;

/**
 * Dummy filter that actually lets all the variants pass.
 */
public class AllPassFilter implements Filter<AnnotatedVariant> {

    private static final FilterType FILTER_TYPE = FilterType.FREQUENCY_FILTER;
    private static final FilterResult PASS = FilterResult.pass(FILTER_TYPE);

    @Override
    public FilterType getFilterType() {
        return FILTER_TYPE;
    }

    @Override
    public FilterResult runFilter(AnnotatedVariant filterable) {
        filterable.addFilterResult(PASS);

        return PASS;
    }
}
