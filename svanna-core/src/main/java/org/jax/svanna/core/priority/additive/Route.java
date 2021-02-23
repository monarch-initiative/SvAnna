package org.jax.svanna.core.priority.additive;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Annotation route consists of >=1 legs.
 */
public interface Route {

    static Route of(List<RouteLeg> routeLegs) {
        return RouteDefault.of(routeLegs);
    }

    /**
     * @return a non-empty list of legs - genomic regions
     */
    List<RouteLeg> segments();

    default List<String> routeIds() {
        return segments().stream()
                .map(RouteLeg::id)
                .collect(Collectors.toList());
    }

}
