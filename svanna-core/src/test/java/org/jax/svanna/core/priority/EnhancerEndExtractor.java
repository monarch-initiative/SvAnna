package org.jax.svanna.core.priority;

import de.charite.compbio.jannovar.impl.intervals.IntervalEndExtractor;
import org.jax.svanna.model.landscape.enhancer.Enhancer;
import org.monarchinitiative.svart.CoordinateSystem;
import org.monarchinitiative.svart.Strand;

class EnhancerEndExtractor implements IntervalEndExtractor<Enhancer> {

    @Override
    public int getBegin(Enhancer enhancer) {
        return enhancer.startOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased());
    }

    @Override
    public int getEnd(Enhancer enhancer) {
        return enhancer.endOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased());
    }
}
