package org.jax.svann.genomicreg;

import org.jax.svann.reference.CoordinateSystem;
import org.jax.svann.reference.GenomicPosition;
import org.jax.svann.reference.Strand;
import org.jax.svann.reference.genome.Contig;

public class TestGenomicPosition implements GenomicPosition {
    private final Contig chromosome;
    private final int pos;
    private final Strand strand;


    /**
     * @param con contig
     * @param p   one-based (inclusive) position
     * @param s   strand
     */
    public TestGenomicPosition(Contig con, int p, Strand s) {
        this.chromosome = con;
        this.pos = p;
        this.strand = s;
    }


    @Override
    public Contig getContig() {
        return this.chromosome;
    }

    @Override
    public int getPosition() {
        return this.pos;
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
            int position = chromosome.getLength() - pos + 1;
            return new TestGenomicPosition(chromosome, position, strand);
        }
    }
}
