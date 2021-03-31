package org.jax.svanna.core.analysis;

import org.jax.svanna.core.filter.Filter;
import org.jax.svanna.core.priority.SvPrioritizer;
import org.jax.svanna.core.priority.SvPriority;
import org.jax.svanna.core.reference.SvannaVariant;

import java.util.stream.Stream;

/**
 * Variant analysis consisting of two steps: filtering and prioritization.
 *
 */
public class FilterAndPrioritize implements VariantAnalysis {

    private final Filter<SvannaVariant> variantFilter;
    private final SvPrioritizer<? extends SvPriority> prioritizer;

    public FilterAndPrioritize(Filter<SvannaVariant> filter, SvPrioritizer<? extends SvPriority> prioritizer) {
        this.variantFilter = filter;
        this.prioritizer = prioritizer;
    }

    @Override
    public Stream<SvannaVariant> analyze(Stream<SvannaVariant> variants) {
        return variants.peek(v -> v.addFilterResult(variantFilter.runFilter(v)))
                .peek(v -> v.setSvPriority(prioritizer.prioritize(v)));
    }
}
