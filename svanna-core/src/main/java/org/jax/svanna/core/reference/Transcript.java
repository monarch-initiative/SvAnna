package org.jax.svanna.core.reference;

import org.monarchinitiative.svart.Contig;
import org.monarchinitiative.svart.CoordinateSystem;
import org.monarchinitiative.svart.GenomicRegion;
import org.monarchinitiative.svart.Strand;

import java.util.List;

public interface Transcript extends GenomicRegion {

    static Transcript noncoding(Contig contig, Strand strand, CoordinateSystem coordinateSystem,
                                int start, int end,
                                String accessionId, List<Exon> exons) {
        return NonCodingTranscript.of(contig, strand, coordinateSystem, start, end, accessionId, exons);
    }

    String accessionId();

    boolean isCoding();

    List<Exon> exons();

    @Override
    Transcript withCoordinateSystem(CoordinateSystem coordinateSystem);

    @Override
    Transcript withStrand(Strand other);
}
