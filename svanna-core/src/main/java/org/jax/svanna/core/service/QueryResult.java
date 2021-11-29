package org.jax.svanna.core.service;

import xyz.ielis.silent.genes.model.Located;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public interface QueryResult<T extends Located> {

    static <T extends Located> QueryResult<T> of(Collection<T> overlapping) {
        return of(overlapping, null, null);
    }

    static <T extends Located> QueryResult<T> of(Collection<T> overlapping, T upstream, T downstream) {
        Objects.requireNonNull(overlapping, "Overlapping items collection must not be null");
        if (overlapping.isEmpty()) {
            return (upstream == null && downstream == null)
                    ? empty()
                    : new QueryResultUpstreamDownstream<>(upstream, downstream);
        } else {
            return new QueryResultDefault<>(List.copyOf(overlapping), upstream, downstream);
        }
    }

    static <T extends Located> QueryResult<T> empty() {
        return QueryResultEmpty.instance();
    }

    List<T> overlapping();

    Optional<T> upstream();

    Optional<T> downstream();

    default boolean hasOverlapping() {
        return !overlapping().isEmpty();
    }

    default boolean isEmpty() {
        return this == empty();
    }
}
