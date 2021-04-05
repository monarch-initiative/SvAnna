package org.jax.svanna.core.reference;

import org.monarchinitiative.svart.Contig;
import org.monarchinitiative.svart.CoordinateSystem;
import org.monarchinitiative.svart.Position;
import org.monarchinitiative.svart.Strand;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

class NonCodingTranscript extends BaseTranscript<NonCodingTranscript> {

    private static final NumberFormat NF = NumberFormat.getInstance();

    static NonCodingTranscript of(Contig contig, Strand strand, CoordinateSystem coordinateSystem,
                                  int start, int end,
                                  String accessionId,
                                  List<Exon> exons) {

        return new NonCodingTranscript(contig, strand, coordinateSystem, Position.of(start), Position.of(end),
                accessionId, exons);
    }

    protected NonCodingTranscript(Contig contig,
                                  Strand strand,
                                  CoordinateSystem coordinateSystem,
                                  Position start,
                                  Position end,

                                  String accessionId,
                                  List<Exon> exons) {
        super(contig, strand, coordinateSystem, start, end, accessionId, exons);
    }

    @Override
    public boolean isCoding() {
        return false;
    }

    @Override
    protected NonCodingTranscript newRegionInstance(Contig contig, Strand strand, CoordinateSystem coordinateSystem, Position start, Position end) {
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

        return new NonCodingTranscript(contig, strand, coordinateSystem, start, end, accessionId(), exons);
    }

    @Override
    public String toString() {
        return "NonCodingTranscript{" +
                "accessionId='" + accessionId() + '\'' +
                ", " + contigName() + ':' + NF.format(start()) + '-' + NF.format(end()) + '(' + strand() + ')' +
                '}';
    }
}
