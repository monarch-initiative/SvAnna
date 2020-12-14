package org.jax.svanna.io.parse;

import org.jax.svanna.core.filter.FilterResult;
import org.jax.svanna.core.filter.FilterType;
import org.jax.svanna.core.reference.SvannaVariant;
import org.jax.svanna.core.reference.Zygosity;
import org.monarchinitiative.variant.api.*;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

class DefaultSvannaVariant extends BaseVariant<DefaultSvannaVariant> implements SvannaVariant {

    protected final Metadata metadata;
    protected final Set<FilterType> passedFilterTypes;
    protected final Set<FilterType> failedFilterTypes;

    protected DefaultSvannaVariant(Contig contig,
                                   String id,
                                   Strand strand,
                                   CoordinateSystem coordinateSystem,
                                   Position startPosition,
                                   Position endPosition,
                                   String ref,
                                   String alt,
                                   int changeLength,
                                   Metadata metadata) {
        // for creating a novel instance via static constructor
        this(contig, id, strand, coordinateSystem, startPosition, endPosition, ref, alt, changeLength,
                metadata, Set.of(), Set.of());
    }

    protected DefaultSvannaVariant(Contig contig,
                                   String id,
                                   Strand strand,
                                   CoordinateSystem coordinateSystem,
                                   Position startPosition,
                                   Position endPosition,
                                   String ref,
                                   String alt,
                                   int changeLength,
                                   Metadata metadata,
                                   Set<FilterType> passedFilterTypes,
                                   Set<FilterType> failedFilterTypes) {
        // for creating a novel instance from an existing instance in `newVariantInstance`
        super(contig, id, strand, coordinateSystem, startPosition, endPosition, ref, alt, changeLength);
        this.metadata = Objects.requireNonNull(metadata);
        this.passedFilterTypes = new HashSet<>(passedFilterTypes);
        this.failedFilterTypes = new HashSet<>(failedFilterTypes);
    }

    static DefaultSvannaVariant oneBasedSequenceVariant(Contig contig, String id, int pos, String ref, String alt,
                                                        Metadata metadata) {
        Position start = Position.of(pos);
        if (VariantType.isSymbolic(alt)) {
            throw new IllegalArgumentException("Unable to create non-symbolic variant from symbolic or breakend allele " + alt);
        }
        Position end = calculateEnd(start, ref, alt);
        int changeLength = alt.length() - ref.length();
        return of(contig, id, Strand.POSITIVE, CoordinateSystem.ONE_BASED, start, end, ref, alt, changeLength, metadata);
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
                                   Metadata metadata) {
        return new DefaultSvannaVariant(
                contig, id, strand, coordinateSystem, startPosition, endPosition, ref, alt, changeLength,
                metadata);
    }

    private static Position calculateEnd(Position start, String ref, String alt) {
        // we assume CoordinateSystem.ONE_BASED
        // SNV case
        if ((ref.length() | alt.length()) == 1) {
            return start;
        }
        return start.withPos(start.pos() + ref.length() - 1);
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
                metadata,
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
        return metadata.dp();
    }

    @Override
    public int numberOfRefReads() {
        return metadata.refReads();
    }

    @Override
    public int numberOfAltReads() {
        return metadata.altReads();
    }

    @Override
    public Zygosity zygosity() {
        return metadata.zygosity();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        DefaultSvannaVariant that = (DefaultSvannaVariant) o;
        return Objects.equals(metadata, that.metadata) && Objects.equals(passedFilterTypes, that.passedFilterTypes) && Objects.equals(failedFilterTypes, that.failedFilterTypes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), metadata, passedFilterTypes, failedFilterTypes);
    }

    @Override
    public String toString() {
        return "DefaultSvannaVariant{" +
                "metadata=" + metadata +
                ", passedFilterTypes=" + passedFilterTypes +
                ", failedFilterTypes=" + failedFilterTypes +
                "} " + super.toString();
    }
}
