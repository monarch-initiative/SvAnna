package org.jax.svanna.db.landscape;

import org.monarchinitiative.svart.GenomicRegion;
import org.monarchinitiative.sgenes.model.Located;

import java.util.List;

public interface AnnotationDao<T extends Located> {

    List<T> getOverlapping(GenomicRegion query);

}
