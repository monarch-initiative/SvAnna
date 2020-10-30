package org.jax.svann.overlap;

import com.google.common.collect.ImmutableMap;
import de.charite.compbio.jannovar.data.Chromosome;
import de.charite.compbio.jannovar.data.JannovarData;
import de.charite.compbio.jannovar.data.ReferenceDictionary;
import de.charite.compbio.jannovar.impl.intervals.IntervalArray;
import de.charite.compbio.jannovar.reference.GenomeInterval;
import de.charite.compbio.jannovar.reference.Strand;
import org.jax.svann.except.SvAnnRuntimeException;
import org.jax.svann.genomicreg.Enhancer;
import org.jax.svann.reference.Adjacency;
import org.jax.svann.reference.Breakend;
import org.jax.svann.reference.SequenceRearrangement;
import org.jax.svann.reference.SvType;
import org.jax.svann.reference.genome.Contig;

import java.util.List;
import java.util.Map;

public class EnhancerOverlapper {
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

    public List<Enhancer> getEnhancerOverlaps(SequenceRearrangement rearrangement) {
        if (rearrangement.getType() == SvType.DELETION) {
            GenomeInterval gi = getDeletionInterval(rearrangement);
            return getSimpleEnhancerOverlap(gi);
        }

        return List.of();
    }


}
