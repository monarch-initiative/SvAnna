package org.jax.svanna.db.landscape;

import org.monarchinitiative.svart.GenomicRegion;
import xyz.ielis.silent.genes.model.Located;

import java.util.List;

public interface AnnotationDao<T extends Located> {

    @Deprecated(forRemoval = true) // for backward compatibility
    List<T> getAllItems();

    List<T> getOverlapping(GenomicRegion query);

}
