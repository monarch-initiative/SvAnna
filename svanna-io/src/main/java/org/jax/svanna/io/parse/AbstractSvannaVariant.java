package org.jax.svanna.io.parse;

import org.jax.svanna.core.filter.FilterResult;
import org.jax.svanna.core.filter.FilterType;
import org.jax.svanna.core.reference.SvannaVariant;
import org.jax.svanna.core.reference.Zygosity;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

abstract class AbstractSvannaVariant implements SvannaVariant {

    protected final Zygosity zygosity;
    protected final int minDepthOfCoverage;
    private final Set<FilterType> passedFilterTypes;
    private final Set<FilterType> failedFilterTypes;

    protected AbstractSvannaVariant(Zygosity zygosity, int minDepthOfCoverage) {
        this.zygosity = Objects.requireNonNull(zygosity);
        if (minDepthOfCoverage < -1) {
            throw new IllegalArgumentException("Minimum depth of coverage must be greater than `-1`: " + minDepthOfCoverage);
        }
        this.minDepthOfCoverage = minDepthOfCoverage;
        passedFilterTypes = new HashSet<>();
        failedFilterTypes = new HashSet<>();
    }

    /*
     *  **********************  Filterable  **********************
     */
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

    /*
     * **********************   VariantMetadata   **********************
     */
    @Override
    public int minDepthOfCoverage() {
        return minDepthOfCoverage;
    }

    @Override
    public Zygosity zygosity() {
        return zygosity;
    }



    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AbstractSvannaVariant that = (AbstractSvannaVariant) o;
        return minDepthOfCoverage == that.minDepthOfCoverage && zygosity == that.zygosity && Objects.equals(passedFilterTypes, that.passedFilterTypes) && Objects.equals(failedFilterTypes, that.failedFilterTypes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(zygosity, minDepthOfCoverage, passedFilterTypes, failedFilterTypes);
    }

    @Override
    public String toString() {
        return "AbstractSvannaVariant{" +
                "zygosity=" + zygosity +
                ", minDepthOfCoverage=" + minDepthOfCoverage +
                ", passedFilterTypes=" + passedFilterTypes +
                ", failedFilterTypes=" + failedFilterTypes +
                '}';
    }
}
