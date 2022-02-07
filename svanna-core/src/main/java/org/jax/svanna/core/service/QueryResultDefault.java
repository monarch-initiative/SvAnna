package org.jax.svanna.core.service;

import org.monarchinitiative.sgenes.model.Located;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

class QueryResultDefault<T extends Located> implements QueryResult<T> {

    private final List<T> overlapping;

    private final T upstream;

    private final T downstream;

    QueryResultDefault(List<T> overlapping, T upstream, T downstream) {
        this.overlapping = overlapping;
        this.upstream = upstream;
        this.downstream = downstream;
    }

    @Override
    public List<T> overlapping() {
        return overlapping;
    }

    @Override
    public Optional<T> upstream() {
        return Optional.ofNullable(upstream);
    }

    @Override
    public Optional<T> downstream() {
        return Optional.ofNullable(downstream);
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
