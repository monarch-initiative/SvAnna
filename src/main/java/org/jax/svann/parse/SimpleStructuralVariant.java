package org.jax.svann.parse;

import org.jax.svann.filter.FilterResult;
import org.jax.svann.filter.FilterType;
import org.jax.svann.reference.Adjacency;
import org.jax.svann.reference.StructuralVariant;
import org.jax.svann.reference.SvType;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class SimpleStructuralVariant extends SimpleSequenceRearrangement implements StructuralVariant {

    private final Set<FilterType> passedFilterTypes;

    private final Set<FilterType> failedFilterTypes;

    protected SimpleStructuralVariant(SvType type, List<Adjacency> adjacencies) {
        super(type, adjacencies);
        passedFilterTypes = new HashSet<>();
        failedFilterTypes = new HashSet<>();
    }

    static SimpleStructuralVariant of(SvType type, Adjacency... adjacencies) {
        return new SimpleStructuralVariant(type, Arrays.asList(adjacencies));
    }

    static SimpleStructuralVariant of(SvType type, List<Adjacency> adjacencies) {
        return new SimpleStructuralVariant(type, adjacencies);
    }

    @Override
    public synchronized boolean passedFilters() {
        return failedFilterTypes.isEmpty();
    }

    @Override
    public synchronized boolean passedFilter(FilterType filterType) {
        return !failedFilterTypes.contains(filterType) && passedFilterTypes.contains(filterType);
    }

    @Override
    public synchronized boolean addFilterResult(FilterResult filterResult) {
        return filterResult.passed()
                ? passedFilterTypes.add(filterResult.getFilterType())
                : failedFilterTypes.add(filterResult.getFilterType());
    }

}
