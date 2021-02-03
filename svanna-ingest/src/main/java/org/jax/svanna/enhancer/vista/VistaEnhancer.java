package org.jax.svanna.enhancer.vista;



import org.jax.svanna.enhancer.AnnotatedTissue;
import org.jax.svanna.enhancer.IngestedEnhancer;

import java.util.List;

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
}
