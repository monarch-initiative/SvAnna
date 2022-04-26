package org.monarchinitiative.svanna.core.priority.additive;

/**
 * Result of the prioritization for one or more variants.
 */
public interface RouteResult {

    /**
     * @return real-valued priority with lower bound zero and no upper bound.
     */
    double priority();

}
