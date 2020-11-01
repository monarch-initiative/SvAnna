package org.jax.svann.overlap;

import com.google.common.collect.ImmutableMap;
import de.charite.compbio.jannovar.data.Chromosome;
import de.charite.compbio.jannovar.data.JannovarData;
import de.charite.compbio.jannovar.data.ReferenceDictionary;
import de.charite.compbio.jannovar.impl.intervals.IntervalArray;
import de.charite.compbio.jannovar.reference.GenomeInterval;
import de.charite.compbio.jannovar.reference.Strand;
import de.charite.compbio.jannovar.reference.TranscriptModel;
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
import java.util.List;
import java.util.Map;

public class EnhancerOverlapper {
    private static final Logger LOGGER = LoggerFactory.getLogger(EnhancerOverlapper.class);
    /** Key: A chromosome id; value: a Jannovar Interval array for searching for overlapping enhancers. */
    private final Map<Integer, IntervalArray<Enhancer>> chromosomeToEnhancerIntervalArrayMap;
    /**
     * Reference dictionary (part of {@link JannovarData}).
     */
    private final ReferenceDictionary rd;
    /**
     * Map of Chromosomes (part of {@link JannovarData}). It assigns integers to chromosome names such as CM000666.2.
     */
    private final ImmutableMap<Integer, Chromosome> chromosomeMap;

    public EnhancerOverlapper(JannovarData jdata, Map<Integer, IntervalArray<Enhancer>>  enhancerMap) {
        this.chromosomeToEnhancerIntervalArrayMap = enhancerMap;
        this.rd = jdata.getRefDict();
        this.chromosomeMap = jdata.getChromosomes();
    }

    /**
     * Get enhancers that overlap with the genomic interval associated with DEL, INS and related SVs
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
        Breakend left = deletion.getLeft();
        Breakend right = deletion.getRight();
        Contig chrom = left.getContig();
        int id = chrom.getId();
        int begin = left.getBegin();
        int end = right.getEnd();
        return new GenomeInterval(rd, Strand.FWD, id, begin,end);
    }

    List<Enhancer> getBreakendOverlaps(Breakend be) {
        Contig chrom = be.getContig();
        int id = chrom.getId();
        int begin = be.getBegin();
        int end = be.getEnd();
        GenomeInterval gi = new GenomeInterval(rd, Strand.FWD, id, begin,end);
        return getSimpleEnhancerOverlap(gi);
    }

    /**
     * This is intended to be used to check if structural rearrangements such as inversions
     * affect enhancers -- if a breakend occurs WITHIN an enhancer, we regard this as a match.
     * In contrast, enhancers are defined as being independent of orientation, and so we
     * do not regard it as a match if an enhancer is completely contained within an inversion.
     * @param inversion Representation of an inversion, insertion or translocation TODO rename
     * @return Prioritization
     */
    private List<Enhancer> getEnhancersAffectedByBreakends(SequenceRearrangement inversion) {
        List<Adjacency> adjacencies = inversion.getAdjacencies();
        if (adjacencies.size() != 2) {
            throw new SvAnnRuntimeException("Malformed inversion adjacency list with size " + adjacencies.size());
        }
        List<Enhancer> overlaps = new ArrayList<>();
        for (var a : adjacencies) {
            Breakend be = a.getLeft();
            if (this.chromosomeMap.containsKey(be.getContig())) {
                List<Enhancer> leftOverlaps = getBreakendOverlaps(be);
                leftOverlaps.stream().forEach(overlaps::add);
            } else {
                LOGGER.error("Skipping left breakend for " +inversion + " because we did not find a contig");
            }
            be = a.getRight();
            if (this.chromosomeMap.containsKey(be.getContig())) {
                List<Enhancer> rightOverlaps = getBreakendOverlaps(be);
                rightOverlaps.stream().forEach(overlaps::add);
            } else {
                LOGGER.error("Skipping right breakend for " +inversion + " because we did not find a contig");
            }
        }
        return overlaps;
    }



    public List<Enhancer> getEnhancerOverlaps(SequenceRearrangement rearrangement) {
        if (rearrangement.getType() == SvType.DELETION) {
            GenomeInterval gi = getDeletionInterval(rearrangement);
            return getSimpleEnhancerOverlap(gi);
        } else if (rearrangement.getType() == SvType.INVERSION) {
            return getEnhancersAffectedByBreakends(rearrangement);
        }
        else if (rearrangement.getType() == SvType.INSERTION) {
            return getEnhancersAffectedByBreakends(rearrangement);
        }
        else if (rearrangement.getType() == SvType.TRANSLOCATION) {
            return getEnhancersAffectedByBreakends(rearrangement);
        }
        System.err.println("[WARNING Enhancer overlap type not implemented yet");
        return List.of();
    }


}
