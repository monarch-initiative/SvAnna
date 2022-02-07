package org.jax.svanna.db;

import org.monarchinitiative.sgenes.model.Located;

public interface IngestDao<T extends Located> {

    int insertItem(T item);
}
