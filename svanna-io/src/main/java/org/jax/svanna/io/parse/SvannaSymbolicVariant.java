package org.jax.svanna.io.parse;

import org.jax.svanna.core.reference.SvannaVariant;
import org.jax.svanna.core.reference.Zygosity;
import org.monarchinitiative.variant.api.*;
import org.monarchinitiative.variant.api.impl.Seq;

import java.util.Objects;

/**
 * Represents a large (or small) variant with a 'symbolic' alt allele as defined in the VCF specifications.
 */
class SvannaSymbolicVariant extends AbstractSvannaVariant {

    private final Contig contig;
    private final String id;
    private final Strand strand;
    private final CoordinateSystem coordinateSystem;
    private final Position startPosition;
    private final Position endPosition;
    private final String ref, alt;
    private final VariantType variantType;
    private final int changeLength;

    private SvannaSymbolicVariant(Contig contig,
                                  String id,
                                  Strand strand,
                                  CoordinateSystem coordinateSystem,
                                  Position startPosition,
                                  Position endPosition,
                                  String ref,
                                  String alt,
                                  int changeLength,
                                  Zygosity zygosity,
                                  int minDepthOfCoverage) {
        super(zygosity, minDepthOfCoverage);
        this.contig = Objects.requireNonNull(contig);
        this.id = Objects.requireNonNull(id);
        this.strand = Objects.requireNonNull(strand);
        this.coordinateSystem = Objects.requireNonNull(coordinateSystem);
        this.startPosition = Objects.requireNonNull(startPosition);
        this.endPosition = Objects.requireNonNull(endPosition);

        int startZeroBased = normalisedStartPosition(CoordinateSystem.ZERO_BASED).pos();
        if (startZeroBased >= endPosition.pos()) {
            throw new IllegalArgumentException("start " + startZeroBased + " must be upstream of end " + endPosition.pos());
        }
        this.ref = Objects.requireNonNull(ref);
        this.alt = Objects.requireNonNull(alt);
        this.variantType = VariantType.parseType(alt);
        this.changeLength = checkChangeLength(changeLength, startZeroBased, endPosition, variantType);
    }

    static SvannaSymbolicVariant of(Contig contig,
                                    String id,
                                    Strand strand,
                                    CoordinateSystem coordinateSystem,
                                    Position startPosition,
                                    Position endPosition,
                                    String ref,
                                    String alt,
                                    int changeLength,
                                    Zygosity zygosity,
                                    int minDepthOfCoverage) {
        return new SvannaSymbolicVariant(contig,
                id,
                strand,
                coordinateSystem,
                startPosition,
                endPosition,
                ref,
                alt,
                changeLength,
                zygosity,
                minDepthOfCoverage);
    }

    private int checkChangeLength(int changeLength, int startZeroBased, Position endPosition, VariantType variantType) {
        if (variantType.baseType() == VariantType.DEL && (startZeroBased - (endPosition.pos() - 1) != changeLength)) {
            throw new IllegalArgumentException("BAD DEL!");
        } else if (variantType.baseType() == VariantType.INS && (changeLength <= 0)) {
            throw new IllegalArgumentException("BAD INS!");
        } else if (variantType.baseType() == VariantType.DUP && (changeLength <= 0)) {
            throw new IllegalArgumentException("BAD DUP!");
        } else if (variantType.baseType() == VariantType.INV && (changeLength != 0)) {
            throw new IllegalArgumentException("BAD INV!");
        }
        return changeLength;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public Contig contig() {
        return contig;
    }

    @Override
    public Position startPosition() {
        return startPosition;
    }

    @Override
    public Position endPosition() {
        return endPosition;
    }

    @Override
    public Strand strand() {
        return strand;
    }

    @Override
    public SvannaSymbolicVariant withStrand(Strand other) {
        if (strand == other) {
            return this;
        }
        Position start = startPosition.invert(contig, coordinateSystem);
        Position end = endPosition.invert(contig, coordinateSystem);
        return new SvannaSymbolicVariant(contig, id, other, coordinateSystem, end, start, Seq.reverseComplement(ref), alt, changeLength, zygosity, minDepthOfCoverage);
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
    public CoordinateSystem coordinateSystem() {
        return coordinateSystem;
    }

    @Override
    public SvannaVariant withCoordinateSystem(CoordinateSystem other) {
        if (coordinateSystem == other) {
            return this;
        }
        return new SvannaSymbolicVariant(contig, id, strand, other, normalisedStartPosition(other), endPosition, ref, alt, changeLength, zygosity, minDepthOfCoverage);
    }

    @Override
    public int changeLength() {
        return changeLength;
    }

    @Override
    public VariantType variantType() {
        return variantType;
    }

    @Override
    public boolean isSymbolic() {
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        SvannaSymbolicVariant that = (SvannaSymbolicVariant) o;
        return changeLength == that.changeLength && Objects.equals(contig, that.contig) && Objects.equals(id, that.id) && strand == that.strand && coordinateSystem == that.coordinateSystem && Objects.equals(startPosition, that.startPosition) && Objects.equals(endPosition, that.endPosition) && Objects.equals(ref, that.ref) && Objects.equals(alt, that.alt) && variantType == that.variantType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), contig, id, strand, coordinateSystem, startPosition, endPosition, ref, alt, variantType, changeLength);
    }

    @Override
    public String toString() {
        return "SvannaSymbolicVariant{" +
                "contig=" + contig +
                ", id='" + id + '\'' +
                ", strand=" + strand +
                ", coordinateSystem=" + coordinateSystem +
                ", startPosition=" + startPosition +
                ", endPosition=" + endPosition +
                ", ref='" + ref + '\'' +
                ", alt='" + alt + '\'' +
                ", variantType=" + variantType +
                ", changeLength=" + changeLength +
                "} " + super.toString();
    }
}
