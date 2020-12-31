package org.jax.svann.priority;

import de.charite.compbio.jannovar.impl.intervals.IntervalEndExtractor;
import org.jax.svann.genomicreg.Enhancer;
import org.jax.svann.reference.Strand;

class EnhancerEndExtractor implements IntervalEndExtractor<Enhancer> {

    @Override
    public int getBegin(Enhancer enhancer) {
        return enhancer.withStrand(Strand.FWD).getStart().getPosition();
    }

    @Override
    public int getEnd(Enhancer enhancer) {
        return enhancer.withStrand(Strand.FWD).getEnd().getPosition();
    }
}
