package org.jax.svanna.core.landscape;

import org.monarchinitiative.svart.GenomicRegion;

import java.util.List;

public interface AnnotationDao<T extends GenomicRegion> {

    List<T> getAllItems();

    List<T> getOverlapping(GenomicRegion query);

}
