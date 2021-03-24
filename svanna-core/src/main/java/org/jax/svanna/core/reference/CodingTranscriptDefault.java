package org.jax.svanna.core.reference;

import org.monarchinitiative.svart.Contig;
import org.monarchinitiative.svart.CoordinateSystem;
import org.monarchinitiative.svart.Position;
import org.monarchinitiative.svart.Strand;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

class CodingTranscriptDefault extends BaseTranscript<CodingTranscriptDefault> implements CodingTranscript {

    private final Position codingStartPosition;
    private final Position codingEndPosition;

    static CodingTranscriptDefault of(Contig contig, Strand strand, CoordinateSystem coordinateSystem,
                                      Position start, Position end,
                                      String accessionId, List<Exon> exons,
                                      Position codingStartPosition, Position codingEndPosition) {
        return new CodingTranscriptDefault(contig, strand, coordinateSystem, start, end, accessionId, exons, codingStartPosition, codingEndPosition);
    }

    private CodingTranscriptDefault(Contig contig, Strand strand, CoordinateSystem coordinateSystem,
                                    Position startPosition, Position endPosition,
                                    String accessionId, List<Exon> exons,
                                    Position codingStartPosition, Position codingEndPosition) {
        super(contig, strand, coordinateSystem, startPosition, endPosition, accessionId, exons);
        this.codingStartPosition = Objects.requireNonNull(codingStartPosition);
        this.codingEndPosition = Objects.requireNonNull(codingEndPosition);
        checkCdsCoordinates();
    }

    private void checkCdsCoordinates() {
        boolean cdsStartIsExonic = false;
        int cdsStart = codingStartPosition.pos() + coordinateSystem().startDelta(CoordinateSystem.zeroBased());
        List<Exon> exons = exons();
        for (Exon exon : exons) {
            if (exon.startWithCoordinateSystem(CoordinateSystem.zeroBased()) <= cdsStart
                    && cdsStart <= exon.endWithCoordinateSystem(CoordinateSystem.zeroBased())) {
                cdsStartIsExonic = true;
                break;
            }
        }
        if (!cdsStartIsExonic)
            throw new IllegalArgumentException("CDS start " + codingStartPosition.pos() + " does not seem to be located in exonic region");

        boolean cdsEndIsExonic = false;
        int cdsEnd = codingEndPosition.pos() + coordinateSystem().endDelta(CoordinateSystem.zeroBased());
        for (int i = exons.size() - 1; i >= 0; i--) {
            Exon exon = exons.get(i);
            if (exon.startWithCoordinateSystem(CoordinateSystem.zeroBased()) <= cdsEnd
                    && cdsEnd <= exon.endWithCoordinateSystem(CoordinateSystem.zeroBased())) {
                cdsEndIsExonic = true;
                break;
            }
        }
        if (!cdsEndIsExonic)
            throw new IllegalArgumentException("CDS end " + codingEndPosition.pos() + " does not seem to be located in exonic region");
    }

    @Override
    public Position codingStartPosition() {
        return codingStartPosition;
    }

    @Override
    public Position codingEndPosition() {
        return codingEndPosition;
    }

    @Override
    public boolean isCoding() {
        return true;
    }

    @Override
    protected CodingTranscriptDefault newRegionInstance(Contig contig, Strand strand, CoordinateSystem coordinateSystem, Position startPosition, Position endPosition) {
        Position cdsStart, cdsEnd;
        List<Exon> exons;
        if (strand() != strand) {
            exons = new ArrayList<>(exons().size());
            for (int i = exons().size() - 1; i >= 0; i--) {
                Exon current = exons().get(i);
                Position eStart = current.startPosition().invert(coordinateSystem, contig());
                Position eEnd = current.endPosition().invert(coordinateSystem, contig());
                exons.add(Exon.of(coordinateSystem, eEnd, eStart)); // intent
            }

            cdsEnd = codingStartPosition.invert(coordinateSystem, contig); // intent
            cdsStart = codingEndPosition.invert(coordinateSystem, contig); // intent
        } else if (coordinateSystem() != coordinateSystem) {
            exons = new ArrayList<>(exons().size());
            for (Exon exon : exons()) {
                exons.add(exon.withCoordinateSystem(coordinateSystem));
            }

            cdsStart = codingStartPosition.shift(coordinateSystem().startDelta(coordinateSystem));
            cdsEnd = codingEndPosition.shift(coordinateSystem().endDelta(coordinateSystem));
        } else {
            exons = exons();

            cdsStart = codingStartPosition;
            cdsEnd = codingEndPosition;
        }

        return new CodingTranscriptDefault(contig, strand, coordinateSystem, startPosition, endPosition, accessionId(), exons, cdsStart, cdsEnd);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        CodingTranscriptDefault that = (CodingTranscriptDefault) o;
        return Objects.equals(codingStartPosition, that.codingStartPosition) && Objects.equals(codingEndPosition, that.codingEndPosition);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), codingStartPosition, codingEndPosition);
    }

    @Override
    public String toString() {
        return "CodingTranscript{" +
                "accessionId=" + accessionId() +
                ", " + contigName() + ':' + start() + '-' + end() + '(' + strand() + ')' +
                ", cds=(" + codingStartPosition + '-' + codingEndPosition + ')' +
                '}';
    }
}
