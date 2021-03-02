package org.jax.svanna.core.priority.additive;

import org.monarchinitiative.svart.GenomicRegion;

/**
 * This interface returns a number in range <code>[0,...]</code> to estimate gene's functional after the projection.
 * Here, <code>1</code> means that {@link T}'s function is un-affected by the projection,
 * <code>0</code> is for a projection that renders {@link T} to be dysfunctional, <code>2</code> is for double the
 * function.
 */
public interface SequenceImpactCalculator<T extends GenomicRegion> {

    double projectImpact(Projection<T> projection);

    default double noImpact() {
        return 1.;
    }

}
