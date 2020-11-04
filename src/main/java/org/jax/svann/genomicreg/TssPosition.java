package org.jax.svann.genomicreg;

import org.jax.svann.reference.CoordinateSystem;
import org.jax.svann.reference.Position;
import org.jax.svann.reference.Strand;
import org.jax.svann.reference.genome.Contig;

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
    private final EnhancerGenomicPosition genomicPosition;

    /** Be consistent with equals: use the same fields as getSigFields().*/
    private static final Comparator<TssPosition> COMPARATOR =
            comparing(TssPosition::getGenomicPosition)
                    .thenComparing(TssPosition::getTranscriptId);


    public TssPosition(String gene, String transcript, Contig chr, Position pos, Strand strand) {
        this.geneSymbol = gene;
        this.transcriptId = transcript;
        this.genomicPosition = new EnhancerGenomicPosition(chr, pos.getPos(), strand, CoordinateSystem.ONE_BASED);
    }


    public String getGeneSymbol() {
        return geneSymbol;
    }

    public String getTranscriptId() {
        return transcriptId;
    }

    public EnhancerGenomicPosition getGenomicPosition() {
        return genomicPosition;
    }


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
