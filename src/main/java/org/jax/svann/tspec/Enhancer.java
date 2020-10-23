package org.jax.svann.tspec;

import org.jax.svann.reference.CoordinateSystem;
import org.jax.svann.reference.Position;
import org.jax.svann.reference.genome.Contig;
import org.monarchinitiative.phenol.ontology.data.TermId;

public class Enhancer {

    private final static int DEFAULT_DISTANCE_THRESHOLD = 500_000;
    /** The contig (chromosome) on which this enhancer is located. */
    private final Contig contig;
    private final Position start;
    private final Position end;
    private final double tau;
    private final TermId termId;

    public Enhancer(Contig cony, int s, int e, double t, TermId tid) {
        this.contig = cony;
        this.start = Position.precise(s, CoordinateSystem.ONE_BASED);
        this.end = Position.precise(e, CoordinateSystem.ONE_BASED);
        this.tau = t;
        this.termId = tid;
    }

    public boolean matchesPos(Contig otherContig, Position pos, int THRESHOLD) {
        return this.contig.equals(otherContig)
                && Math.abs(start.getPos() - pos.getPos()) < THRESHOLD;
    }


    public boolean matchesPos(Contig otherContig, Position pos) {
        return matchesPos(otherContig, pos, DEFAULT_DISTANCE_THRESHOLD);
    }

    public Contig getChromosome() {
        return contig;
    }

    public Position getStart() {
        return start;
    }

    public Position getEnd() {
        return end;
    }

    public double getTau() {
        return tau;
    }

    public TermId getTermId() {
        return termId;
    }

    public String getSummary() {
        return String.format("%s:%d-%d [%s]", contig, start, end, termId.getValue());
    }
}
