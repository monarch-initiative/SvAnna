package org.jax.svann.reference;

import java.util.Objects;

public class StandardGenomePosition implements GenomePosition {

    private final Contig contig;

    private final int pos;

    private final Strand strand;

    public static StandardGenomePosition of(Contig contig, int pos, Strand strand) {
        return new StandardGenomePosition(contig, pos, strand);
    }

    private StandardGenomePosition(Contig contig, int pos, Strand strand) {
        this.contig = contig;
        this.pos = pos;
        this.strand = strand;
    }

    @Override
    public Contig getContig() {
        return contig;
    }

    @Override
    public int getPos() {
        return pos;
    }

    @Override
    public Strand getStrand() {
        return strand;
    }

    @Override
    public GenomePosition withStrand(Strand strand) {
        if (this.strand.equals(strand)) {
            return this;
        } else {
            int pos = contig.getLength() - this.pos;
            return new StandardGenomePosition(contig, pos, strand);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StandardGenomePosition that = (StandardGenomePosition) o;
        return pos == that.pos &&
                Objects.equals(contig, that.contig) &&
                strand == that.strand;
    }

    @Override
    public int hashCode() {
        return Objects.hash(contig, pos, strand);
    }

    @Override
    public String toString() {
        return "GenomePosition{" +
                "contig=" + contig +
                ", position=" + pos +
                ", strand=" + strand +
                '}';
    }
}
