package org.jax.svanna.core.reference;

import org.monarchinitiative.svart.*;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

// Gene is a collection of transcripts with a symbol. Gene is created using a builder. Several checks are performend
// during the build: Gene is at least one consists of >=1 transcript(s), all the transcripts must be
// on the same strand and
public class Gene extends BaseGenomicRegion<Gene> {

    private final String hgvsSymbol;
    private final Set<Transcript> transcripts;

    protected Gene(Contig contig, Strand strand, CoordinateSystem coordinateSystem, Position startPosition, Position endPosition,
                   String hgvsSymbol, Set<Transcript> transcripts) {
        super(contig, strand, coordinateSystem, startPosition, endPosition);
        this.hgvsSymbol = Objects.requireNonNull(hgvsSymbol);
        this.transcripts = transcripts;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String hgvsSymbol() {
        return hgvsSymbol;
    }

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

        return new Gene(contig, strand, coordinateSystem, startPosition, endPosition, hgvsSymbol, transcripts);
    }

    // not thread safe
    public static class Builder {

        private final Set<Transcript> transcripts = new HashSet<>();

        private String hgvsSymbol;

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

        public Builder hgvsSymbol(String hgvsSymbol) {
            this.hgvsSymbol = hgvsSymbol;

            return this;
        }

        public Gene build() {
            if (transcripts.isEmpty())
                throw new IllegalArgumentException("Cannot create a gene with no transcripts");
            if (transcripts.stream().anyMatch(tx -> tx.strand() != strand))
                throw new IllegalArgumentException("Cannot create gene with transcripts on different strands");
            if (transcripts.stream().anyMatch(tx -> !tx.contig().equals(contig)))
                throw new IllegalArgumentException("Cannot create gene with transcripts on different contigs");

            int start = Integer.MAX_VALUE, end = Integer.MIN_VALUE;
            for (Transcript tx : transcripts) {
                int txStart = tx.startWithCoordinateSystem(CoordinateSystem.zeroBased());
                if (txStart < start)
                    start = txStart;
                int txEnd = tx.endWithCoordinateSystem(CoordinateSystem.zeroBased());
                if (txEnd > end)
                    end = txEnd;
            }
            return new Gene(contig, strand, CoordinateSystem.zeroBased(), Position.of(start), Position.of(end), hgvsSymbol, Set.copyOf(transcripts));
        }


    }

}
