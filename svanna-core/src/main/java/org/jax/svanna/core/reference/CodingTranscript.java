package org.jax.svanna.core.reference;

import org.monarchinitiative.svart.Contig;
import org.monarchinitiative.svart.CoordinateSystem;
import org.monarchinitiative.svart.Position;
import org.monarchinitiative.svart.Strand;

import java.util.List;

public interface CodingTranscript extends Transcript {

    static CodingTranscript of(Contig contig, Strand strand, CoordinateSystem coordinateSystem,
                               int start, int end,
                               String accessionId, List<Exon> exons,
                               int codingStart, int codingEnd) {
        return CodingTranscriptDefault.of(contig, strand, coordinateSystem, Position.of(start), Position.of(end),
                accessionId, exons, Position.of(codingStart), Position.of(codingEnd));
    }

    static CodingTranscript of(Contig contig, Strand strand, CoordinateSystem coordinateSystem,
                               Position startPosition, Position endPosition,
                               String accessionId, List<Exon> exons,
                               Position codingStartPosition, Position codingEndPosition) {
        return CodingTranscriptDefault.of(contig, strand, coordinateSystem, startPosition, endPosition, accessionId, exons, codingStartPosition, codingEndPosition);
    }

    Position codingStartPosition();

    Position codingEndPosition();

    default int codingStart() {
        return codingStartPosition().pos();
    }

    default int codingStartWithCoordinateSystem(CoordinateSystem target) {
        return codingStart() + coordinateSystem().startDelta(target);
    }

    default int codingEnd() {
        return codingEndPosition().pos();
    }

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
        for (Exon exon : exons()) {
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
            Exon exon = exons().get(i);
            length += exon.endWithCoordinateSystem(CoordinateSystem.zeroBased())
                    - Math.max(exon.startWithCoordinateSystem(CoordinateSystem.zeroBased()), cdsEnd);
            if (exon.contains(cdsEnd))
                break;
        }

        return length;
    }
}
