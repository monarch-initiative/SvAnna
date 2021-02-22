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
public class FilterAndPrioritize<T extends SvannaVariant> implements VariantAnalysis<T> {

    private final Filter<T> variantFilter;
    private final SvPrioritizer<T, ? extends SvPriority> prioritizer;

    public FilterAndPrioritize(Filter<T> filter, SvPrioritizer<T, ? extends SvPriority> prioritizer) {
        this.variantFilter = filter;
        this.prioritizer = prioritizer;
    }

    @Override
    public Stream<T> analyze(Stream<T> variants) {
        return variants.peek(v -> v.addFilterResult(variantFilter.runFilter(v)))
                .peek(v -> v.setSvPriority(prioritizer.prioritize(v)));
    }
}
