package org.jax.svanna.core.service;

import org.monarchinitiative.sgenes.model.Located;

import java.util.List;
import java.util.Optional;

class QueryResultEmpty<T extends Located> implements QueryResult<T> {

    private static final QueryResultEmpty<?> INSTANCE = new QueryResultEmpty<>();

    @SuppressWarnings("unchecked")
    static <T extends Located> QueryResultEmpty<T> instance() {
        return (QueryResultEmpty<T>) INSTANCE;
    }

    private QueryResultEmpty() {}


    @Override
    public List<T> overlapping() {
        return List.of();
    }

    @Override
    public Optional<T> upstream() {
        return Optional.empty();
    }

    @Override
    public Optional<T> downstream() {
        return Optional.empty();
    }

    @Override
    public String toString() {
        return "QueryResultEmpty{}";
    }
}
