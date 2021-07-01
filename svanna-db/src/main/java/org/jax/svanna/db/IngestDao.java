package org.jax.svanna.db;

import org.monarchinitiative.svart.GenomicRegion;

public interface IngestDao<T extends GenomicRegion> {

    int insertItem(T item);
}
