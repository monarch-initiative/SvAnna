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
    private final List<GenomicRegion> exons;


    private TranscriptDefault(Contig contig,
                              Strand strand,
                              CoordinateSystem coordinateSystem,
                              Position start,
                              Position end,

                              String accessionId,
                              String hgvsSymbol,
                              GenomicRegion cdsRegion,
                              List<GenomicRegion> exons) {
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
                                       List<GenomicRegion> exons) {
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
                                       List<GenomicRegion> exons) {
        return new TranscriptDefault(contig, strand, coordinateSystem, Position.of(start), Position.of(end), accessionId, hgvsSymbol, null, exons);
    }

    public static TranscriptDefault coding(Contig contig, Strand strand, CoordinateSystem coordinateSystem,
                                    int start, int end,
                                    int cdsStart, int cdsEnd,
                                    String accessionId, String hgvsSymbol,
                                    List<GenomicRegion> exons) {
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
    public List<GenomicRegion> exons() {
        return exons;
    }

//    @Override
//    public TranscriptDefault withStrand(Strand other) {
//        if (strand() == other) {
//            return this;
//        } else {
//            Position start = startPosition().invert(coordinateSystem(), contig());
//            Position end = endPosition().invert(coordinateSystem(), contig());
//
//            GenomicRegion cdsRegionWithStrand = isCoding ? cdsRegion.withStrand(other) : null;
//
//            List<GenomicRegion> exonsWithStrand = new ArrayList<>(exons.size());
//            for (int i = exons.size() - 1; i >= 0; i--) {
//                GenomicRegion exon = exons.get(i);
//                exonsWithStrand.add(exon.withStrand(other));
//            }
//
//            return new TranscriptDefault(contig(), other, coordinateSystem(), end, start, // inverted order!
//                    accessionId, hgvsSymbol, cdsRegionWithStrand, exonsWithStrand);
//        }
//    }

//    @Override
//    public TranscriptDefault withCoordinateSystem(CoordinateSystem other) {
//        if (coordinateSystem() == other) {
//            return this;
//        } else {
//            GenomicRegion cdsWithCoordinateSystem = isCoding ? cdsRegion.withCoordinateSystem(other) : null;
//            List<GenomicRegion> exonsWithCoordinateSystem = new ArrayList<>(exons.size());
//            for (GenomicRegion region : exons) {
//                GenomicRegion exon = region.withCoordinateSystem(other);
//                exonsWithCoordinateSystem.add(exon);
//            }
//
//            return new TranscriptDefault(contig(), strand(), other, startPositionWithCoordinateSystem(other), endPositionWithCoordinateSystem(other),
//                    accessionId, hgvsSymbol, cdsWithCoordinateSystem,
//                    exonsWithCoordinateSystem);
//        }
//    }
//
//    @Override
//    public TranscriptDefault toOppositeStrand() {
//        return withStrand(strand().opposite());
//    }

    @Override
    protected TranscriptDefault newRegionInstance(Contig contig, Strand strand, CoordinateSystem coordinateSystem, Position start, Position end) {
        // no-op Not required as the newVariantInstance returns the same type and this is only required for
        // the BaseGenomicRegion.withCoordinateSystem and withStrand methods which are overridden in this class

        GenomicRegion cds = cdsRegion;
        if (cds!=null)
            cds = cds.withStrand(strand).withCoordinateSystem(coordinateSystem);
        List<GenomicRegion> exons = new ArrayList<>(exons().size());
        for (GenomicRegion exon : exons()) {
            exons.add(exon.withStrand(strand).withCoordinateSystem(coordinateSystem));
        }

        return new TranscriptDefault(contig, strand, coordinateSystem, start, end, accessionId, hgvsSymbol, cds, exons);
    }

}
