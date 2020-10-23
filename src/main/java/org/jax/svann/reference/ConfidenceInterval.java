package org.jax.svann.reference;

import java.util.Objects;

public class ConfidenceInterval implements Comparable<ConfidenceInterval> {

    private static final ConfidenceInterval PRECISE = new ConfidenceInterval(0, 0);

    private final int upstream;
    private final int downstream;

    private ConfidenceInterval(int upstream, int downstream) {
        this.upstream = Math.abs(upstream);
        this.downstream = Math.abs(downstream);
    }

    public static ConfidenceInterval precise() {
        return PRECISE;
    }

    /**
     * Make confidence interval, absolute values are used for both bounds.
     *
     * @param upstream   number of bases upstream
     * @param downstream number of bases downstream
     * @return confidence interval
     */
    public static ConfidenceInterval of(int upstream, int downstream) {
        return new ConfidenceInterval(upstream, downstream);
    }

    public int getUpstream() {
        return upstream;
    }

    public int getDownstream() {
        return downstream;
    }

    /**
     *
     * @return length of the confidence interval, precise CI has length <code>0</code>
     */
    public int length() {
        return upstream + downstream;
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
        // upstream coordinate is generally smaller than the current
        return String.format("[%d,%d]", upstream, downstream);
    }

    /**
     * Shorter confidence interval is better.
     *
     * @param o confidence interval to compare with
     * @return comparison result as specified in {@link Comparable}
     */
    @Override
    public int compareTo(ConfidenceInterval o) {
        // the order is reversed on purpose, smaller confidence
        return Integer.compare(o.length(), length());
    }
}
