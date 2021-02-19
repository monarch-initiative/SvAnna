package org.jax.svanna.db.landscape;

import org.jax.svanna.core.landscape.AnnotationDao;
import org.jax.svanna.core.landscape.AnnotationDataService;
import org.jax.svanna.core.landscape.Enhancer;
import org.jax.svanna.core.landscape.RepetitiveRegion;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.svart.GenomicRegion;

import java.util.List;
import java.util.Set;

public class DbAnnotationDataService implements AnnotationDataService {

    private final EnhancerAnnotationDao enhancerAnnotationDao;
    private final AnnotationDao<RepetitiveRegion> repetitiveRegionDao;

    public DbAnnotationDataService(EnhancerAnnotationDao enhancerAnnotationDao,
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
    public Set<TermId> enhancerPhenotypeAssociations() {
        return enhancerAnnotationDao.getPhenotypeAssociations();
    }

    @Override
    public List<RepetitiveRegion> overlappingRepetitiveRegions(GenomicRegion query) {
        return repetitiveRegionDao.getOverlapping(query);
    }

}
