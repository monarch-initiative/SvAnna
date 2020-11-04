package org.jax.svann.parse;

import org.jax.svann.reference.ConfidenceInterval;
import org.jax.svann.reference.Position;
import org.jax.svann.reference.Strand;
import org.jax.svann.reference.genome.Contig;

import java.util.Objects;

class ChromosomalPosition {

    private final Contig contig;
    private final int position;
    private final ConfidenceInterval ci;
    private final Strand strand;

    private ChromosomalPosition(Contig contig,
                                int position,
                                ConfidenceInterval ci,
                                Strand strand) {
        this.contig = contig;
        this.position = position;
        this.ci = ci;
        this.strand = strand;
    }

    @Deprecated
    static ChromosomalPosition of(Contig contig, Position position, Strand strand) {
        return new ChromosomalPosition(contig, position.getPos(), position.getConfidenceInterval(), strand);
    }

    /**
     * @param contig   contig
     * @param position 1-based position
     * @param ci       confidence interval
     * @param strand   strand
     * @return position
     */
    static ChromosomalPosition imprecise(Contig contig, int position, ConfidenceInterval ci, Strand strand) {
        return new ChromosomalPosition(contig, position, ci, strand);
    }

    /**
     * @param contig   contig
     * @param position 1-based position
     * @param strand   strand
     * @return position
     */
    static ChromosomalPosition precise(Contig contig, int position, Strand strand) {
        return new ChromosomalPosition(contig, position, ConfidenceInterval.precise(), strand);
    }

    public int getPosition() {
        return position;
    }

    public ConfidenceInterval getCi() {
        return ci;
    }

    public Contig getContig() {
        return contig;
    }

    @Deprecated
    public Position getBeginPosition() {
        return Position.imprecise(position, ci);
    }

    public Strand getStrand() {
        return strand;
    }


    public ChromosomalPosition withStrand(Strand strand) {
        if (this.strand.equals(strand)) {
            return this;
        } else {
            int pos = contig.getLength() - position + 1;
            return new ChromosomalPosition(contig, pos, ci.toOppositeStrand(), strand);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChromosomalPosition that = (ChromosomalPosition) o;
        return position == that.position &&
                Objects.equals(contig, that.contig) &&
                Objects.equals(ci, that.ci) &&
                strand == that.strand;
    }

    @Override
    public int hashCode() {
        return Objects.hash(contig, position, ci, strand);
    }

    @Override
    public String toString() {
        return contig.getPrimaryName() + ":" + position + "(" + strand + ")";
    }
}
