package org.jax.svanna.core.overlap;

import de.charite.compbio.jannovar.impl.intervals.IntervalArray;
import org.jax.svanna.core.reference.Enhancer;
import org.monarchinitiative.variant.api.*;
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
            case INS:
            case DUP:
                return getSimpleEnhancerOverlap(variant);
            case TRA:
            case BND:
                return getEnhancersAffectedByBreakends(variant);
            default:
                LOGGER.warn("Enhancer overlap not yet implemented for {}", variant.variantType());
                return List.of();
        }
    }

    public List<Enhancer> getEnhancerRegionOverlaps(GenomicRegion region, int padding) {
        region = region.toPositiveStrand();
        Contig contig = region.contig();
        int start = Math.max(region.start() - padding, 0);
        int end = Math.min(region.end() + padding, region.contig().length());

        return getEnhancers(contig, start, end);
    }

    /**
     * Get enhancers that overlap with the genomic interval associated with DEL, INS and related SVs
     *
     * @param region a GenomeInterval associated with a structural variant
     * @return a list of overlapping enhancers (can be empty)
     */
    private List<Enhancer> getSimpleEnhancerOverlap(GenomicRegion region) {
        if (!chromosomeToEnhancerIntervalArrayMap.containsKey(region.contigId())) {
            LOGGER.warn("Could not find enhancers for contig {}", region.contigName());
            return List.of();
        }

        // let's make sure we only use POSITIVE 0-based coordinates for querying the array
        region = region.toPositiveStrand().toZeroBased();
        return getEnhancers(region.contig(), region.start(), region.end());
    }

    private List<Enhancer> getEnhancerOverlapsForTranslocation(Breakended variant) {
        GenomicPosition lb = variant.left().toPositiveStrand();
        IntervalArray<Enhancer> leftArray = chromosomeToEnhancerIntervalArrayMap.get(lb.contigId());
        List<Enhancer> overlappingEnhancers = new ArrayList<>(leftArray.findOverlappingWithPoint(lb.pos()).getEntries());
        GenomicPosition rb = variant.right().toPositiveStrand();
        IntervalArray<Enhancer> rightArray = chromosomeToEnhancerIntervalArrayMap.get(rb.contigId());
        overlappingEnhancers.addAll(rightArray.findOverlappingWithPoint(rb.pos()).getEntries());
        if (overlappingEnhancers.isEmpty()) {
            return getLongRangeEnhancerEffects(lb, rb);
        }
        return overlappingEnhancers;
    }

    /**
     * This is intended to be used to check if structural rearrangements such as inversions
     * affect enhancers -- if a breakend occurs WITHIN an enhancer, we regard this as a match.
     * In contrast, enhancers are defined as being independent of orientation, and so we
     * do not regard it as a match if an enhancer is completely contained within an inversion.
     *
     * @param variant Representation of an inversion, insertion or translocation
     * @return Prioritization
     */
    private List<Enhancer> getEnhancersAffectedByBreakends(Variant variant) {
        switch (variant.variantType()) {
            case INS:
            case INV:
                // we assume that all coordinates of these types are on a single contig
                return getSimpleEnhancerOverlap(variant);
            case TRA:
            case BND:
                // by definition, translocation has coordinates on different contigs
                if (variant instanceof Breakended) {
                    return getEnhancerOverlapsForTranslocation((Breakended) variant);
                }
            default:
                LOGGER.warn("Unable to get enhancers for variant {}", variant);
                return List.of();
        }
    }

    private List<Enhancer> getEnhancers(Contig contig, int start, int end) {
        IntervalArray<Enhancer> intervalArray = chromosomeToEnhancerIntervalArrayMap.get(contig.id());
        return intervalArray.findOverlappingWithInterval(start, end).getEntries();
    }

    private List<Enhancer> getLongRangeEnhancerEffects(GenomicPosition left, GenomicPosition right) {
        System.out.println("[WARNING] Long range enhancer prioritization not implemented");
        return List.of();
    }



}
