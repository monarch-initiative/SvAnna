package org.jax.svanna.core;

import org.jax.svanna.core.reference.Gene;
import org.jax.svanna.core.reference.Transcript;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.svart.*;

import java.util.Objects;
import java.util.Set;

public class TestGene extends BaseGenomicRegion<TestGene> implements Gene {

    private final TermId accessionId;
    private final TermId hgvsSymbol;

    public static TestGene of(TermId accessionId, TermId hgvsSymbol, Contig contig, Strand strand, CoordinateSystem coordinateSystem, int start, int end) {
        return new TestGene(accessionId, hgvsSymbol, contig, strand, coordinateSystem, Position.of(start), Position.of(end));
    }

    protected TestGene(TermId accessionId, TermId hgvsSymbol, Contig contig, Strand strand, CoordinateSystem coordinateSystem, Position startPosition, Position endPosition) {
        super(contig, strand, coordinateSystem, startPosition, endPosition);
        this.accessionId = accessionId;
        this.hgvsSymbol = hgvsSymbol;
    }

    @Override
    public TermId accessionId() {
        return null;
    }

    @Override
    public TermId hgvsName() {
        return hgvsSymbol;
    }

    @Override
    public Set<Transcript> transcripts() {
        return Set.of();
    }

    @Override
    protected TestGene newRegionInstance(Contig contig, Strand strand, CoordinateSystem coordinateSystem, Position startPosition, Position endPosition) {
        return new TestGene(accessionId, hgvsSymbol, contig, strand, coordinateSystem, startPosition, endPosition);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        TestGene testGene = (TestGene) o;
        return Objects.equals(accessionId, testGene.accessionId) && Objects.equals(hgvsSymbol, testGene.hgvsSymbol);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), accessionId, hgvsSymbol);
    }

    @Override
    public String toString() {
        return "TestGene{" +
                "accessionId=" + accessionId +
                ", hgvsSymbol=" + hgvsSymbol +
                '}';
    }
}
