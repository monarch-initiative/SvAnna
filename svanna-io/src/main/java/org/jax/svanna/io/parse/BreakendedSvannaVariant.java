package org.jax.svanna.io.parse;

import org.jax.svanna.core.filter.FilterResult;
import org.jax.svanna.core.filter.FilterType;
import org.jax.svanna.core.reference.SvannaVariant;
import org.jax.svanna.core.reference.Zygosity;
import org.monarchinitiative.variant.api.*;
import org.monarchinitiative.variant.api.impl.Seq;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

class BreakendedSvannaVariant implements SvannaVariant, Breakended {

    private final String eventId;
    private final Breakend left, right;
    private final String ref, trailingRef, alt;

    protected final Zygosity zygosity;
    protected final int minDepthOfCoverage;
    private final Set<FilterType> passedFilterTypes;
    private final Set<FilterType> failedFilterTypes;

    static BreakendedSvannaVariant of(String eventId,
                                             Breakend left,
                                             Breakend right,
                                             String ref,
                                             String alt,
                                             Zygosity zygosity,
                                             int minDepthOfCoverage) {
        return new BreakendedSvannaVariant(eventId, left, right, ref, alt, zygosity, minDepthOfCoverage);
    }

    private BreakendedSvannaVariant(String eventId,
                                    Breakend left,
                                    Breakend right,
                                    String ref,
                                    String alt,
                                    Zygosity zygosity,
                                    int minDepthOfCoverage) {
        this(eventId, left, right, ref, "", alt, zygosity, minDepthOfCoverage);
    }

    private BreakendedSvannaVariant(String eventId,
                                    Breakend left,
                                    Breakend right,
                                    String ref,
                                    String trailingRef,
                                    String alt,
                                    Zygosity zygosity,
                                    int minDepthOfCoverage) {
        this.eventId = Objects.requireNonNull(eventId);
        this.left = Objects.requireNonNull(left);
        this.right = Objects.requireNonNull(right);
        this.ref = Objects.requireNonNull(ref);
        this.trailingRef = trailingRef;
        this.alt = Objects.requireNonNull(alt);

        this.zygosity = Objects.requireNonNull(zygosity);
        if (minDepthOfCoverage < -1) {
            throw new IllegalArgumentException("Minimum depth of coverage must be greater than `-1`: " + minDepthOfCoverage);
        }
        this.minDepthOfCoverage = minDepthOfCoverage;
        passedFilterTypes = new HashSet<>();
        failedFilterTypes = new HashSet<>();
    }

    /*
     * **********************  Breakended  **********************
     */
    @Override
    public Breakend left() {
        return left;
    }

    @Override
    public Breakend right() {
        return right;
    }

    @Override
    public String eventId() {
        return eventId;
    }

    /*
     * **********************  Variant  **********************
     */
    @Override
    public Contig contig() {
        return left.contig();
    }

    @Override
    public Position startPosition() {
        return left.position();
    }

    @Override
    public Position endPosition() {
        return left.position();
    }

    @Override
    public Strand strand() {
        return left.strand();
    }

    /**
     * This method returns the unchanged breakend variant, since <em>left</em> and <em>right</em> breakend might be
     * located on different strands and it may not be possible to convert the breakend variant to required
     * <code>strand</code>.
     * <p>
     * For instance, the VCF record
     * <pre>2	321681	bnd_W	G	G]17:198982]	6	PASS	SVTYPE=BND;MATEID=bnd_Y;EVENT=tra1</pre>
     * describes a breakend <em>bnd_W</em> that is located at <em>2:321,681</em> on {@link Strand#POSITIVE}, and
     * the mate breakend <em>bnd_Y</em> located at <em>17:83,058,460</em> on {@link Strand#NEGATIVE}.
     * <p>
     * This variant cannot be converted to {@link Strand#NEGATIVE}, the {@link #left()} breakend will always be on
     * {@link Strand#POSITIVE}.
     * <p>
     * Note: use {@link #toOppositeStrand()} method to flip the breakend variant to the opposite strand
     * @param other target strand
     * @return this variant with <em>no change</em>
     */
    @Override
    public BreakendedSvannaVariant withStrand(Strand other) {
        return this;
    }

    @Override
    public BreakendedSvannaVariant toOppositeStrand() {
        return new BreakendedSvannaVariant(
                eventId, right.toOppositeStrand(), left.toOppositeStrand(),
                Seq.reverseComplement(trailingRef), Seq.reverseComplement(ref), Seq.reverseComplement(alt),
                zygosity, minDepthOfCoverage);
    }

    @Override
    public CoordinateSystem coordinateSystem() {
        return CoordinateSystem.ZERO_BASED;
    }

    /**
     * No-op.
     * @param other ignored argument
     * @return this instance
     */
    @Override
    public SvannaVariant withCoordinateSystem(CoordinateSystem other) {
        return this;
    }

    @Override
    public String id() {
        return left.id();
    }

    @Override
    public String ref() {
        return ref;
    }

    @Override
    public String alt() {
        return alt;
    }

    @Override
    public int length() {
        return alt.length();
    }

    @Override
    public int refLength() {
        return ref.length();
    }

    @Override
    public int changeLength() {
        return alt.length();
    }

    @Override
    public VariantType variantType() {
        return VariantType.BND;
    }

    @Override
    public boolean isSymbolic() {
        return true;
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
        BreakendedSvannaVariant that = (BreakendedSvannaVariant) o;
        return minDepthOfCoverage == that.minDepthOfCoverage && Objects.equals(eventId, that.eventId) && Objects.equals(left, that.left) && Objects.equals(right, that.right) && Objects.equals(ref, that.ref) && Objects.equals(trailingRef, that.trailingRef) && Objects.equals(alt, that.alt) && zygosity == that.zygosity && Objects.equals(passedFilterTypes, that.passedFilterTypes) && Objects.equals(failedFilterTypes, that.failedFilterTypes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventId, left, right, ref, trailingRef, alt, zygosity, minDepthOfCoverage, passedFilterTypes, failedFilterTypes);
    }

    @Override
    public String toString() {
        return "BreakendedSvannaVariant{" +
                "eventId='" + eventId + '\'' +
                ", left=" + left +
                ", right=" + right +
                ", ref='" + ref + '\'' +
                ", trailingRef='" + trailingRef + '\'' +
                ", alt='" + alt + '\'' +
                ", zygosity=" + zygosity +
                ", minDepthOfCoverage=" + minDepthOfCoverage +
                ", passedFilterTypes=" + passedFilterTypes +
                ", failedFilterTypes=" + failedFilterTypes +
                '}';
    }
}
