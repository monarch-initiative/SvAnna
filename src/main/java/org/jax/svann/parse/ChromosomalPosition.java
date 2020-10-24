package org.jax.svann.parse;

import org.jax.svann.reference.ChromosomalRegion;
import org.jax.svann.reference.Position;
import org.jax.svann.reference.Strand;
import org.jax.svann.reference.genome.Contig;

import java.util.Objects;

class ChromosomalPosition implements ChromosomalRegion {

    private final Contig contig;

    private final Position position;

    private final Strand strand;

    private ChromosomalPosition(Contig contig, Position position, Strand strand) {
        this.contig = contig;
        this.position = position;
        this.strand = strand;
    }

    static ChromosomalPosition of(Contig contig, Position position, Strand strand) {
        return new ChromosomalPosition(contig, position, strand);
    }

    @Override
    public Contig getContig() {
        return contig;
    }

    @Override
    public Position getBeginPosition() {
        return position;
    }

    @Override
    public Strand getStrand() {
        return strand;
    }

    @Override
    public ChromosomalPosition withStrand(Strand strand) {
        if (this.strand.equals(strand)) {
            return this;
        } else {
            Position pos = Position.imprecise(contig.getLength() - position.getPos() + 1,
                    position.getConfidenceInterval());
            return new ChromosomalPosition(contig, pos, strand);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChromosomalPosition that = (ChromosomalPosition) o;
        return Objects.equals(contig, that.contig) &&
                Objects.equals(position, that.position) &&
                strand == that.strand;
    }

    @Override
    public int hashCode() {
        return Objects.hash(contig, position, strand);
    }

    @Override
    public String toString() {
        return contig.getPrimaryName() + ":" + position;
    }
}
