package org.jax.svanna.db.landscape;

import org.jax.svanna.core.landscape.*;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.svart.GenomicRegion;

import java.util.List;
import java.util.Set;

public class DbAnnotationDataService implements AnnotationDataService {

    private final EnhancerAnnotationDao enhancerAnnotationDao;
    private final AnnotationDao<RepetitiveRegion> repetitiveRegionDao;
    private final PopulationVariantDao populationVariantDao;

    public DbAnnotationDataService(EnhancerAnnotationDao enhancerAnnotationDao,
                                   AnnotationDao<RepetitiveRegion> repetitiveRegionDao, PopulationVariantDao populationVariantDao) {
        this.enhancerAnnotationDao = enhancerAnnotationDao;
        this.repetitiveRegionDao = repetitiveRegionDao;
        this.populationVariantDao = populationVariantDao;
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

    @Override
    public Set<PopulationVariantOrigin> availableOrigins() {
        return populationVariantDao.availableOrigins();
    }

    @Override
    public List<PopulationVariant> getOverlapping(GenomicRegion query, Set<PopulationVariantOrigin> origins) {
        return populationVariantDao.getOverlapping(query, origins);
    }

}
