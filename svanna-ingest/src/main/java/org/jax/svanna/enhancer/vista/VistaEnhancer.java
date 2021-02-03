package org.jax.svanna.enhancer.vista;



import org.jax.svanna.enhancer.AnnotatedTissue;
import org.jax.svanna.enhancer.IngestedEnhancer;

import java.util.List;
import java.util.Objects;

public class VistaEnhancer implements IngestedEnhancer {
    private final String name;
    private final String chrom;
    private final int begin;
    private final int end;
    private final List<AnnotatedTissue> tissues;


    public VistaEnhancer(String name, String chrom, int begin, int end, List<AnnotatedTissue> tissues) {
        this.name = name;
        this.chrom = chrom;
        this.begin = begin;
        this.end = end;
        this.tissues = tissues;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getChromosome() {
        return this.chrom;
    }

    @Override
    public int getBegin() {
        return this.begin;
    }

    @Override
    public int getEnd() {
        return this.end;
    }

    @Override
    public List<AnnotatedTissue> getTissues() {
        return this.tissues;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VistaEnhancer that = (VistaEnhancer) o;
        return begin == that.begin && end == that.end && Objects.equals(name, that.name) && Objects.equals(chrom, that.chrom) && Objects.equals(tissues, that.tissues);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, chrom, begin, end, tissues);
    }

    @Override
    public String toString() {
        return "VistaEnhancer{" +
                "name='" + name + '\'' +
                ", chrom='" + chrom + '\'' +
                ", begin=" + begin +
                ", end=" + end +
                ", tissues=" + tissues +
                '}';
    }
}
