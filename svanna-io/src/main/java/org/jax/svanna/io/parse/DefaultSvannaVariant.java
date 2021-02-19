package org.jax.svanna.io.parse;

import org.jax.svanna.core.filter.FilterResult;
import org.jax.svanna.core.filter.FilterType;
import org.jax.svanna.core.prioritizer.DiscreteSvPriority;
import org.jax.svanna.core.reference.SvannaVariant;
import org.jax.svanna.core.reference.Zygosity;
import org.monarchinitiative.svart.*;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

final class DefaultSvannaVariant extends BaseVariant<DefaultSvannaVariant> implements SvannaVariant {

    private final VariantCallAttributes variantCallAttributes;
    private final Set<FilterType> passedFilterTypes;
    private final Set<FilterType> failedFilterTypes;
    private final AtomicReference<DiscreteSvPriority> priority = new AtomicReference<>();

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
                                 Set<FilterType> failedFilterTypes) {
        // for creating a novel instance from an existing instance in `newVariantInstance`
        super(contig, id, strand, coordinateSystem, startPosition, endPosition, ref, alt, changeLength);
        this.variantCallAttributes = Objects.requireNonNull(variantCallAttributes);
        this.passedFilterTypes = new HashSet<>(passedFilterTypes);
        this.failedFilterTypes = new HashSet<>(failedFilterTypes);
    }

    private DefaultSvannaVariant(Builder builder) {
        super(builder);
        variantCallAttributes = Objects.requireNonNull(builder.variantCallAttributes, "Variant call attributes cannot be null");
        passedFilterTypes = builder.passedFilterTypes;
        failedFilterTypes = builder.failedFilterTypes;
    }

    static DefaultSvannaVariant sequenceVariant(Contig contig, String id,
                                                Strand strand, CoordinateSystem coordinateSystem, int pos, String ref, String alt,
                                                VariantCallAttributes variantCallAttributes) {
        if (VariantType.isSymbolic(alt)) {
            throw new IllegalArgumentException("Unable to create non-symbolic variant from symbolic or breakend allele " + alt);
        }
        Position start = Position.of(pos);
        Position end = calculateEnd(start, coordinateSystem, ref, alt);
        int changeLength = calculateChangeLength(ref, alt);
        return of(contig, id, strand, coordinateSystem, start, end, ref, alt, changeLength, variantCallAttributes);
    }

    static Builder builder() {
        return new Builder();
    }

    static DefaultSvannaVariant of(Contig contig,
                                   String id,
                                   Strand strand,
                                   CoordinateSystem coordinateSystem,
                                   Position startPosition,
                                   Position endPosition,
                                   String ref,
                                   String alt,
                                   int changeLength,
                                   VariantCallAttributes variantCallAttributes) {
        return new DefaultSvannaVariant(
                contig, id, strand, coordinateSystem, startPosition, endPosition, ref, alt, changeLength,
                variantCallAttributes, new HashSet<>(), new HashSet<>());
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
                failedFilterTypes);
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
    public synchronized void setSvPriority(DiscreteSvPriority priority) {
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
        DefaultSvannaVariant that = (DefaultSvannaVariant) o;
        return Objects.equals(variantCallAttributes, that.variantCallAttributes) && Objects.equals(passedFilterTypes, that.passedFilterTypes) && Objects.equals(failedFilterTypes, that.failedFilterTypes) && Objects.equals(priority, that.priority);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), variantCallAttributes, passedFilterTypes, failedFilterTypes, priority);
    }

    @Override
    public String toString() {
        return "DefaultSvannaVariant{" +
                "variantCallAttributes=" + variantCallAttributes +
                ", passedFilterTypes=" + passedFilterTypes +
                ", failedFilterTypes=" + failedFilterTypes +
                ", priority=" + priority.get() +
                "} " + super.toString();
    }

    static class Builder extends BaseVariant.Builder<Builder> {

        private VariantCallAttributes variantCallAttributes;
        private final Set<FilterType> passedFilterTypes = new HashSet<>();
        private final Set<FilterType> failedFilterTypes = new HashSet<>();

        public Builder variantCallAttributes(VariantCallAttributes variantCallAttributes) {
            this.variantCallAttributes = variantCallAttributes;
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
