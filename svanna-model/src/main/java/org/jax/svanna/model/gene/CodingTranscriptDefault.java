package org.jax.svanna.model.gene;

import org.monarchinitiative.svart.Contig;
import org.monarchinitiative.svart.CoordinateSystem;
import org.monarchinitiative.svart.Coordinates;
import org.monarchinitiative.svart.Strand;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

class CodingTranscriptDefault extends BaseTranscript<CodingTranscriptDefault> implements CodingTranscript {

    private static final NumberFormat NF = NumberFormat.getInstance();

    private final int codingStartPosition;
    private final int codingEndPosition;

    static CodingTranscriptDefault of(Contig contig, Strand strand, Coordinates coordinates,
                                      String accessionId, List<Coordinates> exons,
                                      int codingStartPosition, int codingEndPosition) {
        return new CodingTranscriptDefault(contig, strand, coordinates, accessionId, exons, codingStartPosition, codingEndPosition);
    }

    private CodingTranscriptDefault(Contig contig, Strand strand, Coordinates coordinates,
                                    String accessionId, List<Coordinates> exons,
                                    int codingStartPosition, int codingEndPosition) {
        super(contig, strand, coordinates, accessionId, exons);
        this.codingStartPosition = codingStartPosition;
        this.codingEndPosition = codingEndPosition;
        checkCdsCoordinates();
    }

    private void checkCdsCoordinates() {
        boolean cdsStartIsExonic = false;
        int cdsStart = codingStartPosition + coordinateSystem().startDelta(CoordinateSystem.zeroBased());
        List<Coordinates> exons = exons();
        for (Coordinates exon : exons) {
            if (exon.startWithCoordinateSystem(CoordinateSystem.zeroBased()) <= cdsStart
                    && cdsStart <= exon.endWithCoordinateSystem(CoordinateSystem.zeroBased())) {
                cdsStartIsExonic = true;
                break;
            }
        }
        if (!cdsStartIsExonic)
            throw new IllegalArgumentException("CDS start " + codingStartPosition + " does not seem to be located in exonic region");

        boolean cdsEndIsExonic = false;
        int cdsEnd = codingEndPosition + coordinateSystem().endDelta(CoordinateSystem.zeroBased());
        for (int i = exons.size() - 1; i >= 0; i--) {
            Coordinates exon = exons.get(i);
            if (exon.startWithCoordinateSystem(CoordinateSystem.zeroBased()) <= cdsEnd
                    && cdsEnd <= exon.endWithCoordinateSystem(CoordinateSystem.zeroBased())) {
                cdsEndIsExonic = true;
                break;
            }
        }
        if (!cdsEndIsExonic)
            throw new IllegalArgumentException("CDS end " + codingEndPosition + " does not seem to be located in exonic region");
    }

    @Override
    public int codingStart() {
        return codingStartPosition;
    }

    @Override
    public int codingEnd() {
        return codingEndPosition;
    }

    @Override
    public boolean isCoding() {
        return true;
    }

    @Override
    protected CodingTranscriptDefault newRegionInstance(Contig contig, Strand strand, Coordinates coordinates) {
        int cdsStart, cdsEnd;
        List<Coordinates> exons;
        CoordinateSystem coordinateSystem = coordinates.coordinateSystem();
        if (strand() != strand) {
            // Changing Strand
            exons = new ArrayList<>(exons().size());
            for (int i = exons().size() - 1; i >= 0; i--) {
                Coordinates current = exons().get(i);
                exons.add(current.invert(contig)); // intent
            }

            cdsEnd = Coordinates.invertPosition(coordinateSystem, contig, codingStartPosition); // intent
            cdsStart = Coordinates.invertPosition(coordinateSystem, contig, codingEndPosition); // intent
        } else if (coordinateSystem() != coordinateSystem) {
            // Changing CoordinateSystem
            exons = new ArrayList<>(exons().size());
            for (Coordinates exon : exons()) {
                exons.add(exon.withCoordinateSystem(coordinateSystem));
            }

            cdsStart = codingStartPosition + coordinateSystem().startDelta(coordinateSystem);
            cdsEnd = codingEndPosition + coordinateSystem().endDelta(coordinateSystem);
        } else {
            // No-op - should not really happen as either Strand or CoordinateSystem is being changed
            // when `newRegionInstance` is called.
            exons = exons();

            cdsStart = codingStartPosition;
            cdsEnd = codingEndPosition;
        }

        return new CodingTranscriptDefault(contig, strand, coordinates, accessionId(), exons, cdsStart, cdsEnd);
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
                ", " + contigName() + ':' + NF.format(start()) + '-' + NF.format(end()) + '(' + strand() + ')' +
                ", cds=(" + codingStartPosition + '-' + codingEndPosition + ')' +
                '}';
    }
}
