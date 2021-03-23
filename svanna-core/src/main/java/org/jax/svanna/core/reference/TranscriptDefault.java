package org.jax.svanna.core.reference;

import org.monarchinitiative.svart.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

// TODO - revise coding/noncoding transcript definitions
public class TranscriptDefault extends BaseGenomicRegion<TranscriptDefault> implements Transcript {

    private final String accessionId;
    private final String hgvsSymbol;
    private final GenomicRegion cdsRegion;
    private final List<Exon> exons;


    private TranscriptDefault(Contig contig,
                              Strand strand,
                              CoordinateSystem coordinateSystem,
                              Position start,
                              Position end,

                              String accessionId,
                              String hgvsSymbol,
                              GenomicRegion cdsRegion,
                              List<Exon> exons) {
        super(contig, strand, coordinateSystem, start, end);

        this.accessionId = Objects.requireNonNull(accessionId);
        this.hgvsSymbol = Objects.requireNonNull(hgvsSymbol);
        this.cdsRegion = cdsRegion;
        this.exons = List.copyOf(Objects.requireNonNull(exons));
    }

    public static TranscriptDefault of(Contig contig, Strand strand, CoordinateSystem coordinateSystem,
                                       int start, int end,
                                       String accessionId,
                                       String hgvsSymbol,
                                       boolean isCoding,
                                       int cdsStart, int cdsEnd,
                                       List<Exon> exons) {
        GenomicRegion cdsRegion = isCoding
                ? GenomicRegion.of(contig, strand, coordinateSystem, Position.of(cdsStart), Position.of(cdsEnd))
                : null;

        return new TranscriptDefault(contig, strand, coordinateSystem, Position.of(start), Position.of(end),
                accessionId, hgvsSymbol,
                cdsRegion, exons);
    }

    public static TranscriptDefault nonCoding(Contig contig, Strand strand, CoordinateSystem coordinateSystem,
                                       int start, int end,
                                       String accessionId, String hgvsSymbol,
                                       List<Exon> exons) {
        return new TranscriptDefault(contig, strand, coordinateSystem, Position.of(start), Position.of(end), accessionId, hgvsSymbol, null, exons);
    }

    public static TranscriptDefault coding(Contig contig, Strand strand, CoordinateSystem coordinateSystem,
                                    int start, int end,
                                    int cdsStart, int cdsEnd,
                                    String accessionId, String hgvsSymbol,
                                    List<Exon> exons) {
        GenomicRegion cdsRegion = GenomicRegion.of(contig, strand, coordinateSystem, Position.of(cdsStart), Position.of(cdsEnd));
        return new TranscriptDefault(contig, strand, coordinateSystem, Position.of(start), Position.of(end), accessionId, hgvsSymbol, cdsRegion, exons);
    }

    @Override
    public String accessionId() {
        return accessionId;
    }

    @Override
    public String hgvsSymbol() {
        return hgvsSymbol;
    }

    public Optional<GenomicRegion> cdsRegion() {
        return Optional.ofNullable(cdsRegion);
    }

    @Override
    public List<Exon> exons() {
        return exons;
    }

    @Override
    protected TranscriptDefault newRegionInstance(Contig contig, Strand strand, CoordinateSystem coordinateSystem, Position start, Position end) {
        GenomicRegion cds = cdsRegion;
        if (cds != null)
            cds = cds.withStrand(strand).withCoordinateSystem(coordinateSystem);
        List<Exon> exons;
        if (strand() != strand) {
            exons = new ArrayList<>(exons().size());
            for (int i = exons().size() - 1; i >= 0; i--) {
                Exon current = exons().get(i);
                Position eStart = current.startPosition().invert(coordinateSystem, contig());
                Position eEnd = current.endPosition().invert(coordinateSystem, contig());
                exons.add(Exon.of(coordinateSystem, eEnd, eStart)); // intentional
            }
        } else if (coordinateSystem() != coordinateSystem) {
            exons = new ArrayList<>(exons().size());
            for (Exon exon : exons()) {
                exons.add(exon.withCoordinateSystem(coordinateSystem));
            }
        } else {
            exons = exons();
        }

        return new TranscriptDefault(contig, strand, coordinateSystem, start, end, accessionId, hgvsSymbol, cds, exons);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        TranscriptDefault that = (TranscriptDefault) o;
        return Objects.equals(accessionId, that.accessionId) && Objects.equals(hgvsSymbol, that.hgvsSymbol) && Objects.equals(cdsRegion, that.cdsRegion) && Objects.equals(exons, that.exons);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), accessionId, hgvsSymbol, cdsRegion, exons);
    }

    @Override
    public String toString() {
        return "TranscriptDefault{" +
                "accessionId='" + accessionId + '\'' +
                ", hgvsSymbol='" + hgvsSymbol + '\'' +
                ", cdsRegion=" + cdsRegion +
                ", exons=" + exons +
                '}';
    }
}
