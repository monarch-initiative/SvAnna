package org.jax.svann.parse;

import org.jax.svann.reference.Breakend;

import java.util.Objects;

/**
 * Adjacency ties together 2 breakends.
 */
// TODO(DD): 26. 10. 2020 evaluate if this is useful & correct
public class Adjacency {

    private final Breakend a, b;

    public Adjacency(Breakend a, Breakend b) {
        this.a = a;
        this.b = b;
    }

    public Breakend mateA() {
        return a;
    }

    public Breakend mateB() {
        return b;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Adjacency adjacency = (Adjacency) o;
        return Objects.equals(a, adjacency.a) &&
                Objects.equals(b, adjacency.b);
    }

    @Override
    public int hashCode() {
        return Objects.hash(a, b);
    }

    @Override
    public String toString() {
        return "Adjacency{" +
                "a=" + a +
                ", b=" + b +
                '}';
    }
}
