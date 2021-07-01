package org.jax.svanna.core.reference;

import org.monarchinitiative.svart.GenomicRegion;

import java.util.Objects;
import java.util.Set;

class QueryResultDefault<T extends GenomicRegion> implements QueryResult<T> {

    private final Set<T> overlapping;

    private final T upstream;

    private final T downstream;

    static <T extends GenomicRegion> QueryResultDefault<T> of(Set<T> overlapping, T upstream, T downstream) {
        return new QueryResultDefault<>(overlapping, upstream, downstream);
    }

    private QueryResultDefault(Set<T> overlapping, T upstream, T downstream) {
        this.overlapping = overlapping;
        this.upstream = upstream;
        this.downstream = downstream;
    }

    @Override
    public Set<T> overlapping() {
        return overlapping;
    }

    @Override
    public T upstream() {
        return null;
    }

    @Override
    public T downstream() {
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QueryResultDefault<?> that = (QueryResultDefault<?>) o;
        return Objects.equals(overlapping, that.overlapping) && Objects.equals(upstream, that.upstream) && Objects.equals(downstream, that.downstream);
    }

    @Override
    public int hashCode() {
        return Objects.hash(overlapping, upstream, downstream);
    }

    @Override
    public String toString() {
        return "DefaultQueryResult{" +
                "overlapping=" + overlapping +
                ", upstream=" + upstream +
                ", downstream=" + downstream +
                '}';
    }
}
