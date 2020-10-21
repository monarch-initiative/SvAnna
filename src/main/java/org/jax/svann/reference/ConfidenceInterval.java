package org.jax.svann.reference;

import java.util.Objects;

public class ConfidenceInterval {

    private static final ConfidenceInterval PRECISE = new ConfidenceInterval(0, 0);

    private final int upstream;
    private final int downstream;

    private ConfidenceInterval(int upstream, int downstream) {
        this.upstream = upstream;
        this.downstream = downstream;
    }

    public static ConfidenceInterval precise() {
        return PRECISE;
    }

    public static ConfidenceInterval of(int upstream, int downstream) {
        return new ConfidenceInterval(upstream, downstream);
    }

    public int getUpstream() {
        return upstream;
    }

    public int getDownstream() {
        return downstream;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConfidenceInterval that = (ConfidenceInterval) o;
        return upstream == that.upstream &&
                downstream == that.downstream;
    }

    @Override
    public int hashCode() {
        return Objects.hash(upstream, downstream);
    }

    @Override
    public String toString() {
        return "ConfidenceInterval{" +
                "upstream=" + upstream +
                ", downstream=" + downstream +
                '}';
    }
}
