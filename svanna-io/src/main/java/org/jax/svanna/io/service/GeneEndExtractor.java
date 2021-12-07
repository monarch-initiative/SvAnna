package org.jax.svanna.io.service;

import de.charite.compbio.jannovar.impl.intervals.IntervalEndExtractor;
import org.monarchinitiative.svart.CoordinateSystem;
import org.monarchinitiative.svart.Strand;
import xyz.ielis.silent.genes.model.Gene;

class GeneEndExtractor implements IntervalEndExtractor<Gene> {

    private static final GeneEndExtractor INSTANCE = new GeneEndExtractor();

    static GeneEndExtractor instance() {
        return INSTANCE;
    }

    private GeneEndExtractor() {
    }

    @Override
    public int getBegin(Gene gene) {
        return gene.startOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased());
    }

    @Override
    public int getEnd(Gene gene) {
        return gene.endOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased());
    }
}