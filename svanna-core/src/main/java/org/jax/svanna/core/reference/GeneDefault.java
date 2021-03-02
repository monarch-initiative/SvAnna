package org.jax.svanna.core.reference;

import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.svart.*;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class GeneDefault extends BaseGenomicRegion<Gene> implements Gene {

    private final TermId accessionId;
    private final TermId hgvsSymbol;
    private final Set<Transcript> transcripts;

    protected GeneDefault(Contig contig, Strand strand, CoordinateSystem coordinateSystem, Position startPosition, Position endPosition,
                          TermId accessionId, TermId hgvsSymbol, Set<Transcript> transcripts) {
        super(contig, strand, coordinateSystem, startPosition, endPosition);
//        this.accessionId = Objects.requireNonNull(accessionId);
        this.accessionId = accessionId;
        this.hgvsSymbol = Objects.requireNonNull(hgvsSymbol);
        this.transcripts = transcripts;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public TermId accessionId() {
        return accessionId;
    }

    @Override
    public TermId hgvsName() {
        return hgvsSymbol;
    }

    @Override
    public Set<Transcript> transcripts() {
        return transcripts;
    }

    @Override
    protected Gene newRegionInstance(Contig contig, Strand strand, CoordinateSystem coordinateSystem, Position startPosition, Position endPosition) {
        Set<Transcript> transcripts;
        // if we get here we're flipping either strand or changing the coordinate system
        if (strand != strand())
            transcripts = this.transcripts.stream().map(tx -> tx.withStrand(strand)).collect(Collectors.toUnmodifiableSet());
        else if (coordinateSystem!=coordinateSystem())
            transcripts = this.transcripts.stream().map(tx -> tx.withCoordinateSystem(coordinateSystem)).collect(Collectors.toUnmodifiableSet());
        else
            throw new IllegalArgumentException("We're changing neither strand nor coordinate system. This should not have happened!");

        return new GeneDefault(contig, strand, coordinateSystem, startPosition, endPosition, accessionId, hgvsSymbol, transcripts);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        GeneDefault that = (GeneDefault) o;
        return Objects.equals(accessionId, that.accessionId) && Objects.equals(hgvsSymbol, that.hgvsSymbol) && Objects.equals(transcripts, that.transcripts);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), accessionId, hgvsSymbol, transcripts);
    }

    @Override
    public String toString() {
        return "GeneDefault{" +
                "accessionId=" + accessionId +
                ", hgvsSymbol=" + hgvsSymbol +
                ", transcripts=" + transcripts +
                '}';
    }

    // not thread safe
    public static class Builder {

        private final Set<Transcript> transcripts = new HashSet<>();

        private TermId accessionId;
        private TermId hgvsSymbol;

        private Contig contig = null;

        private Strand strand = null;

        private Builder() {
            // private no-op
        }

        public Builder addTranscript(Transcript transcript) {
            this.transcripts.add(transcript.withCoordinateSystem(CoordinateSystem.zeroBased()));
            if (contig == null)
                contig = transcript.contig();
            if (strand == null)
                strand = transcript.strand();

            return this;
        }

        public Builder addAllTranscripts(Collection<Transcript> transcripts) {
            transcripts.forEach(this::addTranscript);
            return this;
        }

        public Builder hgvsSymbol(TermId hgvsSymbol) {
            this.hgvsSymbol = hgvsSymbol;
            return this;
        }

        public Builder accessionId(TermId accessionId) {
            this.accessionId = accessionId;
            return this;
        }


        public Gene build() {
            if (transcripts.isEmpty())
                throw new IllegalArgumentException("Cannot create a gene with no transcripts");
            if (transcripts.stream().anyMatch(tx -> tx.strand() != strand))
                // All transcripts of Gencode genes are always on a single strand
                throw new IllegalArgumentException("Cannot create gene with transcripts on different strands");
            Set<Transcript> txs = transcripts;
            if (transcripts.stream().anyMatch(tx -> !tx.contig().equals(contig))) {
                Set<String> contigs = transcripts.stream().map(Transcript::contigName).collect(Collectors.toSet());
                if (contigs.contains("X") && contigs.contains("Y")) {
                    txs = transcripts.stream().filter(tx -> tx.contigName().equals("X")).collect(Collectors.toSet());
                } else
                    throw new IllegalArgumentException("Cannot create gene with transcripts on different contigs");
            }

            int start = Integer.MAX_VALUE, end = Integer.MIN_VALUE;
            for (Transcript tx : txs) {
                int txStart = tx.startWithCoordinateSystem(CoordinateSystem.zeroBased());
                if (txStart < start)
                    start = txStart;
                int txEnd = tx.endWithCoordinateSystem(CoordinateSystem.zeroBased());
                if (txEnd > end)
                    end = txEnd;
            }
            return new GeneDefault(contig, strand, CoordinateSystem.zeroBased(), Position.of(start), Position.of(end), accessionId, hgvsSymbol, Set.copyOf(txs));
        }

    }

}
