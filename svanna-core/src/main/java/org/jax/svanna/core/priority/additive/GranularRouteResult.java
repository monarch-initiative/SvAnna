package org.jax.svanna.core.priority.additive;

import org.jax.svanna.core.priority.GeneAwareSvPriority;

import java.util.Map;

/**
 * Route result with the additional info regarding scores wrt. individual genes.
 */
public interface GranularRouteResult extends RouteResult, GeneAwareSvPriority {

    static GranularRouteResult of(Map<String, Double> scores) {
        return GranularRouteResultDefault.of(scores);
    }

    @Override
    default double priority() {
        return getPriority();
    }
}
