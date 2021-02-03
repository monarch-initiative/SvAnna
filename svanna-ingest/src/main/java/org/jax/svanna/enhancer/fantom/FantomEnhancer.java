package org.jax.svanna.enhancer.fantom;

import org.jax.svanna.enhancer.AnnotatedTissue;
import org.jax.svanna.enhancer.IngestedEnhancer;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.List;

public class FantomEnhancer implements IngestedEnhancer {
    private final String chromosome;
    private final int start;
    private final int end;
    private final double tau;
    /** HPO id corresponding to the tissue or cell line that has the most counts.*/
    private final TermId top;
    /** Total CPM of this enhancer in all tissues. */
    private final double totalCpm;


    public  FantomEnhancer(String chrom, int s, int e, double tau, TermId stype, double totalCounts){
        this.chromosome = chrom;
        this.start = s;
        this.end = e;
        this.tau = tau;
        this.top = stype;
        this.totalCpm = totalCounts;
    }

    @Override
    public String getName() {
        return String.format("%s:%d-%d", this.chromosome, this.start, this.end);
    }

    public String getChromosome() {
        return chromosome;
    }

    @Override
    public int getBegin() {
        return this.start;
    }

    @Override
    public int getEnd() {
        return this.end;
    }

    @Override
    public List<AnnotatedTissue> getTissues() {
        return null;
    }

    public double getTau() {
        return tau;
    }

    public TermId getTop() {
        return top;
    }

    public double getTotalCpm() {
        return totalCpm;
    }

}
