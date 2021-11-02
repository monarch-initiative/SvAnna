package org.jax.svanna.core;

import org.jax.svanna.core.reference.CodingTranscript;
import org.jax.svanna.core.reference.Gene;
import org.jax.svanna.core.reference.Transcript;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.svart.*;

import java.util.Objects;
import java.util.Set;

public class TestGene extends BaseGenomicRegion<TestGene> implements Gene {

    private final TermId accessionId;
    private final String geneName;

    public static TestGene of(TermId accessionId, String geneName, Contig contig, Strand strand, CoordinateSystem coordinateSystem, int start, int end) {
        return new TestGene(accessionId, geneName, contig, strand, Coordinates.of(coordinateSystem, start, end));
    }

    protected TestGene(TermId accessionId, String geneName, Contig contig, Strand strand, Coordinates coordinates) {
        super(contig, strand, coordinates);
        this.accessionId = accessionId;
        this.geneName = geneName;
    }

    @Override
    public TermId accessionId() {
        return accessionId;
    }

    @Override
    public String geneSymbol() {
        return geneName;
    }

    @Override
    public Set<Transcript> nonCodingTranscripts() {
        return Set.of();
    }

    @Override
    public Set<CodingTranscript> codingTranscripts() {
        return Set.of();
    }

    @Override
    protected TestGene newRegionInstance(Contig contig, Strand strand, Coordinates coordinates) {
        return new TestGene(accessionId, geneName, contig, strand, coordinates);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        TestGene testGene = (TestGene) o;
        return Objects.equals(accessionId, testGene.accessionId) && Objects.equals(geneName, testGene.geneName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), accessionId, geneName);
    }

    @Override
    public String toString() {
        return "TestGene{" +
                "accessionId=" + accessionId +
                ", geneName=" + geneName +
                '}';
    }
}
