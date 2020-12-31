package org.jax.svann.reference;

import java.util.Comparator;
import java.util.Objects;

public class ConfidenceInterval implements Comparable<ConfidenceInterval> {

    private static final Comparator<ConfidenceInterval> COMPARATOR = Comparator.comparing(ConfidenceInterval::length).reversed();

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

    /**
     * Make confidence interval, absolute values are used for both bounds.
     *
     * @param upstream   number of bases upstream
     * @param downstream number of bases downstream
     * @return confidence interval
     */
    public static ConfidenceInterval of(int upstream, int downstream) {
        int absUp = Math.abs(upstream);
        int absDown = Math.abs(downstream);
        return absUp == 0 && absDown == 0
                ? PRECISE
                : new ConfidenceInterval(absUp, absDown);
    }

    public int getUpstream() {
        return upstream;
    }

    public int getDownstream() {
        return downstream;
    }

    public boolean isPrecise() {
        return this == PRECISE;
    }

    public ConfidenceInterval toOppositeStrand() {
        return isPrecise() ? PRECISE : new ConfidenceInterval(downstream, upstream);
    }

    /**
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
        return COMPARATOR.compare(this, o);
    }
}
