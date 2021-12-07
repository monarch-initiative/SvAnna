package org.jax.svanna.db;

import xyz.ielis.silent.genes.model.Located;

public interface IngestDao<T extends Located> {

    int insertItem(T item);
}
