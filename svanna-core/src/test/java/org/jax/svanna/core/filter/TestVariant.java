package org.jax.svanna.core.filter;

import org.jax.svanna.core.priority.SvPriority;
import org.jax.svanna.core.reference.SvannaVariant;
import org.jax.svanna.core.reference.Zygosity;
import org.monarchinitiative.svart.*;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public class TestVariant extends BaseGenomicVariant<TestVariant> implements SvannaVariant {

    private final Set<FilterType> passedFilterTypes;
    private final Set<FilterType> failedFilterTypes;
    private final AtomicReference<SvPriority> priority = new AtomicReference<>();

    public static TestVariant of(Contig contig, String id, Strand strand, CoordinateSystem coordinateSystem, int start, int end, String ref, String alt, int changeLength) {
        return new TestVariant(contig, id, strand, Coordinates.of(coordinateSystem, start, end), ref, alt, changeLength, new HashSet<>(), new HashSet<>());
    }

    protected TestVariant(Contig contig, String id, Strand strand, Coordinates coordinates, String ref, String alt, int changeLength, Set<FilterType> passedFilterTypes, Set<FilterType> failedFilterTypes) {
        super(contig, id, strand, coordinates, ref, alt, changeLength);
        this.passedFilterTypes = passedFilterTypes;
        this.failedFilterTypes = failedFilterTypes;
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
    public boolean failedFilter(FilterType filterType) {
        return failedFilterTypes.contains(filterType) && !passedFilterTypes.contains(filterType);
    }

    @Override
    public synchronized boolean addFilterResult(FilterResult filterResult) {
        return filterResult.passed()
                ? passedFilterTypes.add(filterResult.getFilterType())
                : failedFilterTypes.add(filterResult.getFilterType());
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
    public Zygosity zygosity() {
        return Zygosity.HETEROZYGOUS;
    }

    @Override
    public int minDepthOfCoverage() {
        return 0;
    }

    @Override
    public int numberOfRefReads() {
        return 0;
    }

    @Override
    public int numberOfAltReads() {
        return 0;
    }

    @Override
    public int copyNumber() {
        return 0;
    }

    @Override
    protected TestVariant newVariantInstance(Contig contig, String id, Strand strand, Coordinates coordinates, String ref, String alt, int changeLength) {
        return new TestVariant(contig, id, strand, coordinates, ref, alt, changeLength, passedFilterTypes, failedFilterTypes);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        TestVariant that = (TestVariant) o;
        return Objects.equals(passedFilterTypes, that.passedFilterTypes) && Objects.equals(failedFilterTypes, that.failedFilterTypes) && Objects.equals(priority, that.priority);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), passedFilterTypes, failedFilterTypes, priority);
    }

    @Override
    public String toString() {
        return "TestVariant{" +
                "passedFilterTypes=" + passedFilterTypes +
                ", failedFilterTypes=" + failedFilterTypes +
                ", priority=" + priority +
                '}';
    }
}
