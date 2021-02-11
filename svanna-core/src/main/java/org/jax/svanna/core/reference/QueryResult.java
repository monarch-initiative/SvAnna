package org.jax.svanna.core.reference;

import org.monarchinitiative.svart.GenomicRegion;

import java.util.Optional;
import java.util.Set;

public interface QueryResult<T extends GenomicRegion> {

    Set<T> overlapping();

    Optional<T> upstream();

    Optional<T> downstream();

    static <T extends GenomicRegion> QueryResult<T> empty() {
        return QueryResultEmpty.instance();
    }

    static <T extends GenomicRegion> QueryResult<T> overlapping(Set<T> overlapping) {
        return of(overlapping, null, null);
    }

    static <T extends GenomicRegion> QueryResult<T> neighbors(T upstream, T downstream) {
        return of(Set.of(), upstream, downstream);
    }

    static <T extends GenomicRegion> QueryResult<T> of(Set<T> overlapping, T upstream, T downstream) {
        return QueryResultDefault.of(overlapping, upstream, downstream);
    }
}
