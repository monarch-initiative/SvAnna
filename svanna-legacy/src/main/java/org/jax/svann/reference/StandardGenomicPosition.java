package org.jax.svann.reference;

import org.jax.svann.reference.genome.Contig;

import java.text.NumberFormat;
import java.util.Objects;

/**
 * Yet another implementation of a genomic position.
 */
public class StandardGenomicPosition implements GenomicPosition {

    private static final NumberFormat POS_FMT = NumberFormat.getInstance();

    static {
        POS_FMT.setGroupingUsed(true);
    }

    private final Contig contig;

    private final int position;

    private final Strand strand;

    private StandardGenomicPosition(Contig contig, int position, Strand strand) {
        this.contig = contig;
        this.position = position;
        this.strand = strand;
    }

    public static StandardGenomicPosition precise(Contig contig, int position, Strand strand) {
        return new StandardGenomicPosition(contig, position, strand);
    }

    @Override
    public Contig getContig() {
        return contig;
    }

    @Override
    public int getPosition() {
        return position;
    }

    @Override
    public CoordinateSystem getCoordinateSystem() {
        return CoordinateSystem.ONE_BASED;
    }

    @Override
    public Strand getStrand() {
        return strand;
    }

    @Override
    public GenomicPosition withStrand(Strand strand) {
        if (this.strand.equals(strand)) {
            return this;
        } else {
            int pos = contig.getLength() - position + 1;
            return new StandardGenomicPosition(contig, pos, strand);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StandardGenomicPosition that = (StandardGenomicPosition) o;
        return position == that.position &&
                Objects.equals(contig, that.contig) &&
                strand == that.strand;
    }

    @Override
    public int hashCode() {
        return Objects.hash(contig, position, strand);
    }

    @Override
    public String toString() {
        return contig.getPrimaryName() + ":" + POS_FMT.format(position) + "(" + strand + ')';
    }
}
