package org.jax.svanna.core.filter;

import org.jax.svanna.core.reference.SvannaVariant;

/**
 * Dummy filter that actually lets all the variants pass.
 */
public class AllPassFilter implements Filter<SvannaVariant> {

    private static final FilterType FILTER_TYPE = FilterType.FREQUENCY_FILTER;
    private static final FilterResult PASS = FilterResult.pass(FILTER_TYPE);

    @Override
    public FilterType getFilterType() {
        return FILTER_TYPE;
    }

    @Override
    public FilterResult runFilter(SvannaVariant filterable) {
        filterable.addFilterResult(PASS);

        return PASS;
    }
}
