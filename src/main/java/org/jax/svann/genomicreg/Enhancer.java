package org.jax.svann.genomicreg;

import org.jax.svann.reference.*;
import org.jax.svann.reference.genome.Contig;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.Objects;
import java.util.StringJoiner;

public class Enhancer implements GenomicRegion {

    private final static int DEFAULT_DISTANCE_THRESHOLD = 500_000;
    /**
     * The contig (chromosome) on which this enhancer is located.
     */
    private final Contig contig;
    private final EnhancerGenomicPosition start;
    private final EnhancerGenomicPosition end;
    private final double tau;
    private final TermId termId;

    public Enhancer(Contig cony, int s, int e, double t, TermId tid) {
        this.contig = cony;
        this.start = new EnhancerGenomicPosition(cony, s, Strand.FWD, CoordinateSystem.ONE_BASED);
        this.end = new EnhancerGenomicPosition(cony, e, Strand.FWD, CoordinateSystem.ONE_BASED);
        this.tau = t;
        this.termId = tid;
    }

    // TODO: 26. 10. 2020 this should not get contig & position, but ChromosomalRegion (or subclass)
    public boolean matchesPos(Contig otherContig, Position pos, int THRESHOLD) {
        return this.contig.equals(otherContig)
                && Math.abs(start.getPosition() - pos.getPos()) < THRESHOLD;
    }


    // TODO: 26. 10. 2020 this should not get contig & position, but ChromosomalRegion (or subclass)
    public boolean matchesPos(Contig otherContig, Position pos) {
        return matchesPos(otherContig, pos, DEFAULT_DISTANCE_THRESHOLD);
    }

    @Override
    public Contig getContig() {
        return contig;
    }

    @Override
    public GenomicPosition getStart() {
        return start;
    }

    @Override
    public GenomicPosition getEnd() {
        return end;
    }

    public double getTau() {
        return tau;
    }

    public TermId getTermId() {
        return termId;
    }

    public String getSummary() {
        return String.format("%s:%d-%d [%s]", contig, start.getPosition(), end.getPosition(), termId.getValue());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Enhancer enhancer = (Enhancer) o;
        return Double.compare(enhancer.tau, tau) == 0 &&
                Objects.equals(contig, enhancer.contig) &&
                Objects.equals(start, enhancer.start) &&
                Objects.equals(end, enhancer.end) &&
                Objects.equals(termId, enhancer.termId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(contig, start, end, tau, termId);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Enhancer.class.getSimpleName() + "[", "]")
                .add("contig=" + contig)
                .add("start=" + start)
                .add("end=" + end)
                .add("tau=" + tau)
                .add("termId=" + termId)
                .toString();
    }
}
