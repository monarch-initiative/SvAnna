package org.jax.svanna.core.reference;

import org.monarchinitiative.svart.Contig;
import org.monarchinitiative.svart.CoordinateSystem;
import org.monarchinitiative.svart.Coordinates;
import org.monarchinitiative.svart.Strand;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

class NonCodingTranscript extends BaseTranscript<NonCodingTranscript> {

    private static final NumberFormat NF = NumberFormat.getInstance();

    static NonCodingTranscript of(Contig contig, Strand strand, CoordinateSystem coordinateSystem,
                                  int start, int end,
                                  String accessionId,
                                  List<Coordinates> exons) {

        return new NonCodingTranscript(contig, strand, Coordinates.of(coordinateSystem, start, end), accessionId, exons);
    }

    protected NonCodingTranscript(Contig contig,
                                  Strand strand,
                                  Coordinates coordinates,

                                  String accessionId,
                                  List<Coordinates> exons) {
        super(contig, strand, coordinates, accessionId, exons);
    }

    @Override
    public boolean isCoding() {
        return false;
    }

    @Override
    protected NonCodingTranscript newRegionInstance(Contig contig, Strand strand, Coordinates coordinates) {
        List<Coordinates> exons;
        CoordinateSystem coordinateSystem = coordinates.coordinateSystem();
        if (strand() != strand) {
            exons = new ArrayList<>(exons().size());
            for (int i = exons().size() - 1; i >= 0; i--) {
                Coordinates current = exons().get(i);
                exons.add(current.invert(contig));
            }
        } else if (coordinateSystem() != coordinateSystem) {
            exons = new ArrayList<>(exons().size());
            for (Coordinates exon : exons()) {
                exons.add(exon.withCoordinateSystem(coordinateSystem));
            }
        } else {
            exons = exons();
        }

        return new NonCodingTranscript(contig, strand, coordinates, accessionId(), exons);
    }

    @Override
    public String toString() {
        return "NonCodingTranscript{" +
                "accessionId='" + accessionId() + '\'' +
                ", " + contigName() + ':' + NF.format(start()) + '-' + NF.format(end()) + '(' + strand() + ')' +
                '}';
    }
}
