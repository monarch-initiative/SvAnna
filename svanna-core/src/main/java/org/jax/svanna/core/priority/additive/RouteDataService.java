package org.jax.svanna.core.priority.additive;

public interface RouteDataService<T extends RouteData> {

    T getData(Routes route);

}
