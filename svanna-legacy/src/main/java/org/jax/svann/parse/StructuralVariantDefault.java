package org.jax.svann.parse;

import org.jax.svann.filter.FilterResult;
import org.jax.svann.filter.FilterType;
import org.jax.svann.reference.Adjacency;
import org.jax.svann.reference.StructuralVariant;
import org.jax.svann.reference.SvType;
import org.jax.svann.reference.Zygosity;

import java.util.*;

class StructuralVariantDefault extends SequenceRearrangementDefault implements StructuralVariant {

    private final Zygosity zygosity;

    private final Set<FilterType> passedFilterTypes;

    private final Set<FilterType> failedFilterTypes;

    protected StructuralVariantDefault(SvType type,
                                       List<Adjacency> adjacencies,
                                       Zygosity zygosity) {
        super(type, adjacencies);
        passedFilterTypes = new HashSet<>();
        failedFilterTypes = new HashSet<>();
        this.zygosity = Objects.requireNonNull(zygosity);
    }

    /**
     * Create variant with {@link Zygosity#UNKNOWN}.
     */
    static StructuralVariantDefault of(SvType type, Adjacency... adjacencies) {
        return of(type, Arrays.asList(adjacencies));
    }

    static StructuralVariantDefault of(SvType type, List<Adjacency> adjacencies) {
        return of(type, adjacencies, Zygosity.UNKNOWN);
    }

    static StructuralVariantDefault of(SvType type, List<Adjacency> adjacencies, Zygosity zygosity) {
        return new StructuralVariantDefault(type, adjacencies, zygosity);
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

    @Override
    public Zygosity zygosity() {
        return zygosity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        StructuralVariantDefault that = (StructuralVariantDefault) o;
        return zygosity == that.zygosity &&
                Objects.equals(passedFilterTypes, that.passedFilterTypes) &&
                Objects.equals(failedFilterTypes, that.failedFilterTypes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), zygosity, passedFilterTypes, failedFilterTypes);
    }


}
