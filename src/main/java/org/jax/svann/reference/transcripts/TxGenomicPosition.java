package org.jax.svann.reference.transcripts;

import org.jax.svann.reference.CoordinateSystem;
import org.jax.svann.reference.GenomicPosition;
import org.jax.svann.reference.Strand;
import org.jax.svann.reference.genome.Contig;

import java.util.Objects;

/**
 * Yet another implementation of a genomic position.
 */
class TxGenomicPosition implements GenomicPosition {

    private final Contig contig;

    private final int position;

    private final Strand strand;

    TxGenomicPosition(Contig contig, int position, Strand strand) {
        this.contig = contig;
        this.position = position;
        this.strand = strand;
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
            return new TxGenomicPosition(contig, pos, strand);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TxGenomicPosition that = (TxGenomicPosition) o;
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
        return "TxGenomicPosition{" +
                "contig=" + contig +
                ", position=" + position +
                ", strand=" + strand +
                '}';
    }
}
