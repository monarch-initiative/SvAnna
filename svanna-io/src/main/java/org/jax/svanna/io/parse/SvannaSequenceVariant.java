package org.jax.svanna.io.parse;

import org.jax.svanna.core.reference.SvannaVariant;
import org.jax.svanna.core.reference.Zygosity;
import org.monarchinitiative.variant.api.*;
import org.monarchinitiative.variant.api.impl.Seq;

import java.util.Objects;

/**
 * Represents a simple genomic variation of known sequence. Here simple is defined as not being a symbolic and/or
 * breakend re-arrangement.
 */
class SvannaSequenceVariant extends AbstractSvannaVariant {

    private final Contig contig;
    private final String id;
    private final Strand strand;
    private final CoordinateSystem coordinateSystem;
    private final Position startPosition;
    private final Position endPosition;
    private final String ref;
    private final String alt;

    private static Position calculateEnd(Position start, String ref, String alt) {
        if ((ref.length() | alt.length()) == 1) {
            // SNV case
            return start;
        }
        return start.withPos(start.pos() + ref.length() - 1);
    }

    static SvannaSequenceVariant oneBased(Contig contig,
                                          String id,
                                          int pos,
                                          String ref,
                                          String alt,
                                          Zygosity zygosity,
                                          int minDepthOfCoverage) {
        Position start = Position.of(pos);
        Position end = calculateEnd(start, ref, alt);
        return of(contig, id, Strand.POSITIVE, CoordinateSystem.ONE_BASED, start, end, ref, alt, zygosity, minDepthOfCoverage);
    }

    static SvannaSequenceVariant of(Contig contig,
                                    String id,
                                    Strand strand,
                                    CoordinateSystem coordinateSystem,
                                    Position startPosition,
                                    Position endPosition,
                                    String ref,
                                    String alt,
                                    Zygosity zygosity,
                                    int minDepthOfCoverage) {
        return new SvannaSequenceVariant(contig,
                id,
                strand,
                coordinateSystem,
                startPosition,
                endPosition,
                ref,
                alt,
                zygosity,
                minDepthOfCoverage);
    }

    private SvannaSequenceVariant(Contig contig,
                                  String id,
                                  Strand strand,
                                  CoordinateSystem coordinateSystem,
                                  Position startPosition,
                                  Position endPosition,
                                  String ref,
                                  String alt,
                                  Zygosity zygosity,
                                  int minDepthOfCoverage) {
        super(zygosity, minDepthOfCoverage);
        if (VariantType.isSymbolic(alt)) {
            throw new IllegalArgumentException("Unable to create non-symbolic variant from symbolic or breakend allele " + alt);
        }
        this.contig = Objects.requireNonNull(contig);
        this.id = Objects.requireNonNull(id);
        this.strand = Objects.requireNonNull(strand);
        this.coordinateSystem = Objects.requireNonNull(coordinateSystem);
        this.startPosition = Objects.requireNonNull(startPosition);
        this.endPosition = Objects.requireNonNull(endPosition);
        this.ref = Objects.requireNonNull(ref);
        this.alt = Objects.requireNonNull(alt);
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
    public SvannaSequenceVariant withStrand(Strand other) {
        if (strand == other) {
            return this;
        }
        Position start = startPosition.invert(contig, coordinateSystem);
        Position end = endPosition.invert(contig, coordinateSystem);
        return new SvannaSequenceVariant(contig, id, other, coordinateSystem,
                end, start, Seq.reverseComplement(ref), Seq.reverseComplement(alt),
                zygosity, minDepthOfCoverage);
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
        return new SvannaSequenceVariant(contig, id, strand, coordinateSystem,
                normalisedStartPosition(coordinateSystem), endPosition, ref, alt,
                zygosity, minDepthOfCoverage);
    }

    @Override
    public String id() {
        return id;
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
    public int changeLength() {
        return alt.length() - ref.length();
    }

    @Override
    public boolean isSymbolic() {
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        SvannaSequenceVariant that = (SvannaSequenceVariant) o;
        return Objects.equals(contig, that.contig) && Objects.equals(id, that.id) && strand == that.strand && coordinateSystem == that.coordinateSystem && Objects.equals(startPosition, that.startPosition) && Objects.equals(endPosition, that.endPosition) && Objects.equals(ref, that.ref) && Objects.equals(alt, that.alt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), contig, id, strand, coordinateSystem, startPosition, endPosition, ref, alt);
    }

    @Override
    public String toString() {
        return "SvannaSequenceVariant{" +
                "contig=" + contig +
                ", id='" + id + '\'' +
                ", strand=" + strand +
                ", coordinateSystem=" + coordinateSystem +
                ", startPosition=" + startPosition +
                ", endPosition=" + endPosition +
                ", ref='" + ref + '\'' +
                ", alt='" + alt + '\'' +
                "} " + super.toString();
    }
}
