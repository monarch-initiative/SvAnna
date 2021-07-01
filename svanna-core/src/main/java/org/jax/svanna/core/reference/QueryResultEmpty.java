package org.jax.svanna.core.reference;

import org.monarchinitiative.svart.GenomicRegion;

import java.util.Collections;
import java.util.Set;

class QueryResultEmpty<T extends GenomicRegion> implements QueryResult<T> {

    private static final QueryResultEmpty<?> INSTANCE = new QueryResultEmpty<>();

    @SuppressWarnings("unchecked")
    static <T extends GenomicRegion> QueryResultEmpty<T> instance() {
        return (QueryResultEmpty<T>) INSTANCE;
    }

    private QueryResultEmpty() {}


    @Override
    public Set<T> overlapping() {
        return Collections.emptySet();
    }

    @Override
    public T upstream() {
        return null;
    }

    @Override
    public T downstream() {
        return null;
    }


}
