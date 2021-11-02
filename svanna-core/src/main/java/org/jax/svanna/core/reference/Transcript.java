package org.jax.svanna.core.reference;

import org.monarchinitiative.svart.*;

import java.util.List;

public interface Transcript extends GenomicRegion {

    static Transcript noncoding(Contig contig, Strand strand, CoordinateSystem coordinateSystem,
                                int start, int end,
                                String accessionId, List<Coordinates> exons) {
        return NonCodingTranscript.of(contig, strand, coordinateSystem, start, end, accessionId, exons);
    }

    String accessionId();

    boolean isCoding();

    List<Coordinates> exons();

    @Override
    Transcript withCoordinateSystem(CoordinateSystem coordinateSystem);

    @Override
    Transcript withStrand(Strand other);
}
