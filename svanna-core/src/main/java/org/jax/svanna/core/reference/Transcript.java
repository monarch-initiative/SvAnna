package org.jax.svanna.core.reference;

import org.monarchinitiative.svart.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Transcript extends BaseGenomicRegion<Transcript> {

    private final String accessionId;
    private final String hgvsSymbol;
    private final boolean isCoding;
    private final GenomicRegion cdsRegion;
    private final List<GenomicRegion> exons;


    private Transcript(Contig contig,
                       Strand strand,
                       CoordinateSystem coordinateSystem,
                       Position start,
                       Position end,

                       String accessionId,
                       String hgvsSymbol,
                       boolean isCoding,
                       GenomicRegion cdsRegion,
                       List<GenomicRegion> exons) {
        super(contig, strand, coordinateSystem, start, end);

        this.accessionId = Objects.requireNonNull(accessionId);
        this.hgvsSymbol = Objects.requireNonNull(hgvsSymbol);
        this.cdsRegion = cdsRegion;
        this.isCoding = isCoding;
        this.exons = Objects.requireNonNull(exons);
    }

    public static Transcript of(Contig contig, Strand strand, CoordinateSystem coordinateSystem,
                                int start, int end,
                                int cdsStart, int cdsEnd,
                                String accessionId,
                                String hgvsSymbol,
                                boolean isCoding,
                                List<GenomicRegion> exons) {
        GenomicRegion cdsRegion = isCoding
                ? GenomicRegion.of(contig, strand, coordinateSystem, Position.of(cdsStart), Position.of(cdsEnd))
                : null;

        return new Transcript(contig, strand, coordinateSystem, Position.of(start), Position.of(end),
                accessionId, hgvsSymbol, isCoding,
                cdsRegion, exons);
    }

    public String accessionId() {
        return accessionId;
    }

    public String hgvsSymbol() {
        return hgvsSymbol;
    }

    public boolean isCoding() {
        return isCoding;
    }

    /**
     * @return genomic region representing the coding region or <code>null</code> if the transcript is non-coding
     */
    public GenomicRegion cdsRegion() {
        return cdsRegion;
    }

    /**
     * @return start position of the coding region or <code>null</code> if the transcript is non-coding
     */
    public Position cdsStartPosition() {
        return isCoding ? cdsRegion.startPosition() : null;
    }

    /**
     * @return end position of the coding region or <code>null</code> if the transcript is non-coding
     */
    public Position cdsEndPosition() {
        return isCoding ? cdsRegion.endPosition() : null;
    }

    public List<GenomicRegion> exons() {
        return exons;
    }

    @Override
    public Transcript withStrand(Strand other) {
        if (strand() == other) {
            return this;
        } else {
            Position start = startPosition().invert(coordinateSystem(), contig());
            Position end = endPosition().invert(coordinateSystem(), contig());

            GenomicRegion cdsRegionWithStrand = isCoding ? cdsRegion.withStrand(other) : null;

            List<GenomicRegion> exonsWithStrand = new ArrayList<>(exons.size());
            for (int i = exons.size() - 1; i >= 0; i--) {
                GenomicRegion exon = exons.get(i);
                exonsWithStrand.add(exon.withStrand(other));
            }

            return new Transcript(contig(), other, coordinateSystem(), end, start, // inverted order!
                    accessionId, hgvsSymbol, isCoding, cdsRegionWithStrand, exonsWithStrand);
        }
    }

    @Override
    public Transcript withCoordinateSystem(CoordinateSystem other) {
        if (coordinateSystem() == other) {
            return this;
        } else {
            GenomicRegion cdsWithCoordinateSystem = isCoding ? cdsRegion.withCoordinateSystem(other) : null;
            List<GenomicRegion> exonsWithCoordinateSystem = new ArrayList<>(exons.size());
            for (GenomicRegion region : exons) {
                GenomicRegion exon = region.withCoordinateSystem(other);
                exonsWithCoordinateSystem.add(exon);
            }

            return new Transcript(contig(), strand(), other, startPositionWithCoordinateSystem(other), endPositionWithCoordinateSystem(other),
                    accessionId, hgvsSymbol, isCoding, cdsWithCoordinateSystem,
                    exonsWithCoordinateSystem);
        }
    }

    @Override
    public Transcript toOppositeStrand() {
        return withStrand(strand().opposite());
    }

    @Override
    protected Transcript newRegionInstance(Contig contig, Strand strand, CoordinateSystem coordinateSystem, Position start, Position end) {
        // no-op Not required as the newVariantInstance returns the same type and this is only required for
        // the BaseGenomicRegion.withCoordinateSystem and withStrand methods which are overridden in this class
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Transcript that = (Transcript) o;
        return isCoding == that.isCoding && Objects.equals(accessionId, that.accessionId) && Objects.equals(hgvsSymbol, that.hgvsSymbol) && Objects.equals(cdsRegion, that.cdsRegion) && Objects.equals(exons, that.exons);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), accessionId, hgvsSymbol, isCoding, cdsRegion, exons);
    }

    @Override
    public String toString() {
        return "Transcript{" +
                "accessionId='" + accessionId + '\'' +
                ", hgvsSymbol='" + hgvsSymbol + '\'' +
                ", isCoding=" + isCoding +
                ", cdsRegion=" + cdsRegion +
                ", exons=" + exons +
                "} " + super.toString();
    }
}
