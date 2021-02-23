package org.jax.svanna.db.metadata;

import org.jax.svanna.core.landscape.TadBoundary;

import java.util.Objects;

class TadBoundaryPair {

    private final TadBoundary upstream;
    private final TadBoundary downstream;

    TadBoundaryPair(TadBoundary upstream, TadBoundary downstream) {
        this.upstream = upstream;
        this.downstream = downstream;
    }

    public TadBoundary upstream() {
        return upstream;
    }

    public TadBoundary downstream() {
        return downstream;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TadBoundaryPair that = (TadBoundaryPair) o;
        return Objects.equals(upstream, that.upstream) && Objects.equals(downstream, that.downstream);
    }

    @Override
    public int hashCode() {
        return Objects.hash(upstream, downstream);
    }

    @Override
    public String toString() {
        return "TadBoundaryPair{" +
                "upstream=" + upstream +
                ", downstream=" + downstream +
                '}';
    }
}
