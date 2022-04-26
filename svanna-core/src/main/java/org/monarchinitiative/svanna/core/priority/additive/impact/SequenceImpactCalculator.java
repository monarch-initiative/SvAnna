package org.monarchinitiative.svanna.core.priority.additive.impact;

import org.monarchinitiative.svanna.core.priority.additive.Projection;
import org.monarchinitiative.sgenes.model.Located;

/**
 * This interface returns a number in range <code>[0,...]</code> to estimate {@link T}'s function on the allele
 * represented by the {@link Projection}.
 * <p>
 * The default function is <code>1</code>; this means that {@link T}'s function
 * is the same as on the reference allele. The value of <code>0</code> is for an allele that renders {@link T}
 * dysfunctional, <code>2</code> is for doubling of the function.
 */
public interface SequenceImpactCalculator<T extends Located> {

    double projectImpact(Projection<T> projection);

    default double noImpact() {
        return 1.;
    }

}
