package org.monarchinitiative.svanna.core.overlap;

import org.monarchinitiative.sgenes.model.Gene;

import java.util.*;

public class GeneOverlap {

    private final Gene gene;
    private final SortedMap<String, TranscriptOverlap> txOverlapByAccessionId;

    public static GeneOverlap of(Gene gene, Map<String, TranscriptOverlap> txOverlapByAccessionId) {
        SortedMap<String, TranscriptOverlap> txs = new TreeMap<>(txOverlapByAccessionId);
        return new GeneOverlap(gene, txs);
    }

    private GeneOverlap(Gene gene, SortedMap<String, TranscriptOverlap> txOverlapByAccessionId) {
        this.gene = Objects.requireNonNull(gene);
        this.txOverlapByAccessionId = Objects.requireNonNull(txOverlapByAccessionId);
        if (txOverlapByAccessionId.isEmpty())
            throw new IllegalArgumentException("Gene overlap must contain at least one transcript overlap");
    }

    public Gene gene() {
        return gene;
    }

    public Collection<TranscriptOverlap> transcriptOverlaps() {
        return txOverlapByAccessionId.values();
    }

    public OverlapType overlapType() {
        return topTranscriptOverlap()
                .map(TranscriptOverlap::getOverlapType)
                .orElse(OverlapType.UNKNOWN);
    }

    private Optional<TranscriptOverlap> topTranscriptOverlap() {
        return txOverlapByAccessionId.values().stream()
                .max(Comparator.comparing(TranscriptOverlap::getOverlapType));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GeneOverlap that = (GeneOverlap) o;
        return Objects.equals(gene, that.gene) && Objects.equals(txOverlapByAccessionId, that.txOverlapByAccessionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(gene, txOverlapByAccessionId);
    }

    @Override
    public String toString() {
        return "GeneOverlap{" +
                "symbol='" + gene.symbol() + '\'' +
                ", transcriptOverlaps=" + txOverlapByAccessionId +
                '}';
    }
}
