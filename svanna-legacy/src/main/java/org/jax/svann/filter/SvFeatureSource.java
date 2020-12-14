package org.jax.svann.filter;

import org.jax.svann.reference.GenomicRegion;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * This interface provides SV features overlapping with given query region.
 */
public interface SvFeatureSource {

    /**
     * Get features that overlap with the given region at least with 1 base.
     *
     * @param region query region
     * @return overlapping features
     */
    default List<SvFeature> getOverlappingFeatures(GenomicRegion region) {
        return getOverlappingFeatures(region, EnumSet.allOf(SVFeatureOrigin.class));
    }

    List<SvFeature> getOverlappingFeatures(GenomicRegion region, Set<SVFeatureOrigin> originSet);
}
