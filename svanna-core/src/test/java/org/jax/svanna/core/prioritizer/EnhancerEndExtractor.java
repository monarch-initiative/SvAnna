package org.jax.svanna.core.prioritizer;

import de.charite.compbio.jannovar.impl.intervals.IntervalEndExtractor;
import org.jax.svanna.core.reference.Enhancer;
import org.monarchinitiative.variant.api.Strand;

class EnhancerEndExtractor implements IntervalEndExtractor<Enhancer> {

    @Override
    public int getBegin(Enhancer enhancer) {
        return enhancer.withStrand(Strand.POSITIVE).startGenomicPosition().pos();
    }

    @Override
    public int getEnd(Enhancer enhancer) {
        return enhancer.withStrand(Strand.POSITIVE).endGenomicPosition().pos();
    }
}
