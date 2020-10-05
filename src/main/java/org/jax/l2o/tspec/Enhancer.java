package org.jax.l2o.tspec;

import org.monarchinitiative.phenol.ontology.data.TermId;

public class Enhancer {
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
}
