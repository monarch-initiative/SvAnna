package org.jax.svanna.core.priority.additive;

public interface RouteDataEvaluator<T extends RouteData, R extends RouteResult> {

    R evaluate(T routeData);

}
