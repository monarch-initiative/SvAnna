package org.jax.svanna.core;

import org.jax.svanna.model.gene.CodingTranscript;
import org.monarchinitiative.svart.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TestTranscript extends BaseGenomicRegion<TestTranscript> implements CodingTranscript {

    private final GenomicRegion cdsRegion;
    private final List<Coordinates> exons;

    public static TestTranscript of(Contig contig, Strand strand, int startPosition, int endPosition, List<Integer> exonCoordinates) {
        List<Coordinates> exons = new ArrayList<>();
        for (int i = 0; i < exonCoordinates.size(); i += 2) {
            int start = exonCoordinates.get(i);
            int end = exonCoordinates.get(i + 1);
            exons.add(Coordinates.of(CoordinateSystem.zeroBased(), start, end));
        }

        GenomicRegion cds = GenomicRegion.of(contig, strand, CoordinateSystem.zeroBased(), startPosition + 10, endPosition - 10);

        return new TestTranscript(contig, strand, Coordinates.of(CoordinateSystem.zeroBased(), startPosition, endPosition), cds, exons);
    }

    protected TestTranscript(Contig contig, Strand strand, Coordinates coordinates, GenomicRegion cdsRegion, List<Coordinates> exons) {
        super(contig, strand, coordinates);
        this.cdsRegion = cdsRegion;
        this.exons = exons;
    }

    @Override
    public String accessionId() {
        return "ACC";
    }

    @Override
    public boolean isCoding() {
        return true;
    }

    @Override
    public int codingStart() {
        return cdsRegion.start();
    }

    @Override
    public int codingEnd() {
        return cdsRegion.end();
    }

    @Override
    public List<Coordinates> exons() {
        return exons;
    }

    @Override
    protected TestTranscript newRegionInstance(Contig contig, Strand strand, Coordinates coordinates) {
        GenomicRegion cds = cdsRegion;
        CoordinateSystem coordinateSystem = coordinates.coordinateSystem();
        if (cds != null)
            cds = cds.withStrand(strand).withCoordinateSystem(coordinateSystem);

        List<Coordinates> exons;
        if (strand() != strand) {
            exons = new ArrayList<>(exons().size());
            for (int i = exons().size() - 1; i >= 0; i--) {
                Coordinates current = exons().get(i);
                exons.add(current.invert(contig)); // intentional
            }
        } else if (coordinateSystem() != coordinateSystem) {
            exons = new ArrayList<>(exons().size());
            for (Coordinates exon : exons()) {
                exons.add(exon.withCoordinateSystem(coordinateSystem));
            }
        } else {
            exons = exons();
        }
        return new TestTranscript(contig, strand, coordinates, cds, exons);
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
