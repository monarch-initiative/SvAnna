package org.jax.svanna.core.reference;

import org.monarchinitiative.svart.GenomicRegion;

import java.util.Set;

public interface QueryResult<T extends GenomicRegion> {

    Set<T> overlapping();

    static <T extends GenomicRegion> QueryResult<T> of(Set<T> overlapping, T upstream, T downstream) {
        return (overlapping.isEmpty() && upstream == null && downstream == null)
                ? empty()
                : QueryResultDefault.of(overlapping, upstream, downstream);
    }

    T upstream();

    T downstream();

    default boolean hasOverlapping() {
        return !overlapping().isEmpty();
    }

    default boolean hasUpstream() {
        return upstream() != null;
    }

    default boolean hasDownstream() {
        return downstream() != null;
    }

    static <T extends GenomicRegion> QueryResult<T> empty() {
        return QueryResultEmpty.instance();
    }

    default boolean isEmpty() {
        return this == empty();
    }
}
