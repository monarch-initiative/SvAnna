package org.jax.svann;

import org.jax.svann.reference.ChromosomalRegion;
import org.jax.svann.reference.Position;
import org.jax.svann.reference.Strand;
import org.jax.svann.reference.genome.Contig;

/**
 * {@link ChromosomalRegion} implementation for testing.
 */
@Deprecated
public class ChromosomalRegionImpl implements ChromosomalRegion {

    private final Contig contig;

    private final Position begin, end;

    private final Strand strand;

    public ChromosomalRegionImpl(Contig contig, Position begin, Position end, Strand strand) {
        this.contig = contig;
        this.begin = begin;
        this.end = end;
        this.strand = strand;
    }

    @Override
    public Contig getContig() {
        return contig;
    }

    @Override
    public Position getBeginPosition() {
        return begin;
    }

    @Override
    public Position getEndPosition() {
        return end;
    }

    //    @Override
    public Strand getStrand() {
        return strand;
    }

    @Override
    public ChromosomalRegionImpl withStrand(Strand strand) {
        if (this.strand.equals(strand)) {
            return this;
        } else {
            Position begin = Position.imprecise(contig.getLength() - this.end.getPos() + 1,
                    this.end.getConfidenceInterval().toOppositeStrand());
            Position end = Position.imprecise(contig.getLength() - this.begin.getPos() + 1,
                    this.begin.getConfidenceInterval().toOppositeStrand());
            return new ChromosomalRegionImpl(contig, begin, end, strand);
        }
    }

}
