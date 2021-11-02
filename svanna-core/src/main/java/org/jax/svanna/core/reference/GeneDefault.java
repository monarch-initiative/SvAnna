package org.jax.svanna.core.reference;

import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.svart.*;

import java.text.NumberFormat;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class GeneDefault extends BaseGenomicRegion<Gene> implements Gene {

    private static final NumberFormat NF = NumberFormat.getInstance();

    private final TermId accessionId;
    private final String geneSymbol;
    private final Set<CodingTranscript> codingTranscripts;
    private final Set<Transcript> noncodingTranscripts;

    protected GeneDefault(Contig contig, Strand strand, Coordinates coordinates,
                          TermId accessionId, String geneSymbol, Set<CodingTranscript> codingTranscripts, Set<Transcript> noncodingTranscripts) {
        super(contig, strand, coordinates);
        this.accessionId = Objects.requireNonNull(accessionId);
        this.geneSymbol = Objects.requireNonNull(geneSymbol);
        this.codingTranscripts = Objects.requireNonNull(codingTranscripts);
        this.noncodingTranscripts = Objects.requireNonNull(noncodingTranscripts);
    }

    protected GeneDefault(Contig contig, Strand strand, CoordinateSystem coordinateSystem, int startPosition, int endPosition,
                          TermId accessionId, String geneSymbol, Set<CodingTranscript> codingTranscripts, Set<Transcript> noncodingTranscripts) {
        this(contig, strand, Coordinates.of(coordinateSystem, startPosition, endPosition), accessionId, geneSymbol, codingTranscripts, noncodingTranscripts);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public TermId accessionId() {
        return accessionId;
    }

    @Override
    public String geneSymbol() {
        return geneSymbol;
    }

    @Override
    public Set<Transcript> nonCodingTranscripts() {
        return noncodingTranscripts;
    }

    @Override
    public Set<CodingTranscript> codingTranscripts() {
        return codingTranscripts;
    }

    @Override
    protected Gene newRegionInstance(Contig contig, Strand strand, Coordinates coordinates) {
        Set<CodingTranscript> codingTranscripts;
        Set<Transcript> noncodingTranscripts;
        // if we get here we're flipping either strand or changing the coordinate system
        CoordinateSystem coordinateSystem = coordinates.coordinateSystem();
        if (strand != strand()) {
            codingTranscripts = this.codingTranscripts.stream()
                    .map(tx -> tx.withStrand(strand))
                    .collect(Collectors.toUnmodifiableSet());
            noncodingTranscripts = this.noncodingTranscripts.stream()
                    .map(tx -> tx.withStrand(strand))
                    .collect(Collectors.toUnmodifiableSet());
        } else if (coordinateSystem != coordinateSystem()) {
            codingTranscripts = this.codingTranscripts.stream()
                    .map(tx -> tx.withCoordinateSystem(coordinateSystem))
                    .collect(Collectors.toUnmodifiableSet());
            noncodingTranscripts = this.noncodingTranscripts.stream()
                    .map(tx -> tx.withCoordinateSystem(coordinateSystem))
                    .collect(Collectors.toUnmodifiableSet());
        } else
            throw new IllegalArgumentException("We're changing neither strand nor coordinate system. This should not have happened!");

        return new GeneDefault(contig, strand, coordinates, accessionId, geneSymbol, codingTranscripts, noncodingTranscripts);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        GeneDefault that = (GeneDefault) o;
        return Objects.equals(accessionId, that.accessionId) && Objects.equals(geneSymbol, that.geneSymbol) && Objects.equals(codingTranscripts, that.codingTranscripts) && Objects.equals(noncodingTranscripts, that.noncodingTranscripts);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), accessionId, geneSymbol, codingTranscripts, noncodingTranscripts);
    }

    @Override
    public String toString() {
        return "Gene " + geneSymbol + '[' + accessionId + "] " + '(' + contigName() + ':' + NF.format(start()) + '-' + NF.format(end()) + " (" + strand() + ')' +
                ", codingTranscripts=[" + codingTranscripts.stream().map(Transcript::accessionId).collect(Collectors.joining(", ")) + ']' +
                ", noncodingTranscripts=[" + noncodingTranscripts.stream().map(Transcript::accessionId).collect(Collectors.joining(", ")) + ']' +
                '}';
    }

    // not thread safe
    public static class Builder {

        private final Set<CodingTranscript> codingTranscripts = new HashSet<>();
        private final Set<Transcript> noncodingTranscripts = new HashSet<>();

        private TermId accessionId;
        private String geneSymbol;

        private Contig contig = null;

        private Strand strand = null;

        private Builder() {
            // private no-op
        }

        public Builder addTranscript(Transcript transcript) {
            if (transcript instanceof CodingTranscript)
                codingTranscripts.add(((CodingTranscript) transcript).withCoordinateSystem(CoordinateSystem.zeroBased()));
            else
                noncodingTranscripts.add(transcript.withCoordinateSystem(CoordinateSystem.zeroBased()));
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

        public Builder geneSymbol(String geneSymbol) {
            this.geneSymbol = geneSymbol;
            return this;
        }

        public Builder accessionId(TermId accessionId) {
            this.accessionId = accessionId;
            return this;
        }

        private <T extends Transcript> Collection<T> checkTranscriptContigs(Collection<T> transcripts) {
            if (transcripts.stream().allMatch(tx -> tx.contig().equals(contig))) {
                return transcripts;
            } else {
                // Transcripts are on multiple contigs. This may happen for transcripts on chrX and chrY.
                // However, such state is illegal otherwise, hence the IllegalArgumentException.
                Set<String> contigNames = transcripts.stream()
                        .map(Transcript::contigName)
                        .collect(Collectors.toSet());
                if (contigNames.contains("X") && contigNames.contains("Y")) {
                    contig = transcripts.stream()
                            .filter(tx -> tx.contigName().equals("X"))
                            .findFirst()
                            .map(Transcript::contig)
                            .orElseThrow(() -> new IllegalArgumentException("Could not find contig for chrX even though it should have been here"));
                    return transcripts.stream()
                            .filter(tx -> tx.contigName().equals("X"))
                            .collect(Collectors.toUnmodifiableSet());
                } else
                    throw new IllegalArgumentException("Cannot create gene with transcripts on different contigs");
            }
        }

        public Gene build() {
            if (codingTranscripts.isEmpty() && noncodingTranscripts.isEmpty())
                throw new IllegalArgumentException("Cannot create a gene with no transcripts");
            if (codingTranscripts.stream().anyMatch(tx -> tx.strand() != strand)
                    || noncodingTranscripts.stream().anyMatch(tx -> tx.strand() != strand))
                // All transcripts of Gencode genes are always on a single strand
                throw new IllegalArgumentException("Cannot create gene with transcripts on different strands");

            Collection<CodingTranscript> codingTxs = checkTranscriptContigs(codingTranscripts);
            Collection<Transcript> noncodingTxs = checkTranscriptContigs(noncodingTranscripts);

            int start = Integer.MAX_VALUE, end = Integer.MIN_VALUE;
            for (Transcript tx : codingTxs) {
                int txStart = tx.startWithCoordinateSystem(CoordinateSystem.zeroBased());
                if (txStart < start) start = txStart;
                int txEnd = tx.endWithCoordinateSystem(CoordinateSystem.zeroBased());
                if (txEnd > end) end = txEnd;
            }

            for (Transcript tx : noncodingTxs) {
                int txStart = tx.startWithCoordinateSystem(CoordinateSystem.zeroBased());
                if (txStart < start) start = txStart;
                int txEnd = tx.endWithCoordinateSystem(CoordinateSystem.zeroBased());
                if (txEnd > end) end = txEnd;
            }

            return new GeneDefault(contig, strand, CoordinateSystem.zeroBased(), start, end, accessionId, geneSymbol, Set.copyOf(codingTxs), Set.copyOf(noncodingTxs));
        }

    }

}
