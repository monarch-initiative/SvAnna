package org.jax.svanna.core.reference;

import org.monarchinitiative.svart.Contig;
import org.monarchinitiative.svart.CoordinateSystem;
import org.monarchinitiative.svart.GenomicRegion;
import org.monarchinitiative.svart.Strand;

import java.util.List;
import java.util.Optional;

public interface Transcript extends GenomicRegion {

    static Transcript coding(Contig contig, Strand strand, CoordinateSystem coordinateSystem,
                             int start, int end,
                             int cdsStart, int cdsEnd,
                             String accessionId, String hgvsSymbol,
                             List<Exon> exons) {
        return TranscriptDefault.coding(contig, strand, coordinateSystem, start, end, cdsStart, cdsEnd, accessionId, hgvsSymbol, exons);
    }

    static Transcript nonCoding(Contig contig, Strand strand, CoordinateSystem coordinateSystem,
                                int start, int end,
                                String accessionId, String hgvsSymbol,
                                List<Exon> exons) {
        return TranscriptDefault.nonCoding(contig, strand, coordinateSystem, start, end, accessionId, hgvsSymbol, exons);
    }

    String accessionId();

    // TODO - should be moved to gene?
    String hgvsSymbol();

    Optional<GenomicRegion> cdsRegion();

    default boolean isCoding() {
        return cdsRegion().isPresent();
    }

    List<Exon> exons();

    @Override
    Transcript withCoordinateSystem(CoordinateSystem coordinateSystem);

    @Override
    Transcript withStrand(Strand other);
}
