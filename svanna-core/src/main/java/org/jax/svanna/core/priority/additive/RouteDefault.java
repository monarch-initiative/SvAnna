package org.jax.svanna.core.priority.additive;

import java.util.List;
import java.util.Objects;

class RouteDefault implements Route {

    private final List<RouteLeg> routeLegs;

    static RouteDefault of(List<RouteLeg> routeLegs) {
        if (routeLegs.isEmpty()) throw new IllegalArgumentException("Segment list cannot be empty");
        return new RouteDefault(List.copyOf(routeLegs));
    }

    private RouteDefault(List<RouteLeg> routeLegs) {
        this.routeLegs = routeLegs;
    }

    @Override
    public List<RouteLeg> segments() {
        return routeLegs;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RouteDefault that = (RouteDefault) o;
        return Objects.equals(routeLegs, that.routeLegs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(routeLegs);
    }

    @Override
    public String toString() {
        return "RouteDefault{" +
                "segments=" + routeLegs +
                '}';
    }

}
