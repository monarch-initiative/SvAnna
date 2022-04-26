package org.monarchinitiative.svanna.core.priority.additive;

import java.util.Objects;

public abstract class RouteData {

    private final Routes routes;

    protected RouteData(Routes routes) {
        this.routes = routes;
    }

    public Routes route() {
        return routes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RouteData routeData = (RouteData) o;
        return Objects.equals(routes, routeData.routes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(routes);
    }

    @Override
    public String toString() {
        return "RouteData{" +
                "route=" + routes +
                '}';
    }
}
