package org.jax.svanna.core.overlap;

import de.charite.compbio.jannovar.impl.intervals.IntervalArray;
import org.jax.svanna.core.landscape.Enhancer;
import org.monarchinitiative.svart.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Calculate the overlap of structural variants with enhancers.
 */
public class EnhancerOverlapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnhancerOverlapper.class);
    /**
     * Key: A chromosome id; value: a Jannovar Interval array for searching for overlapping enhancers.
     */
    private final Map<Integer, IntervalArray<Enhancer>> chromosomeToEnhancerIntervalArrayMap;

    public EnhancerOverlapper(Map<Integer, IntervalArray<Enhancer>> enhancerMap) {
        this.chromosomeToEnhancerIntervalArrayMap = enhancerMap;
    }

    public List<Enhancer> getEnhancerOverlaps(Variant variant) {
        switch (variant.variantType().baseType()) {
            case DEL:
            case INV:
            case DUP:
                return getSimpleEnhancerOverlap(variant);
            case INS:
                // insertion might be an empty region
                return variant.length() == 0 ? enhancerOverlapForEmptyRegion(variant) : getSimpleEnhancerOverlap(variant);
            case TRA:
            case BND:
                return getEnhancerOverlapsForTranslocation((BreakendVariant) variant);
            default:
                if (LOGGER.isWarnEnabled())
                    LOGGER.warn("Enhancer overlap not yet implemented for {}", variant.variantType());
                return List.of();
        }
    }

    public List<Enhancer> getEnhancerRegionOverlaps(GenomicRegion region, int padding) {
        if (!chromosomeToEnhancerIntervalArrayMap.containsKey(region.contigId())) {
            if (LOGGER.isWarnEnabled())
                LOGGER.warn("Could not find enhancers for contig {}", region.contigName());
            return List.of();
        }

        int start = Math.max(region.startOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased()) - padding, 0);
        int end = Math.min(region.endOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased()) + padding, region.contig().length());

        IntervalArray<Enhancer> intervalArray = chromosomeToEnhancerIntervalArrayMap.get(region.contigId());
        return intervalArray.findOverlappingWithInterval(start, end).getEntries();
    }

    /**
     * Get enhancers that overlap with the genomic interval associated with DEL, INV and related SVs
     *
     * @param region a GenomeInterval associated with a structural variant
     * @return a list of overlapping enhancers (can be empty)
     */
    private List<Enhancer> getSimpleEnhancerOverlap(GenomicRegion region) {
        if (!chromosomeToEnhancerIntervalArrayMap.containsKey(region.contigId())) {
            LOGGER.warn("Could not find enhancers for contig {}", region.contigName());
            return List.of();
        }

        int start = region.startOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased());
        int end = region.endOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased());
        IntervalArray<Enhancer> intervalArray = chromosomeToEnhancerIntervalArrayMap.get(region.contigId());
        return intervalArray.findOverlappingWithInterval(start, end).getEntries();
    }


    private List<Enhancer> enhancerOverlapForEmptyRegion(GenomicRegion region) {
        IntervalArray<Enhancer> intervalArray = chromosomeToEnhancerIntervalArrayMap.get(region.contigId());
        int start = region.startOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased()) - 1;
        int end = region.endOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased());
        return intervalArray.findOverlappingWithInterval(start, end).getEntries();
    }

    private List<Enhancer> getEnhancerOverlapsForTranslocation(BreakendVariant variant) {
        List<Enhancer> overlappingEnhancers = new ArrayList<>();

        Breakend left = variant.left();
        overlappingEnhancers.addAll(enhancerOverlapForEmptyRegion(left));

        Breakend right = variant.right();
        overlappingEnhancers.addAll(enhancerOverlapForEmptyRegion(right));

        if (overlappingEnhancers.isEmpty()) {
            return getLongRangeEnhancerEffects(left, right);
        }
        return overlappingEnhancers;
    }

    private List<Enhancer> getLongRangeEnhancerEffects(GenomicRegion left, GenomicRegion right) {
        if (LOGGER.isWarnEnabled())
            LOGGER.warn("Long range enhancer prioritization not implemented");
        return List.of();
    }


}
