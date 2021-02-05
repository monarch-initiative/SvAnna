package org.jax.svanna.ingest.enhancer.fantom;

import org.jax.svanna.ingest.enhancer.AnnotatedTissue;
import org.jax.svanna.ingest.enhancer.IngestedEnhancer;

import java.util.List;
import java.util.Objects;

public class FantomEnhancer implements IngestedEnhancer {
    private final String chromosome;
    private final int start;
    private final int end;
    private final double tau;
    /** HPO id corresponding to the tissue or cell line that has the most counts.*/
    private final AnnotatedTissue topAnnotatedTissue;
    /** Total CPM of this enhancer in all tissues. */
    private final double totalCpm;


    public  FantomEnhancer(String chrom, int s, int e, double tau, AnnotatedTissue tissue, double totalCounts){
        this.chromosome = chrom;
        this.start = s;
        this.end = e;
        this.tau = tau;
        this.topAnnotatedTissue = tissue;
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
        return List.of(this.topAnnotatedTissue);
    }

    public double getTau() {
        return tau;
    }

    public AnnotatedTissue getTop() {
        return this.topAnnotatedTissue;
    }

    public double getTotalCpm() {
        return totalCpm;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FantomEnhancer that = (FantomEnhancer) o;
        return start == that.start &&
                end == that.end &&
                Double.compare(that.tau, tau) == 0 &&
                Double.compare(that.totalCpm, totalCpm) == 0 &&
                Objects.equals(chromosome, that.chromosome) &&
                Objects.equals(topAnnotatedTissue, that.topAnnotatedTissue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(chromosome, start, end, tau, topAnnotatedTissue, totalCpm);
    }

    @Override
    public String toString() {
        return "FantomEnhancer{" +
                "chromosome='" + chromosome + '\'' +
                ", start=" + start +
                ", end=" + end +
                ", tau=" + tau +
                ", top=" + topAnnotatedTissue +
                ", totalCpm=" + totalCpm +
                '}';
    }
}
