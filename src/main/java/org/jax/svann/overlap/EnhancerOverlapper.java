package org.jax.svann.overlap;

import com.google.common.collect.ImmutableMap;
import de.charite.compbio.jannovar.data.Chromosome;
import de.charite.compbio.jannovar.data.JannovarData;
import de.charite.compbio.jannovar.data.ReferenceDictionary;
import de.charite.compbio.jannovar.impl.intervals.IntervalArray;
import de.charite.compbio.jannovar.reference.GenomeInterval;
import de.charite.compbio.jannovar.reference.PositionType;
import de.charite.compbio.jannovar.reference.Strand;
import org.jax.svann.except.SvAnnRuntimeException;
import org.jax.svann.genomicreg.Enhancer;
import org.jax.svann.reference.Adjacency;
import org.jax.svann.reference.Breakend;
import org.jax.svann.reference.SequenceRearrangement;
import org.jax.svann.reference.SvType;
import org.jax.svann.reference.genome.Contig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class EnhancerOverlapper {
    private static final Logger LOGGER = LoggerFactory.getLogger(EnhancerOverlapper.class);
    /**
     * Key: A chromosome id; value: a Jannovar Interval array for searching for overlapping enhancers.
     */
    private final Map<Integer, IntervalArray<Enhancer>> chromosomeToEnhancerIntervalArrayMap;
    /**
     * Reference dictionary (part of {@link JannovarData}).
     */
    private final ReferenceDictionary rd;
    /**
     * Map of Chromosomes (part of {@link JannovarData}). It assigns integers to chromosome names such as CM000666.2.
     */
    private final ImmutableMap<Integer, Chromosome> chromosomeMap;

    public EnhancerOverlapper(JannovarData jdata, Map<Integer, IntervalArray<Enhancer>> enhancerMap) {
        this.chromosomeToEnhancerIntervalArrayMap = enhancerMap;
        this.rd = jdata.getRefDict();
        this.chromosomeMap = jdata.getChromosomes();
    }

    /**
     * Get enhancers that overlap with the genomic interval associated with DEL, INS and related SVs
     *
     * @param gi a GenomeInterval associated with a structural variant
     * @return a list of overlapping enhancers (can be empty)
     */
    private List<Enhancer> getSimpleEnhancerOverlap(GenomeInterval gi) {
        int contigId = gi.getChr();
        IntervalArray<Enhancer> iarray = chromosomeToEnhancerIntervalArrayMap.get(contigId);
        if (iarray == null) {
            throw new SvAnnRuntimeException("Could not find contig for GenomeInterval: {}" + gi);
            //return List.of();
        }
        IntervalArray<Enhancer>.QueryResult queryResult = iarray.findOverlappingWithInterval(gi.getBeginPos(), gi.getEndPos());
        return queryResult.getEntries();
    }

    private GenomeInterval getDeletionInterval(SequenceRearrangement rearrangement) {
        List<Adjacency> adjacencies = rearrangement.getAdjacencies();
        if (adjacencies.size() != 1) {
            throw new SvAnnRuntimeException("Malformed deletion adjacency list with size " + adjacencies.size());
        }
        Adjacency deletion = adjacencies.get(0);
        Breakend left = deletion.getStart();
        Breakend right = deletion.getEnd();
        Contig chrom = left.getContig();
        int id = chrom.getId();
        int begin = left.getPosition();
        int end = right.getPosition();
        return new GenomeInterval(rd, Strand.FWD, id, begin, end);
    }

    List<Enhancer> getEnhancerOverlapsForInsertionAndInversions(Contig contig, int begin, int end) {
        // TODO: 2. 11. 2020 here we assume that the coordinates are always on FWD strand, which might not always hold
        return getSimpleEnhancerOverlap(new GenomeInterval(rd, Strand.FWD, contig.getId(), begin, end, PositionType.ONE_BASED));
    }

    private Collection<? extends Enhancer> getEnhancerOverlapsForTranslocation() {
        // TODO: 2. 11. 2020 implement enhancer overlap for translocations
        LOGGER.warn("Enhancer overlaps for translocations is not yet implemented");
        return List.of();
    }

    @Deprecated
        // consider removal in favor of getEnhancerOverlapsForInsertionAndInversions
    List<Enhancer> getBreakendOverlaps(Breakend be) {
        Contig chrom = be.getContig();
        int id = chrom.getId();
        int begin = be.getPosition();
        GenomeInterval gi = new GenomeInterval(rd, Strand.FWD, id, begin, be.getPosition(), PositionType.ONE_BASED);
        return getSimpleEnhancerOverlap(gi);
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
                overlaps.addAll(getEnhancerOverlapsForInsertionAndInversions(contig, lb.getPosition(), rb.getPosition()));
                break;
            case TRANSLOCATION:
                // by definition, translocation has coordinates on different contigs, correct?
                overlaps.addAll(getEnhancerOverlapsForTranslocation());
                break;
            default:
                LOGGER.warn("");
        }
        return overlaps;
    }


    public List<Enhancer> getEnhancerOverlaps(SequenceRearrangement rearrangement) {
        if (rearrangement.getType() == SvType.DELETION) {
            GenomeInterval gi = getDeletionInterval(rearrangement);
            return getSimpleEnhancerOverlap(gi);
        } else if (rearrangement.getType() == SvType.INVERSION) {
            return getEnhancersAffectedByBreakends(rearrangement);
        } else if (rearrangement.getType() == SvType.INSERTION) {
            return getEnhancersAffectedByBreakends(rearrangement);
        } else if (rearrangement.getType() == SvType.TRANSLOCATION) {
            return getEnhancersAffectedByBreakends(rearrangement);
        }
        LOGGER.warn("Enhancer overlap type not implemented yet");
        return List.of();
    }


}
