package org.jax.svanna.core.priority.additive;

public interface RouteDataEvaluator<T extends RouteData> {

    double evaluate(T routeData);

}
