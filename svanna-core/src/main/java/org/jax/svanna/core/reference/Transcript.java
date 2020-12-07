package org.jax.svanna.core.reference;

import org.monarchinitiative.variant.api.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Transcript extends PreciseGenomicRegion {

    private final String accessionId;

    private final String hgvsSymbol;

    private final boolean isCoding;
    private final GenomicPosition cdsStart;
    private final GenomicPosition cdsEnd;
    private final List<GenomicRegion> exons;

    protected Transcript(GenomicRegion txRegion,
                         String accessionId,
                         String hgvsSymbol,
                         boolean isCoding,
                         GenomicPosition cdsStart,
                         GenomicPosition cdsEnd,
                         List<GenomicRegion> exons) {
        super(txRegion);
        this.accessionId = accessionId;
        this.hgvsSymbol = hgvsSymbol;
        this.isCoding = isCoding;
        this.cdsStart = cdsStart;
        this.cdsEnd = cdsEnd;
        this.exons = exons;
    }

    public static Transcript of(Contig contig,
                                int start,
                                int end,
                                Strand strand,
                                CoordinateSystem coordinateSystem,
                                GenomicPosition cdsStart, GenomicPosition cdsEnd, String accessionId,
                                String hgvsSymbol,
                                boolean isCoding,
                                List<GenomicRegion> exons) {
        GenomicRegion txRegion = PreciseGenomicRegion.of(contig, strand, coordinateSystem, Position.of(start), Position.of(end));
        return new Transcript(txRegion, accessionId, hgvsSymbol, isCoding, cdsStart, cdsEnd, exons);
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

    public GenomicPosition cdsStart() {
        return cdsStart;
    }

    public GenomicPosition cdsEnd() {
        return cdsEnd;
    }

    public List<GenomicRegion> exons() {
        return exons;
    }

    @Override
    public Transcript withStrand(Strand other) {
        if (strand == other) {
            return this;
        } else {
            GenomicPosition cdsStartOnPositive = cdsEnd.withStrand(other);
            GenomicPosition cdsEndOnPositive = cdsStart.withStrand(other);
            List<GenomicRegion> exonsOnPositive = new ArrayList<>(exons.size());
            for (int i = exons.size() - 1; i >= 0; i--) {
                GenomicRegion exon = exons.get(i);
                exonsOnPositive.add(exon.withStrand(other));
            }
            return new Transcript(super.withStrand(other), accessionId, hgvsSymbol, isCoding, cdsStartOnPositive, cdsEndOnPositive, exonsOnPositive);
        }
    }

    @Override
    public Transcript toOppositeStrand() {
        return withStrand(strand().opposite());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Transcript that = (Transcript) o;
        return isCoding == that.isCoding &&
                Objects.equals(accessionId, that.accessionId) &&
                Objects.equals(hgvsSymbol, that.hgvsSymbol) &&
                Objects.equals(cdsStart, that.cdsStart) &&
                Objects.equals(cdsEnd, that.cdsEnd) &&
                Objects.equals(exons, that.exons);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), accessionId, hgvsSymbol, isCoding, cdsStart, cdsEnd, exons);
    }

    @Override
    public String toString() {
        return "Transcript{" +
                "accessionId='" + accessionId + '\'' +
                ", hgvsSymbol='" + hgvsSymbol + '\'' +
                ", isCoding=" + isCoding +
                ", cdsStart=" + cdsStart +
                ", cdsEnd=" + cdsEnd +
                ", exons=" + exons +
                "} " + super.toString();
    }
}
