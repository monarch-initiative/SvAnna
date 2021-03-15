package org.jax.svanna.core;

import org.jax.svanna.core.reference.Exon;
import org.jax.svanna.core.reference.Transcript;
import org.monarchinitiative.svart.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class TestTranscript extends BaseGenomicRegion<TestTranscript> implements Transcript {

    private final GenomicRegion cdsRegion;
    private final List<Exon> exons;

    public static TestTranscript of(Contig contig, Strand strand, int startPosition, int endPosition, List<Integer> exonCoordinates) {
        List<Exon> exons = new ArrayList<>();
        for (int i = 0; i < exonCoordinates.size(); i += 2) {
            Position start = Position.of(exonCoordinates.get(i));
            Position end = Position.of(exonCoordinates.get(i + 1));
            exons.add(Exon.of(CoordinateSystem.zeroBased(), start, end));
        }

        GenomicRegion cds = GenomicRegion.of(contig, strand, CoordinateSystem.zeroBased(), startPosition + 10, endPosition - 10);

        return new TestTranscript(contig, strand, CoordinateSystem.zeroBased(), Position.of(startPosition), Position.of(endPosition), cds, exons);
    }

    protected TestTranscript(Contig contig, Strand strand, CoordinateSystem coordinateSystem, Position startPosition, Position endPosition, GenomicRegion cdsRegion, List<Exon> exons) {
        super(contig, strand, coordinateSystem, startPosition, endPosition);
        this.cdsRegion = cdsRegion;
        this.exons = exons;
    }

    @Override
    public String accessionId() {
        return "ACC";
    }

    @Override
    public String hgvsSymbol() {
        return "HGVS_GENE_SYMBOL";
    }

    @Override
    public Optional<GenomicRegion> cdsRegion() {
        return Optional.of(cdsRegion);
    }

    @Override
    public List<Exon> exons() {
        return exons;
    }

    @Override
    protected TestTranscript newRegionInstance(Contig contig, Strand strand, CoordinateSystem coordinateSystem, Position startPosition, Position endPosition) {
        GenomicRegion cds = cdsRegion;
        if (cds != null)
            cds = cds.withStrand(strand).withCoordinateSystem(coordinateSystem);

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
        return new TestTranscript(contig, strand, coordinateSystem, startPosition, endPosition, cds, exons);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        TestTranscript that = (TestTranscript) o;
        return Objects.equals(exons, that.exons);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), exons);
    }

    @Override
    public String toString() {
        return "TestTranscript{" +
                "cdsRegion=" + cdsRegion +
                ", exons=" + exons +
                '}';
    }
}
