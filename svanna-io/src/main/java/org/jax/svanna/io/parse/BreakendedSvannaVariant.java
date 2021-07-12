package org.jax.svanna.io.parse;

import htsjdk.variant.variantcontext.VariantContext;
import org.jax.svanna.core.filter.FilterResult;
import org.jax.svanna.core.filter.FilterType;
import org.jax.svanna.core.priority.SvPriority;
import org.jax.svanna.core.reference.VariantCallAttributes;
import org.jax.svanna.core.reference.Zygosity;
import org.jax.svanna.io.FullSvannaVariant;
import org.monarchinitiative.svart.BaseBreakendVariant;
import org.monarchinitiative.svart.Breakend;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public final class BreakendedSvannaVariant extends BaseBreakendVariant<BreakendedSvannaVariant> implements FullSvannaVariant {

    private final VariantCallAttributes variantCallAttributes;
    private final Set<FilterType> passedFilterTypes;
    private final Set<FilterType> failedFilterTypes;
    private final AtomicReference<SvPriority> priority;
    private final VariantContext variantContext;

    private BreakendedSvannaVariant(String eventId,
                                    Breakend left,
                                    Breakend right,
                                    String ref,
                                    String alt,
                                    VariantCallAttributes variantCallAttributes,
                                    Set<FilterType> passedFilterTypes,
                                    Set<FilterType> failedFilterTypes,
                                    AtomicReference<SvPriority> priority,
                                    VariantContext variantContext) {

        super(eventId, left, right, ref, alt);

        this.variantCallAttributes = Objects.requireNonNull(variantCallAttributes);
        this.passedFilterTypes = passedFilterTypes;
        this.failedFilterTypes = failedFilterTypes;
        this.priority = priority;
        this.variantContext = variantContext;
    }

    private BreakendedSvannaVariant(Builder builder) {
        super(builder);
        variantCallAttributes = Objects.requireNonNull(builder.variantCallAttributes);
        passedFilterTypes = builder.passedFilterTypes;
        failedFilterTypes = builder.failedFilterTypes;
        priority = builder.priority;
        variantContext = builder.variantContext;
    }

    public static BreakendedSvannaVariant of(String eventId,
                                             Breakend left,
                                             Breakend right,
                                             String ref,
                                             String alt,
                                             VariantCallAttributes variantCallAttributes,
                                             SvPriority svPriority,
                                             VariantContext variantContext) {
        return new BreakendedSvannaVariant(eventId, left, right, ref, alt, variantCallAttributes, new HashSet<>(), new HashSet<>(), new AtomicReference<>(svPriority), variantContext);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    protected BreakendedSvannaVariant newBreakendVariantInstance(String eventId, Breakend left, Breakend right, String ref, String alt) {
        return new BreakendedSvannaVariant(eventId, left, right, ref, alt, variantCallAttributes, passedFilterTypes, failedFilterTypes, priority, variantContext);
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

    /*
     * **********************   VariantMetadata   **********************
     */
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
    public void setSvPriority(SvPriority priority) {
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
        if (!super.equals(o)) return false;
        BreakendedSvannaVariant that = (BreakendedSvannaVariant) o;
        return Objects.equals(variantCallAttributes, that.variantCallAttributes) && Objects.equals(passedFilterTypes, that.passedFilterTypes) && Objects.equals(failedFilterTypes, that.failedFilterTypes) && Objects.equals(priority.get(), that.priority.get()) && Objects.equals(variantContext, that.variantContext);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), variantCallAttributes, passedFilterTypes, failedFilterTypes, priority, variantContext);
    }

    @Override
    public String toString() {
        return "BreakendedSvannaVariant{" +
                "variantCallAttributes=" + variantCallAttributes +
                ", passedFilterTypes=" + passedFilterTypes +
                ", failedFilterTypes=" + failedFilterTypes +
                ", priority=" + priority.get() +
                ", variantContext=" + variantContext +
                "} " + super.toString();
    }

    public static class Builder extends BaseBreakendVariant.Builder<Builder> {

        private final Set<FilterType> passedFilterTypes = new HashSet<>();
        private final Set<FilterType> failedFilterTypes = new HashSet<>();
        private final AtomicReference<SvPriority> priority = new AtomicReference<>();
        private VariantCallAttributes variantCallAttributes;
        private VariantContext variantContext;

        public Builder variantCallAttributes(VariantCallAttributes variantCallAttributes) {
            this.variantCallAttributes = variantCallAttributes;
            return self();
        }

        public Builder addFilterResult(FilterResult filterResult) {
            if (filterResult.wasRun()) {
                if (filterResult.passed())
                    passedFilterTypes.add(filterResult.getFilterType());
                else
                    failedFilterTypes.add(filterResult.getFilterType());
            }

            return self();
        }

        public Builder priority(SvPriority svPriority) {
            priority.set(svPriority);
            return self();
        }

        public Builder variantContext(VariantContext variantContext) {
            this.variantContext = variantContext;
            return self();
        }

        @Override
        public BreakendedSvannaVariant build() {
            return new BreakendedSvannaVariant(self());
        }

        @Override
        protected Builder self() {
            return this;
        }
    }

}
