package org.jax.svann.genomicreg;

import org.jax.svann.reference.CoordinateSystem;
import org.jax.svann.reference.GenomicPosition;
import org.jax.svann.reference.GenomicRegion;
import org.jax.svann.reference.Strand;
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
    private final GenomicPosition start;
    private final GenomicPosition end;
    private final double tau;
    private final TermId hpoId;
    /** Label of the UBERON or CL term for this enhancer. */
    private final String tissueLabel;

    public Enhancer(Contig contig, int start, int end, double t, TermId tid, String termLabel) {
        this(contig,
                new EnhancerGenomicPosition(contig, start, Strand.FWD, CoordinateSystem.ONE_BASED),
                new EnhancerGenomicPosition(contig, end, Strand.FWD, CoordinateSystem.ONE_BASED),
                t, tid, termLabel);
    }

    private Enhancer(Contig contig, GenomicPosition start, GenomicPosition end, double tau, TermId termId,String termLabel) {
        this.contig = contig;
        this.start = start;
        this.end = end;
        this.tau = tau;
        this.hpoId = termId;
        this.tissueLabel = termLabel;
    }


    // TODO: 26. 10. 2020 this should not get contig & position, but ChromosomalRegion (or subclass)
    public boolean matchesPos(Contig otherContig, int pos, int THRESHOLD) {
        return this.contig.equals(otherContig)
                && Math.abs(start.getPosition() - pos) < THRESHOLD;
    }


    // TODO: 26. 10. 2020 this should not get contig & position, but ChromosomalRegion (or subclass)
    public boolean matchesPos(Contig otherContig, int pos) {
        return matchesPos(otherContig, pos, DEFAULT_DISTANCE_THRESHOLD);
    }

    @Override
    public Contig getContig() {
        return contig;
    }

    @Override
    public Enhancer withStrand(Strand strand) {
        if (getStrand().equals(strand)) {
            return this;
        } else {
            // change position order!!
            return new Enhancer(contig, end.withStrand(strand), start.withStrand(strand), tau, hpoId, tissueLabel);
        }
    }

    @Override
    public Strand getStrand() {
        return start.getStrand();
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

    public TermId getHpoId() {
        return hpoId;
    }

    public String getTissueLabel() {
        return tissueLabel;
    }

    public String getSummary() {
        return String.format("%s:%d-%d [%s]", contig, start.getPosition(), end.getPosition(), hpoId.getValue());
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
                Objects.equals(hpoId, enhancer.hpoId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(contig, start, end, tau, hpoId);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Enhancer.class.getSimpleName() + "[", "]")
                .add("contig=" + contig)
                .add("start=" + start)
                .add("end=" + end)
                .add("tau=" + tau)
                .add("termId=" + hpoId)
                .toString();
    }
}
