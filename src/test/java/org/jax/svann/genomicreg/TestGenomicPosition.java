package org.jax.svann.genomicreg;

import org.jax.svann.reference.CoordinateSystem;
import org.jax.svann.reference.GenomicPosition;
import org.jax.svann.reference.Strand;
import org.jax.svann.reference.genome.Contig;

public class TestGenomicPosition implements GenomicPosition  {
    private final Contig chromosome;
    private final int pos;
    private final Strand strand;


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
    public GenomicPosition withStrand(Strand strand) {
        return new TestGenomicPosition(this.chromosome, this.pos, strand);
    }
}
