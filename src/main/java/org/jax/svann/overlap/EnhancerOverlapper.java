package org.jax.svann.overlap;

import de.charite.compbio.jannovar.impl.intervals.IntervalArray;
import org.jax.svann.genomicreg.Enhancer;
import org.jax.svann.reference.*;
import org.jax.svann.reference.genome.Contig;
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

    /**
     * Get enhancers that overlap with the genomic interval associated with DEL, INS and related SVs
     *
     * @param region a GenomeInterval associated with a structural variant
     * @return a list of overlapping enhancers (can be empty)
     */
    private List<Enhancer> getSimpleEnhancerOverlap(GenomicRegion region) {
        if (!chromosomeToEnhancerIntervalArrayMap.containsKey(region.getContigId())) {
            LOGGER.warn("Could not find enhancers for contig {}", region.getContig().getPrimaryName());
            return List.of();
        }

        // let's make sure we only use FWD coordinates for querying the array
        GenomicRegion onStrand = region.withStrand(Strand.FWD);

        IntervalArray<Enhancer> intervalArray = chromosomeToEnhancerIntervalArrayMap.get(region.getContigId());
        IntervalArray<Enhancer>.QueryResult queryResult = intervalArray.findOverlappingWithInterval(onStrand.getStartPosition(), onStrand.getEndPosition());

        return queryResult.getEntries();
    }

    private List<Enhancer> getEnhancerOverlapsForInsertionAndInversions(Contig contig, int begin, int end) {
        // TODO: 2. 11. 2020 here we assume that the coordinates are always on FWD strand, which might not always hold
        GenomicRegion region = StandardGenomicRegion.precise(contig, begin, end, Strand.FWD);
        return getSimpleEnhancerOverlap(region);
    }


    private List<Enhancer> getLongRangeEnhancerEffects(SequenceRearrangement rearrangement) {
        System.out.println("[WARNING] Long range enhancer prioritization not implemented");
        return List.of();
    }




    private List<Enhancer> getEnhancerOverlapsForTranslocation(SequenceRearrangement rearrangement) {
        Breakend lb = rearrangement.getLeftmostBreakend();
        Breakend rb = rearrangement.getRightmostBreakend();
        GenomicPosition posA = StandardGenomicPosition.precise(lb.getContig(), lb.getPosition(), Strand.FWD);
        GenomicPosition posB = StandardGenomicPosition.precise(rb.getContig(), rb.getPosition(), Strand.FWD);
        GenomicRegion regionA = StandardGenomicRegion.of(posA, posA);
        GenomicRegion regionB = StandardGenomicRegion.of(posB, posB);
        List<Enhancer> overlapA = getSimpleEnhancerOverlap(regionA);
        List<Enhancer> overlapB = getSimpleEnhancerOverlap(regionB);
        List<Enhancer> overlappingEnhancers = new ArrayList<>();
        if (! overlapA.isEmpty()) {
            overlappingEnhancers.addAll(overlapA);
        }
        if (! overlapB.isEmpty()) {
            overlappingEnhancers.addAll(overlapB);
        }
        if (overlappingEnhancers.isEmpty()) {
            return getLongRangeEnhancerEffects(rearrangement);
        }
        return overlappingEnhancers;
    }

    /**
     * This is intended to be used to check if structural rearrangements such as inversions
     * affect enhancers -- if a breakend occurs WITHIN an enhancer, we regard this as a match.
     * In contrast, enhancers are defined as being independent of orientation, and so we
     * do not regard it as a match if an enhancer is completely contained within an inversion.
     *
     * @param rearrangement Representation of an inversion, insertion or translocation TODO rename
     * @return Prioritization
     */
    private List<Enhancer> getEnhancersAffectedByBreakends(SequenceRearrangement rearrangement) {
        List<Enhancer> overlaps = new ArrayList<>();
        Breakend lb = rearrangement.getLeftmostBreakend();
        Breakend rb = rearrangement.getRightmostBreakend();
        switch (rearrangement.getType()) {
            case INSERTION:
            case INVERSION:
                // we assume that all coordinates of these types are on a single contig
                Contig contig = lb.getContig();
                return getEnhancerOverlapsForInsertionAndInversions(contig, lb.getPosition(), rb.getPosition());
            case TRANSLOCATION:
                // by definition, translocation has coordinates on different contigs
                return getEnhancerOverlapsForTranslocation(rearrangement);
            default:
                LOGGER.warn("");
        }
        return overlaps;
    }


    public List<Enhancer> getEnhancerOverlaps(SequenceRearrangement rearrangement) {
        switch (rearrangement.getType()) {
            case DELETION:
                GenomicRegion region = StandardGenomicRegion.of(rearrangement.getLeftmostBreakend(), rearrangement.getRightmostBreakend());
                return getSimpleEnhancerOverlap(region);
            case INVERSION:
            case INSERTION:
            case TRANSLOCATION:
                return getEnhancersAffectedByBreakends(rearrangement);
            default:
                LOGGER.warn("Enhancer overlap not yet implemented for {}", rearrangement.getType());
                return List.of();
        }
    }


}
