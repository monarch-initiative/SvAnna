package org.jax.svanna.db.annotation;

import org.jax.svanna.core.annotation.AnnotationDao;
import org.jax.svanna.core.annotation.AnnotationDataService;
import org.jax.svanna.core.reference.Enhancer;
import org.jax.svanna.core.reference.RepetitiveRegion;
import org.monarchinitiative.svart.GenomicRegion;

import java.util.List;

public class DbAnnotationDataService implements AnnotationDataService {

    private final AnnotationDao<Enhancer> enhancerAnnotationDao;
    private final AnnotationDao<RepetitiveRegion> repetitiveRegionDao;

    public DbAnnotationDataService(AnnotationDao<Enhancer> enhancerAnnotationDao,
                                   AnnotationDao<RepetitiveRegion> repetitiveRegionDao) {
        this.enhancerAnnotationDao = enhancerAnnotationDao;
        this.repetitiveRegionDao = repetitiveRegionDao;
    }

    @Override
    public List<Enhancer> overlappingEnhancers(GenomicRegion query) {
        return enhancerAnnotationDao.getOverlapping(query);
    }

    @Override
    public List<Enhancer> allEnhancers() {
        return enhancerAnnotationDao.getAllItems();
    }

    @Override
    public List<RepetitiveRegion> overlappingRepetitiveRegions(GenomicRegion query) {
        return repetitiveRegionDao.getOverlapping(query);
    }

}
