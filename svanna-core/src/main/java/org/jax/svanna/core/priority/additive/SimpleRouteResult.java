package org.jax.svanna.core.priority.additive;

@Deprecated // TODO - remove if there is no usage for this class anymore
public class SimpleRouteResult implements RouteResult {

    private final double priority;

    public static SimpleRouteResult of(double priority) {
        return new SimpleRouteResult(priority);
    }

    private SimpleRouteResult(double priority) {
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
