package org.jax.svanna.io.parse;

import htsjdk.variant.variantcontext.VariantContext;
import org.jax.svanna.core.filter.FilterResult;
import org.jax.svanna.core.filter.FilterType;
import org.jax.svanna.core.priority.SvPriority;
import org.jax.svanna.core.reference.VariantCallAttributes;
import org.jax.svanna.core.reference.Zygosity;
import org.jax.svanna.io.FullSvannaVariant;
import org.monarchinitiative.svart.*;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public final class DefaultSvannaVariant extends BaseVariant<DefaultSvannaVariant> implements FullSvannaVariant {

    private final VariantCallAttributes variantCallAttributes;
    private final Set<FilterType> passedFilterTypes;
    private final Set<FilterType> failedFilterTypes;
    private final AtomicReference<SvPriority> priority;
    private final VariantContext variantContext;

    private DefaultSvannaVariant(Contig contig,
                                 String id,
                                 Strand strand,
                                 CoordinateSystem coordinateSystem,
                                 Position startPosition,
                                 Position endPosition,
                                 String ref,
                                 String alt,
                                 int changeLength,
                                 VariantCallAttributes variantCallAttributes,
                                 Set<FilterType> passedFilterTypes,
                                 Set<FilterType> failedFilterTypes,
                                 AtomicReference<SvPriority> priority,
                                 VariantContext variantContext) {
        // for creating a novel instance from an existing instance in `newVariantInstance`
        super(contig, id, strand, coordinateSystem, startPosition, endPosition, ref, alt, changeLength);
        this.variantCallAttributes = Objects.requireNonNull(variantCallAttributes);
        this.passedFilterTypes = passedFilterTypes;
        this.failedFilterTypes = failedFilterTypes;
        this.priority = priority;
        this.variantContext = variantContext;
    }

    private DefaultSvannaVariant(Builder builder) {
        super(builder);
        variantCallAttributes = Objects.requireNonNull(builder.variantCallAttributes, "Variant call attributes cannot be null");
        passedFilterTypes = builder.passedFilterTypes;
        failedFilterTypes = builder.failedFilterTypes;
        priority = builder.priority;
        variantContext = builder.variantContext;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static DefaultSvannaVariant of(Contig contig,
                                          String id,
                                          Strand strand,
                                          CoordinateSystem coordinateSystem,
                                          Position startPosition,
                                          Position endPosition,
                                          String ref,
                                          String alt,
                                          int changeLength,
                                          VariantCallAttributes variantCallAttributes,
                                          SvPriority svPriority,
                                          VariantContext variantContext) {
        return new DefaultSvannaVariant(
                contig, id, strand, coordinateSystem, startPosition, endPosition, ref, alt, changeLength,
                variantCallAttributes, new HashSet<>(), new HashSet<>(), new AtomicReference<>(svPriority), variantContext);
    }

    @Override
    protected DefaultSvannaVariant newVariantInstance(Contig contig,
                                                      String id,
                                                      Strand strand,
                                                      CoordinateSystem coordinateSystem,
                                                      Position startPosition,
                                                      Position endPosition,
                                                      String ref,
                                                      String alt,
                                                      int changeLength) {
        return new DefaultSvannaVariant(contig,
                id,
                strand,
                coordinateSystem,
                startPosition,
                endPosition,
                ref,
                alt,
                changeLength,
                variantCallAttributes,
                passedFilterTypes,
                failedFilterTypes,
                priority,
                variantContext);
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
        if (!super.equals(o)) return false;
        DefaultSvannaVariant that = (DefaultSvannaVariant) o;
        return Objects.equals(variantCallAttributes, that.variantCallAttributes) && Objects.equals(passedFilterTypes, that.passedFilterTypes) && Objects.equals(failedFilterTypes, that.failedFilterTypes) && Objects.equals(priority, that.priority) && Objects.equals(variantContext, that.variantContext);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), variantCallAttributes, passedFilterTypes, failedFilterTypes, priority, variantContext);
    }

    @Override
    public String toString() {
        return "DefaultSvannaVariant{" +
                "variantCallAttributes=" + variantCallAttributes +
                ", passedFilterTypes=" + passedFilterTypes +
                ", failedFilterTypes=" + failedFilterTypes +
                ", priority=" + priority.get() +
                ", variantContext=" + variantContext +
                "} " + super.toString();
    }

    public static class Builder extends BaseVariant.Builder<Builder> {

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
        public DefaultSvannaVariant build() {
            return new DefaultSvannaVariant(selfWithEndIfMissing());
        }

        @Override
        protected Builder self() {
            return this;
        }
    }

}
