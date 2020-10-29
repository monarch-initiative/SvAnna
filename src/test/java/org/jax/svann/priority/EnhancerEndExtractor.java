package org.jax.svann.priority;

import de.charite.compbio.jannovar.impl.intervals.IntervalEndExtractor;
import org.jax.svann.genomicreg.Enhancer;

class EnhancerEndExtractor implements IntervalEndExtractor<Enhancer> {

    @Override
    public int getBegin(Enhancer enhancer) {
        return enhancer.getStart().getPos();
    }

    @Override
    public int getEnd(Enhancer enhancer) {
        return enhancer.getEnd().getPos();
    }
}
