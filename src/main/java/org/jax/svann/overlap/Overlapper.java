package org.jax.svann.overlap;

import com.google.common.collect.ImmutableMap;
import de.charite.compbio.jannovar.data.Chromosome;
import de.charite.compbio.jannovar.data.JannovarData;
import de.charite.compbio.jannovar.data.ReferenceDictionary;
import de.charite.compbio.jannovar.impl.intervals.IntervalArray;
import de.charite.compbio.jannovar.reference.*;
import org.jax.svann.except.SvAnnRuntimeException;
import org.jax.svann.reference.Adjacency;
import org.jax.svann.reference.Breakend;
import org.jax.svann.reference.SequenceRearrangement;
import org.jax.svann.reference.SvType;
import org.jax.svann.reference.genome.Contig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.jax.svann.overlap.OverlapType.*;

/**
 * This class determines the kind and degree of overlap of a structural variant with transcript and enhancer
 * features. There is one static method {@link #getOverlapList(GenomeInterval, IntervalArray.QueryResult)}
 * that should be used to get overlaps for any structural variant.
 */
public class Overlapper {
    private static final Logger LOGGER = LoggerFactory.getLogger(Overlapper.class);
    private final static int UNINITIALIZED_EXON = -1;

    /**
     * Reference dictionary (part of {@link JannovarData}).
     */
    private final ReferenceDictionary rd;
    /**
     * Map of Chromosomes (part of {@link JannovarData}). It assigns integers to chromosome names such as CM000666.2.
     */
    private final ImmutableMap<Integer, Chromosome> chromosomeMap;

    public Overlapper(JannovarData jannovarData) {
        rd = jannovarData.getRefDict();
        chromosomeMap = jannovarData.getChromosomes();
    }

    /**
     * By assumption, if we get here we have previously determined that the SV is located within an intron
     * of the transcript. If the method is called for an SV that is only partially in the intron, it can
     * return incorrect results. It does not check this.
     *
     * @param tmod
     * @return
     */
    public static int getIntronNumber(TranscriptModel tmod, int startPos, int endPos) {
        List<GenomeInterval> exons = tmod.getExonRegions();
        if (tmod.getStrand().equals(Strand.FWD)) {
            for (int i = 0; i < exons.size() - 1; i++) {
                if (startPos > exons.get(i).getEndPos() && startPos < exons.get(i + 1).getBeginPos()) {
                    return i + 1;
                }
                if (endPos > exons.get(i).getEndPos() && endPos < exons.get(i + 1).getBeginPos()) {
                    return i + 1;
                }
            }
        } else {
            // reverse order for neg-strand genes.
            for (int i = 0; i < exons.size() - 1; i++) {
                if (startPos < exons.get(i).withStrand(Strand.FWD).getEndPos() && startPos > exons.get(i + 1).withStrand(Strand.FWD).getBeginPos()) {
                    return i + 1;
                }
                if (endPos < exons.get(i).withStrand(Strand.FWD).getEndPos() && endPos > exons.get(i + 1).withStrand(Strand.FWD).getBeginPos()) {
                    return i + 1;
                }
            }
        }
        throw new SvAnnRuntimeException("Could not find intron number");
    }

    /**
     * This is the main method for getting a list of overlapping transcripts (with extra data in an {@link Overlap}
     * object) for structural variants that occur on one Chromosome, such as deletions
     *
     * @param rearrangement a structural variant identified in the input file.
     * @return list of overlapping transcripts.
     * TODO REFACTOR
     */
    public List<Overlap> getOverlapList(SequenceRearrangement rearrangement) {
        if (rearrangement.getType() == SvType.DELETION) {
            return getDeletionOverlaps(rearrangement);
        } else if (rearrangement.getType() == SvType.INSERTION) {
            return getInsertionOverlaps(rearrangement);
        } else if (rearrangement.getType() == SvType.TRANSLOCATION) {
            return getTranslocationOverlaps(rearrangement);
        } else if (rearrangement.getType() == SvType.INVERSION) {
            return getInversionOverlaps(rearrangement);
        } else {
            LOGGER.warn("Getting overlaps for `{}` is not yet supported", rearrangement.getType());
            return List.of();
        }
    }

    /**
     * This method returns all overlapping transcripts for simple deletions. We assume that deletions
     * are forward strand (it does not matter but is needed by the API).
     *
     * @param srearrangement The candidate deletion
     * @return List over overlapping transcripts
     */
    public List<Overlap> getTranslocationOverlaps(SequenceRearrangement srearrangement) {
        List<Adjacency> adjacencies = srearrangement.getAdjacencies();
        if (adjacencies.size() != 1) {
            throw new SvAnnRuntimeException("Malformed delection adjacency list with size " + adjacencies.size());
        }
        Adjacency translocation = adjacencies.get(0);
        Breakend breakendA = translocation.getStart();
        Breakend breakendB = translocation.getEnd();
        List<Overlap> overlapA = getTranslocationPartOverlap(breakendA);
        List<Overlap> overlapB = getTranslocationPartOverlap(breakendB);
        // we would like to return one Overlap object for each end of the translocation
        // if the translocation overlaps a transcript it is high impact -- similar to inversions.
        List<Overlap> translocationOverlapPair = new ArrayList<>();
        Optional<Overlap> optOverlap = overlapA.stream().filter(Overlap::inversionDisruptable).findAny();
        if (optOverlap.isPresent()) {
            translocationOverlapPair.add(optOverlap.get());
        } else {
            translocationOverlapPair.add(overlapA.get(0));
        }
        optOverlap = overlapB.stream().filter(Overlap::inversionDisruptable).findAny();
        if (optOverlap.isPresent()) {
            translocationOverlapPair.add(optOverlap.get());
        } else {
            translocationOverlapPair.add(overlapB.get(0));
        }
        return translocationOverlapPair;
    }

    private List<Overlap> getTranslocationPartOverlap(Breakend bend) {
        Contig chrom = bend.getContig();
        int id = chrom.getId();
        int begin = bend.getPosition();
        int end = bend.getPosition();
        // TODO: 4. 11. 2020 this is in fact a position
        GenomeInterval structVarInterval = new GenomeInterval(rd, Strand.FWD, id, begin, end);
        IntervalArray<TranscriptModel> iarray = chromosomeMap.get(id).getTMIntervalTree();
        IntervalArray<TranscriptModel>.QueryResult queryResult =
                iarray.findOverlappingWithInterval(structVarInterval.getBeginPos(), structVarInterval.getEndPos());
        return getOverlapList(structVarInterval, queryResult);
    }

    /**
     * This method returns all overlapping transcripts for simple deletions. We assume that deletions
     * are forward strand (it does not matter but is needed by the API).
     *
     * @param srearrangement The candidate deletion
     * @return List over overlapping transcripts
     */
    public List<Overlap> getDeletionOverlaps(SequenceRearrangement srearrangement) {
        List<Adjacency> adjacencies = srearrangement.getAdjacencies();
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
        GenomeInterval structVarInterval = new GenomeInterval(rd, Strand.FWD, id, begin, end);
        IntervalArray<TranscriptModel> iarray = chromosomeMap.get(id).getTMIntervalTree();
        IntervalArray<TranscriptModel>.QueryResult queryResult =
                iarray.findOverlappingWithInterval(structVarInterval.getBeginPos(), structVarInterval.getEndPos());
        return getOverlapList(structVarInterval, queryResult);
    }


    List<Overlap> getBreakendOverlaps(Breakend be) {
        Contig chrom = be.getContig();
        int id = chrom.getId();
        int begin = be.getPosition();
        int end = be.getPosition();
        // TODO: 4. 11. 2020 this is in fact a point
        GenomeInterval gi = new GenomeInterval(rd, Strand.FWD, id, begin, end);
        IntervalArray<TranscriptModel> iarray = chromosomeMap.get(id).getTMIntervalTree();
        IntervalArray<TranscriptModel>.QueryResult queryResult =
                iarray.findOverlappingWithInterval(gi.getBeginPos(), gi.getEndPos());
        return getOverlapList(gi, queryResult);
    }

    /**
     * This method checks whether any of the breakends of an inversion overlaps with
     * any part of a transcript
     *
     * @param inversion an inversion
     * @return list of transcript overlaps (can be empty)
     */
    List<Overlap> getInversionOverlaps(SequenceRearrangement inversion) {
        List<Adjacency> adjacencies = inversion.getAdjacencies();
        if (adjacencies.size() != 2) {
            throw new SvAnnRuntimeException("Malformed inversion adjacency list with size " + adjacencies.size());
        }
        List<Overlap> overlaps = new ArrayList<>();
        for (var a : adjacencies) {
            Breakend be = a.getStart();
            List<Overlap> leftOverlaps = getBreakendOverlaps(be);
            leftOverlaps.stream().filter(Overlap::inversionDisruptable).forEach(overlaps::add);
            be = a.getEnd();
            List<Overlap> rightOverlaps = getBreakendOverlaps(be);
            rightOverlaps.stream().filter(Overlap::inversionDisruptable).forEach(overlaps::add);
        }
        return overlaps;
    }

    /**
     * This method checks the content of the inverted region.
     *
     * @param inversion inversion
     * @return list of transcript overlaps (can be empty)
     */
    public List<Overlap> getInversionOverlapsRegionBased(SequenceRearrangement inversion) {
        if (!inversion.getType().equals(SvType.INVERSION)) {
            return List.of();
        }
        SequenceRearrangement onFwd = inversion.withStrand(org.jax.svann.reference.Strand.FWD);
        Breakend left = onFwd.getLeftmostBreakend();
        Breakend right = onFwd.getRightmostBreakend();
        // assume that breakends are on the same contig
        GenomeInterval gi = new GenomeInterval(rd, Strand.FWD, left.getContig().getId(), left.getPosition(), right.getPosition(), PositionType.ONE_BASED);

        IntervalArray<TranscriptModel>.QueryResult qresult = chromosomeMap.get(left.getContig().getId()).getTMIntervalTree().findOverlappingWithInterval(left.getPosition(), right.getPosition());

        return getOverlapList(gi, qresult);
    }


    /**
     * This method returns all overlapping transcripts for insertions. We assume that insertions
     * are forward strand (it does not matter but is needed by the API). Insertions have two
     * adjancencies in our implementation
     *
     * @param srearrangement The candidate insertion
     * @return List over overlapping transcripts
     */
    public List<Overlap> getInsertionOverlaps(SequenceRearrangement srearrangement) {
        List<Adjacency> adjacencies = srearrangement.getAdjacencies();
        if (adjacencies.size() != 2) {
            throw new SvAnnRuntimeException("Malformed insertion adjacency list with size " + adjacencies.size());
        }
        Adjacency insertion1 = adjacencies.get(0);
        Breakend left = insertion1.getStart();
        Breakend right = insertion1.getEnd();
        Contig chrom = left.getContig();
        int id = chrom.getId();
        int begin = left.getPosition();
        Adjacency insertion2 = adjacencies.get(1);
        int end = insertion2.getEnd().getPosition();
        GenomeInterval structVarInterval = new GenomeInterval(rd, Strand.FWD, id, begin, end);
        IntervalArray<TranscriptModel> iarray = chromosomeMap.get(id).getTMIntervalTree();
        IntervalArray<TranscriptModel>.QueryResult queryResult =
                iarray.findOverlappingWithInterval(structVarInterval.getBeginPos(), structVarInterval.getEndPos());
        return getOverlapList(structVarInterval, queryResult);
    }

    public List<Overlap> getOverlapList(GenomeInterval svInt,
                                        IntervalArray<TranscriptModel>.QueryResult qresult) {
        GenomePosition start = svInt.getGenomeBeginPos();
        GenomePosition end = svInt.getGenomeEndPos();
        List<Overlap> overlaps = new ArrayList<>();
        if (qresult.getEntries().isEmpty()) {
            return intergenic(start, end, qresult);
        }
        // if we get here, then we overlap with one or more genes
        List<TranscriptModel> overlappingTranscripts = qresult.getEntries();
        for (var tmod : overlappingTranscripts) {
            if (svInt.contains(tmod.getTXRegion())) {
                // the transcript is completely contained in the SV
                Overlap vover = containedIn(tmod, start, end);
                overlaps.add(vover);
                continue;
            }
            if (!tmod.getTXRegion().contains(start) && !tmod.getTXRegion().contains(end)) {
                LOGGER.error("Warning, transcript model ({};{}) retrieved that does not overlap (chr{}:{}-{}): ",
                        tmod.getGeneSymbol(), tmod.getAccession(), start.getChr(), start.getPos(), end.getPos());
                // TODO I observed this once, it should never happen and may be a Jannovar bug or have some other cause
                //throw new L2ORuntimeException(tmod.getGeneSymbol());
            }
            // TODO if the above bug no longer occurs, make a regular if/else with the above
            Overlap voverlap = genic(tmod, svInt);
            overlaps.add(voverlap);
        }
        if (overlaps.isEmpty()) {
            LOGGER.error("Could not find any overlaps with this query result: {}", qresult);
            throw new SvAnnRuntimeException("Empty overlap list");
        }
        return overlaps;
    }

    /**
     * This is called if the transcriptmodel is entirely contained within an SV
     *
     * @param tmod
     * @param start
     * @param end
     * @return
     */
    private Overlap containedIn(TranscriptModel tmod, GenomePosition start, GenomePosition end) {
        String msg = String.format("%s/%s", tmod.getGeneSymbol(), tmod.getAccession());
        return new Overlap(TRANSCRIPT_CONTAINED_IN_SV, tmod, true, msg);
    }


    private List<Overlap> intergenic(GenomePosition start,
                                     GenomePosition end,
                                     IntervalArray<TranscriptModel>.QueryResult qresult) {
        List<Overlap> overlaps = new ArrayList<>();
        // This means that the SV does not overlap with any annotated transcript
        // get distance to nearest transcripts to the left and right
        TranscriptModel tmodelLeft = qresult.getLeft();
        TranscriptModel tmodelRight = qresult.getRight();
        // if we are 5' or 3' to the first or last gene on the chromosome, then
        // there is not left or right gene anymore
        int leftDistance = 0;
        String description = "";
        if (tmodelLeft != null) {
            leftDistance = start.getPos() - tmodelLeft.getTXRegion().withStrand(Strand.FWD).getEndPos();
            description = String.format("%s[%s]", tmodelLeft.getGeneSymbol(), tmodelLeft.getGeneID());
            OverlapType type;
            if (leftDistance <= 2_000) {
                type = UPSTREAM_GENE_VARIANT_2KB;
            } else if (leftDistance <= 5_000) {
                type = OverlapType.UPSTREAM_GENE_VARIANT_5KB;
            } else if (leftDistance <= 500_000) {
                type = OverlapType.UPSTREAM_GENE_VARIANT_500KB;
            } else {
                type = OverlapType.UPSTREAM_GENE_VARIANT;
            }
            overlaps.add(new Overlap(type, tmodelLeft, leftDistance, description));
        }
        if (tmodelRight != null) {
            int rightDistance = tmodelRight.getTXRegion().withStrand(Strand.FWD).getBeginPos() - end.getPos();
            description = String.format("%s[%s]", tmodelRight.getGeneSymbol(), tmodelRight.getGeneID());
            OverlapType overlapType;
            if (rightDistance <= 500) {
                overlapType = OverlapType.DOWNSTREAM_GENE_VARIANT_500B;
            } else if (rightDistance <= 2_000) {
                overlapType = DOWNSTREAM_GENE_VARIANT_2KB;
            } else if (rightDistance <= 5_000) {
                overlapType = DOWNSTREAM_GENE_VARIANT_5KB;
            } else if (rightDistance <= 500_000) {
                overlapType = DOWNSTREAM_GENE_VARIANT_500KB;
            } else {
                overlapType = DOWNSTREAM_GENE_VARIANT;
            }
            overlaps.add(new Overlap(overlapType, tmodelRight, rightDistance, description));
        }
        return overlaps;
    }

    /**
     * We check for overlap in three ways. The structural variant has an interval (b,e). b or e can
     * be contained within an exon. Alternatively, the entire exon can be contained within (b,e). Note
     * that if we call this method then
     *
     * @param tmod       A transcript
     * @param svInterval a structural variant interval
     * @return object representing the number of the first and last affected exon
     */
    private ExonPair getAffectedExons(TranscriptModel tmod, GenomeInterval svInterval) {
        Optional<Integer> firstAffectedExonNumber = Optional.empty();
        Optional<Integer> lastAffectedExonNumber = Optional.empty();
        List<GenomeInterval> exons = tmod.getExonRegions();
        GenomePosition svStartPos = svInterval.getGenomeBeginPos();
        GenomePosition svEndPos = svInterval.getGenomeEndPos();
        boolean[] affected = new boolean[exons.size()]; // initializes to false
        for (int i = 0; i < exons.size(); i++) {
            GenomeInterval exon = exons.get(i);
            if (exon.contains(svStartPos)) {
                affected[i] = true;
            }
            if (exon.contains(svEndPos)) {
                affected[i] = true;
            }
            if (svInterval.contains(exon)) {
                affected[i] = true;
            }
        }
        // -1 is a code for not applicable
        // we may encounter transcripts where the exons do not overlap
        // in this case, first and last will not be changed
        // the ExonPair object will treat first=last=-1 as a signal that there
        // is no overlap.
        int first = -1;
        int last = -1;
        for (int i = 0; i < affected.length; i++) {
            if (first < 0 && affected[i]) {
                first = i + 1;
                last = first;
            } else if (first > 0 && affected[i]) {
                last = i + 1;
            }
        }
        return new ExonPair(first, last);
    }

    /**
     * Calculate overlap for a non-coding transcript. By assumption, if we get here then we have already
     * determined that the SV overlaps with a non-coding transcript
     *
     * @param tmod a non-coding transcript (this is checked by calling code)
     * @return
     */
    public Overlap genic(TranscriptModel tmod, GenomeInterval svInt) {
        GenomePosition start = svInt.getGenomeBeginPos();
        GenomePosition end = svInt.getGenomeEndPos();
        ExonPair exonPair = getAffectedExons(tmod, svInt);
        boolean affectsCds = false; // note this can only only true if the SV is exonic and the transcript is coding
        if (tmod.isCoding()) {
            GenomeInterval cds = tmod.getCDSRegion();
            if (cds.contains(svInt)) {
                affectsCds = true;
            } else if (svInt.contains(cds)) {
                affectsCds = true;
            } else if (cds.contains(svInt.getGenomeBeginPos()) || cds.contains(svInt.getGenomeEndPos())) {
                affectsCds = true;
            }
        }
        if (exonPair.atLeastOneExonOverlap()) {
            // determine which exons are affected
            int firstAffectedExon = exonPair.getFirstAffectedExon();
            int lastAffectedExon = exonPair.getLastAffectedExon();
            if (firstAffectedExon == lastAffectedExon) {
                String msg = String.format("%s/%s[exon %d]",
                        tmod.getGeneSymbol(),
                        tmod.getAccession(),
                        firstAffectedExon);
                return new Overlap(SINGLE_EXON_IN_TRANSCRIPT, tmod, affectsCds, msg);
            } else {
                String msg = String.format("%s/%s[exon %d-%d]",
                        tmod.getGeneSymbol(),
                        tmod.getAccession(),
                        firstAffectedExon,
                        lastAffectedExon);
                return new Overlap(MULTIPLE_EXON_IN_TRANSCRIPT, tmod, affectsCds, msg);
            }
        } else {
            // if we get here, then both positions must be in the same intron
            int intronNum = getIntronNumber(tmod, start.getPos(), end.getPos());
            String msg = String.format("%s/%s[intron %d]", tmod.getGeneSymbol(), tmod.getAccession(), intronNum);
            return new Overlap(INTRONIC, tmod, false, msg);
        }
    }

}
