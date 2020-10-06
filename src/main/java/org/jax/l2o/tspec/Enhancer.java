package org.jax.l2o.tspec;

import org.monarchinitiative.phenol.ontology.data.TermId;

public class Enhancer {

    private final static int DEFAULT_DISTANCE_THRESHOLD = 500_000;

    private final String chromosome;
    private final int start;
    private final int end;
    private final double tau;
    private final TermId termId;

    public Enhancer(String chr, int s, int e, double t, TermId tid) {
        this.chromosome = chr;
        this.start = s;
        this.end = e;
        this.tau = t;
        this.termId = tid;
    }

    public boolean matchesPos(String chr, int pos, int THRESHOLD) {
        return this.chromosome.equals(chr)
                && Math.abs(start - pos) < THRESHOLD;
    }


    public boolean matchesPos(String chr, int pos) {
        return matchesPos(chr, pos, DEFAULT_DISTANCE_THRESHOLD);
    }

    public String getChromosome() {
        return chromosome;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    public double getTau() {
        return tau;
    }

    public TermId getTermId() {
        return termId;
    }

    public String getSummary() {
        return String.format("%s:%d-%d [%s]", chromosome, start, end, termId.getValue());
    }
}
