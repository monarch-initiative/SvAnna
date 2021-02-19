package org.jax.svanna.io.parse;

import org.jax.svanna.core.filter.FilterResult;
import org.jax.svanna.core.filter.FilterType;
import org.jax.svanna.core.prioritizer.DiscreteSvPriority;
import org.jax.svanna.core.reference.SvannaVariant;
import org.jax.svanna.core.reference.Zygosity;
import org.monarchinitiative.svart.BaseBreakendVariant;
import org.monarchinitiative.svart.Breakend;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

final class BreakendedSvannaVariant extends BaseBreakendVariant<BreakendedSvannaVariant> implements SvannaVariant {

    private final VariantCallAttributes variantCallAttributes;
    private final Set<FilterType> passedFilterTypes;
    private final Set<FilterType> failedFilterTypes;
    private final AtomicReference<DiscreteSvPriority> priority = new AtomicReference<>();

    private BreakendedSvannaVariant(String eventId,
                                    Breakend left,
                                    Breakend right,
                                    String ref,
                                    String alt,
                                    VariantCallAttributes variantCallAttributes,
                                    Set<FilterType> passedFilterTypes,
                                    Set<FilterType> failedFilterTypes) {
        super(eventId, left, right, ref, alt);

        this.variantCallAttributes = Objects.requireNonNull(variantCallAttributes);
        this.passedFilterTypes = passedFilterTypes;
        this.failedFilterTypes = failedFilterTypes;
    }

    private BreakendedSvannaVariant(Builder builder) {
        super(builder);
        variantCallAttributes = builder.variantCallAttributes;
        passedFilterTypes = builder.passedFilterTypes;
        failedFilterTypes = builder.failedFilterTypes;
    }

    static BreakendedSvannaVariant of(String eventId,
                                      Breakend left,
                                      Breakend right,
                                      String ref,
                                      String alt,
                                      VariantCallAttributes variantCallAttributes) {
        return new BreakendedSvannaVariant(eventId, left, right, ref, alt, variantCallAttributes, new HashSet<>(), new HashSet<>());
    }

    static Builder builder() {
        return new Builder();
    }

    @Override
    protected BreakendedSvannaVariant newBreakendVariantInstance(String eventId, Breakend left, Breakend right, String ref, String alt) {
        return new BreakendedSvannaVariant(eventId, left, right, ref, alt, variantCallAttributes, passedFilterTypes, failedFilterTypes);
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
    public void setSvPriority(DiscreteSvPriority priority) {
        this.priority.set(priority);
    }

    @Override
    public DiscreteSvPriority svPriority() {
        return priority.get();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        BreakendedSvannaVariant that = (BreakendedSvannaVariant) o;
        return Objects.equals(variantCallAttributes, that.variantCallAttributes) && Objects.equals(passedFilterTypes, that.passedFilterTypes) && Objects.equals(failedFilterTypes, that.failedFilterTypes) && Objects.equals(priority.get(), that.priority.get());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), variantCallAttributes, passedFilterTypes, failedFilterTypes, priority);
    }

    @Override
    public String toString() {
        return "BreakendedSvannaVariant{" +
                "variantCallAttributes=" + variantCallAttributes +
                ", passedFilterTypes=" + passedFilterTypes +
                ", failedFilterTypes=" + failedFilterTypes +
                ", priority=" + priority.get() +
                "} " + super.toString();
    }

    static class Builder extends BaseBreakendVariant.Builder<Builder> {

        private VariantCallAttributes variantCallAttributes;
        private final Set<FilterType> passedFilterTypes = new HashSet<>();
        private final Set<FilterType> failedFilterTypes = new HashSet<>();


        public Builder variantCallAttributes(VariantCallAttributes variantCallAttributes) {
            this.variantCallAttributes = variantCallAttributes;
            return self();
        }

        @Override
        protected BreakendedSvannaVariant build() {
            return new BreakendedSvannaVariant(self());
        }

        @Override
        protected Builder self() {
            return this;
        }
    }

}
