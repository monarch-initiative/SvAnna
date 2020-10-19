package org.jax.svann.tspec;

import java.util.Comparator;
import java.util.Objects;

import static java.util.Comparator.comparing;

/**
 * Represents the strand and the position of a transcription start site.
 * @author Peter Robinson
 */
public class TssPosition implements Comparable<TssPosition> {

    private final String geneSymbol;
    private final String transcriptId;
    private final GenomicPosition genomicPosition;


    public TssPosition(String gene, String transcript, String chr, int pos, boolean ps) {
        this.geneSymbol = gene;
        this.transcriptId = transcript;
        this.genomicPosition = new GenomicPosition(chr, pos, ps);

    }


    public String getGeneSymbol() {
        return geneSymbol;
    }

    public String getTranscriptId() {
        return transcriptId;
    }

    public GenomicPosition getGenomicPosition() {
        return genomicPosition;
    }

    /** Be consistent with equals: use the same fields as getSigFields().*/
    private static Comparator<TssPosition> COMPARATOR =
                comparing(TssPosition::getGenomicPosition)
                        .thenComparing(TssPosition::getTranscriptId);


    @Override
    public int compareTo(TssPosition that) {
        return COMPARATOR.compare(this, that);
    }

    @Override
    public int hashCode() {
        return Objects.hash(genomicPosition, transcriptId);
    }

    @Override
    public boolean equals(Object obj) {
        if (! (obj instanceof TssPosition)) {
            return false;
        }
        TssPosition that = (TssPosition) obj;
        return this.genomicPosition.equals(that.genomicPosition) &&
                this.transcriptId.equals(that.transcriptId);
    }
}
