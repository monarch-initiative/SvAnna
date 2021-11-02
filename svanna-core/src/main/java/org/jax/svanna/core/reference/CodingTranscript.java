package org.jax.svanna.core.reference;

import org.monarchinitiative.svart.Contig;
import org.monarchinitiative.svart.CoordinateSystem;
import org.monarchinitiative.svart.Coordinates;
import org.monarchinitiative.svart.Strand;

import java.util.List;

public interface CodingTranscript extends Transcript {

    static CodingTranscript of(Contig contig, Strand strand, CoordinateSystem coordinateSystem,
                               int start, int end,
                               String accessionId, List<Coordinates> exons,
                               int codingStart, int codingEnd) {
        return of(contig, strand, Coordinates.of(coordinateSystem, start, end),
                accessionId, exons, codingStart, codingEnd);
    }

    static CodingTranscript of(Contig contig, Strand strand, Coordinates coordinates,
                               String accessionId, List<Coordinates> exons,
                               int codingStartPosition, int codingEndPosition) {
        return CodingTranscriptDefault.of(contig, strand, coordinates, accessionId, exons, codingStartPosition, codingEndPosition);
    }


    int codingStart();

    default int codingStartWithCoordinateSystem(CoordinateSystem target) {
        return codingStart() + coordinateSystem().startDelta(target);
    }

    int codingEnd();

    default int codingEndWithCoordinateSystem(CoordinateSystem target) {
        return codingEnd() + coordinateSystem().endDelta(target);
    }

    @Override
    CodingTranscript withCoordinateSystem(CoordinateSystem coordinateSystem);

    @Override
    CodingTranscript withStrand(Strand other);

    default int fivePrimeUtrLength() {
        int cdsStart = codingStartWithCoordinateSystem(CoordinateSystem.zeroBased());
        int length = 0;
        for (Coordinates exon : exons()) {
            int exonStart = exon.startWithCoordinateSystem(CoordinateSystem.zeroBased());
            int exonEnd = exon.endWithCoordinateSystem(CoordinateSystem.zeroBased());
            length += Math.min(exonEnd, cdsStart)
                    - exonStart;
            if (exonStart <= cdsStart && cdsStart < exonEnd)
                break;
        }
        return length;
    }

    default int threePrimeUtrLength() {
        int cdsEnd = codingEndWithCoordinateSystem(CoordinateSystem.zeroBased());
        int length = 0;
        for (int i = exons().size() - 1; i >= 0; i--) {
            Coordinates exon = exons().get(i);
            length += exon.endWithCoordinateSystem(CoordinateSystem.zeroBased())
                    - Math.max(exon.startWithCoordinateSystem(CoordinateSystem.zeroBased()), cdsEnd);
            if (exon.contains(cdsEnd))
                break;
        }

        return length;
    }
}
