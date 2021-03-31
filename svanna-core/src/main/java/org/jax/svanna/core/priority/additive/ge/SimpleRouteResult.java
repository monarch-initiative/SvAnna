package org.jax.svanna.core.priority.additive.ge;

import org.jax.svanna.core.priority.additive.RouteResult;

class SimpleRouteResult implements RouteResult {

    private final double priority;

    SimpleRouteResult(double priority) {
        this.priority = priority;
    }

    @Override
    public double priority() {
        return priority;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SimpleRouteResult that = (SimpleRouteResult) o;
        return Double.compare(that.priority, priority) == 0;
    }

    @Override
    public int hashCode() {
        return Double.hashCode(priority);
    }

    @Override
    public String toString() {
        return "SimpleRouteResult{" +
                "priority=" + priority +
                '}';
    }
}
