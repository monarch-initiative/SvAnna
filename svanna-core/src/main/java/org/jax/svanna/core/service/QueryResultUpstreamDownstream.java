package org.jax.svanna.core.service;

import xyz.ielis.silent.genes.model.Located;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

class QueryResultUpstreamDownstream<T extends Located> implements QueryResult<T> {

    private final T upstream;
    private final T downstream;

    QueryResultUpstreamDownstream(T upstream, T downstream) {
        this.upstream = upstream;
        this.downstream = downstream;
    }

    @Override
    public List<T> overlapping() {
        return List.of();
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
        QueryResultUpstreamDownstream<?> that = (QueryResultUpstreamDownstream<?>) o;
        return Objects.equals(upstream, that.upstream) && Objects.equals(downstream, that.downstream);
    }

    @Override
    public int hashCode() {
        return Objects.hash(upstream, downstream);
    }
}
