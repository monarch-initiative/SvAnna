package org.jax.svanna.io;

import htsjdk.variant.variantcontext.VariantContext;
import org.jax.svanna.core.filter.FilterResult;
import org.jax.svanna.core.filter.FilterType;
import org.jax.svanna.core.priority.SvPriority;
import org.jax.svanna.core.reference.VariantCallAttributes;
import org.jax.svanna.core.reference.Zygosity;
import org.monarchinitiative.svart.*;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

class FullSvannaVariantDefault implements FullSvannaVariant {

    private final GenomicVariant variant;
    private final VariantCallAttributes variantCallAttributes;
    private final Set<FilterType> passedFilterTypes = new HashSet<>();
    private final Set<FilterType> failedFilterTypes = new HashSet<>();
    private final AtomicReference<SvPriority> priority = new AtomicReference<>();
    private final VariantContext variantContext;

    private FullSvannaVariantDefault(GenomicVariant variant,
                                     VariantCallAttributes variantCallAttributes,
                                     Set<FilterType> passedFilterTypes,
                                     Set<FilterType> failedFilterTypes,
                                     SvPriority priority,
                                     VariantContext variantContext) {
        // for creating a novel instance from an existing instance in `newVariantInstance`
        this.variant = Objects.requireNonNull(variant, "Variant must not be null");
        this.variantCallAttributes = Objects.requireNonNull(variantCallAttributes);
        this.passedFilterTypes.addAll(Objects.requireNonNull(passedFilterTypes));
        this.failedFilterTypes.addAll(Objects.requireNonNull(failedFilterTypes));
        this.priority.set(priority);
        this.variantContext = variantContext;
    }

    static FullSvannaVariantDefault of(GenomicVariant variant,
                                       VariantCallAttributes variantCallAttributes,
                                       Set<FilterType> passedFilterTypes,
                                       Set<FilterType> failedFilterTypes,
                                       SvPriority svPriority,
                                       VariantContext variantContext) {
        return new FullSvannaVariantDefault(variant,
                variantCallAttributes,
                passedFilterTypes,
                failedFilterTypes,
                svPriority,
                variantContext);
    }

    @Override
    public GenomicVariant genomicVariant() {
        return variant;
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
    public synchronized boolean failedFilter(FilterType filterType) {
        return failedFilterTypes.contains(filterType) && !passedFilterTypes.contains(filterType);
    }

    @Override
    public synchronized boolean addFilterResult(FilterResult filterResult) {
        if (!filterResult.wasRun())
            return true;
        return filterResult.passed()
                ? passedFilterTypes.add(filterResult.getFilterType())
                : failedFilterTypes.add(filterResult.getFilterType());
    }

    @Override
    public int minDepthOfCoverage() {
        return variantCallAttributes.dp();
    }

    @Override
    public int numberOfRefReads() {
        return variantCallAttributes.refReads();
    }

    @Override
    public int numberOfAltReads() {
        return variantCallAttributes.altReads();
    }

    @Override
    public int copyNumber() {
        return variantCallAttributes.copyNumber();
    }

    @Override
    public Zygosity zygosity() {
        return variantCallAttributes.zygosity();
    }

    @Override
    public synchronized void setSvPriority(SvPriority priority) {
        this.priority.set(priority);
    }

    @Override
    public SvPriority svPriority() {
        return priority.get();
    }

    @Override
    public VariantContext variantContext() {
        return variantContext;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FullSvannaVariantDefault that = (FullSvannaVariantDefault) o;
        return Objects.equals(variant, that.variant) && Objects.equals(variantCallAttributes, that.variantCallAttributes) && Objects.equals(passedFilterTypes, that.passedFilterTypes) && Objects.equals(failedFilterTypes, that.failedFilterTypes) && Objects.equals(priority, that.priority) && Objects.equals(variantContext, that.variantContext);
    }

    @Override
    public int hashCode() {
        return Objects.hash(variant, variantCallAttributes, passedFilterTypes, failedFilterTypes, priority, variantContext);
    }

    @Override
    public String toString() {
        return "FullSvannaVariantDefault{" +
                "variant=" + variant +
                ", variantCallAttributes=" + variantCallAttributes +
                ", passedFilterTypes=" + passedFilterTypes +
                ", failedFilterTypes=" + failedFilterTypes +
                ", priority=" + priority +
                ", variantContext=" + variantContext +
                '}';
    }

}
