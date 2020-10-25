package org.jax.svann.overlap;

import com.google.common.collect.ImmutableMap;
import de.charite.compbio.jannovar.data.*;
import de.charite.compbio.jannovar.impl.intervals.IntervalArray;
import de.charite.compbio.jannovar.reference.GenomeInterval;
import de.charite.compbio.jannovar.reference.GenomePosition;
import de.charite.compbio.jannovar.reference.Strand;
import de.charite.compbio.jannovar.reference.TranscriptModel;
import org.jax.svann.except.SvAnnRuntimeException;
import org.jax.svann.reference.IntrachromosomalEvent;
import org.jax.svann.reference.Position;
import org.jax.svann.reference.genome.Contig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;

import static org.jax.svann.overlap.IntronOverlap.getIntronNumber;
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
     * Reference to the Jannovar transcript file data for annotating the VCF file.
     */
    private final JannovarData jannovarData;
    /**
     * Reference dictionary (part of {@link #jannovarData}).
     */
    private final ReferenceDictionary referenceDictionary;
    /**
     * Map of Chromosomes (part of {@link #jannovarData}). It assigns integers to chromosome names such as CM000666.2.
     */
    private final ImmutableMap<Integer, Chromosome> chromosomeMap;

    /**
     * Deserialize the Jannovar transcript data file that comes with Exomiser. Note that Exomiser
     * uses its own ProtoBuf serializetion and so we need to use its Deserializser. In case the user
     * provides a standard Jannovar serialzied file, we try the legacy deserializer if the protobuf
     * deserializer doesn't work.
     *
     * @return the object created by deserializing a Jannovar file.
     */
    public Overlapper(String jannovarPath){
        File f = new File(jannovarPath);
        if (!f.exists()) {
            throw new SvAnnRuntimeException("[FATAL] Could not find Jannovar transcript file at " + jannovarPath);
        }
        try {
            this.jannovarData = new JannovarDataSerializer(jannovarPath).load();
            this.referenceDictionary = this.jannovarData.getRefDict();
            this.chromosomeMap = this.jannovarData.getChromosomes();
        } catch (SerializationException e) {
            LOGGER.error("Could not deserialize Jannovar file with legacy deserializer...");
            throw new SvAnnRuntimeException(String.format("Could not load Jannovar data from %s (%s)",
                    jannovarPath, e.getMessage()));
        }
    }

    /**
     * This is the main method for getting a list of overlapping transcripts (with extra data in an {@link Overlap}
     * object) for structural variants that occur on one Chromosome, such as deletions
     * @param svEvent
     * @return list of overlapping transcripts.
     */
    public List<Overlap> getOverlapList(IntrachromosomalEvent svEvent) {
        int id = svEvent.getContig().getId();
        Strand strand = Strand.FWD; // We assume all SVs are on the forward strand
        GenomeInterval structVarInterval =
                new GenomeInterval(referenceDictionary, strand, id, svEvent.getBegin(), svEvent.getEnd());
        IntervalArray<TranscriptModel> iarray = this.chromosomeMap.get(id).getTMIntervalTree();
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
            if (! tmod.getTXRegion().contains(start) && ! tmod.getTXRegion().contains(end)) {
                System.err.printf("[ERROR] Warning, transcript model (%s;%s) retrieved that does not overlap (chr%s:%d-%d): ",
                        tmod.getGeneSymbol(), tmod.getAccession(), start.getChr(), start.getPos(), end.getPos());
                // TODO I observed this once, it should never happen and may be a Jannovar bug or have some other cause
                //throw new L2ORuntimeException(tmod.getGeneSymbol());
            }
            // TODO -- there is no reason to have separate methods for coding and noncoding.
            if (tmod.isCoding()) {
                Overlap voverlap = coding(tmod, svInt);
                overlaps.add(voverlap);
            } else {
                Overlap voverlap = noncoding(tmod, svInt);
                overlaps.add(voverlap);
            }
        }
        if (overlaps.isEmpty()) {
            System.err.println("Could not find any overlaps with this query result" + qresult);
            throw new SvAnnRuntimeException("Empty overlap list");
        }
        return overlaps;
    }


    private Overlap containedIn(TranscriptModel tmod, GenomePosition start, GenomePosition end) {
        int left = start.getPos() - tmod.getTXRegion().getBeginPos();
        int right = end.getPos() - tmod.getTXRegion().getEndPos();
        String msg = String.format("%s/%s", tmod.getGeneSymbol(), tmod.getAccession());
        return new Overlap(TRANSCRIPT_CONTAINED_IN_SV, left, right, msg);
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
        String leftGene = "";
        if (tmodelLeft != null) {
            leftDistance = start.getPos() - tmodelLeft.getTXRegion().withStrand(Strand.FWD).getEndPos();
            leftGene = String.format("%s[%s]", tmodelLeft.getGeneSymbol(), tmodelLeft.getGeneID());
        }
        int rightDistance = 0;
        String rightGene = "";
        if (tmodelRight != null) {
            rightDistance = tmodelRight.getTXRegion().withStrand(Strand.FWD).getBeginPos() - end.getPos();
            rightGene = String.format("%s[%s]", tmodelRight.getGeneSymbol(), tmodelRight.getGeneID());
        }
        int minDistance = Math.min(leftDistance, rightDistance);
        boolean left = leftDistance == minDistance; // in case of ties, default to left
        if (left) {
            if (minDistance <= 2_000) {
                overlaps.add(new Overlap(OverlapType.UPSTREAM_GENE_VARIANT_2KB, leftDistance, rightDistance, leftGene));
            } else if (minDistance <= 5_000) {
                overlaps.add(new Overlap(OverlapType.UPSTREAM_GENE_VARIANT_5KB, leftDistance, rightDistance, leftGene));
            } else if (minDistance <= 500_000) {
                overlaps.add(new Overlap(OverlapType.UPSTREAM_GENE_VARIANT_500KB, leftDistance, rightDistance, leftGene));
            } else {
                overlaps.add(new Overlap(OverlapType.UPSTREAM_GENE_VARIANT, leftDistance, rightDistance, leftGene));
            }
        } else {
            if (minDistance <= 2_000) {
                overlaps.add(new Overlap(OverlapType.DOWNSTREAM_GENE_VARIANT_2KB, leftDistance, rightDistance, leftGene));
            } else if (minDistance <= 5_000) {
                overlaps.add(new Overlap(OverlapType.DOWNSTREAM_GENE_VARIANT_5KB, leftDistance, rightDistance, leftGene));
            } else if (minDistance <= 500_000) {
                overlaps.add(new Overlap(OverlapType.DOWNSTREAM_GENE_VARIANT_500KB, leftDistance, rightDistance, leftGene));
            } else {
                overlaps.add(new Overlap(OverlapType.DOWNSTREAM_GENE_VARIANT, leftDistance, rightDistance, leftGene));
            }
        }
        return overlaps;
    }

    /**
     * Calculate overlap for a non-coding transcript. By assumption, if we get here then we have already
     * determined that the SV overlaps with a non-coding transcript
     *
     * @param tmod  a non-coding transcript (this is checked by calling code)
     * @param svInt start and end position of the overlapping structural variant
     * @return
     */
    public Overlap noncoding(TranscriptModel tmod, GenomeInterval svInt) {
        //boolean exonic = overlapsExon(tmod, svInt);
        GenomePosition start = svInt.getGenomeBeginPos();
        GenomePosition end = svInt.getGenomeEndPos();
        ExonPair exonPair = getAffectedExons(tmod, svInt);
        if (exonPair.atLeastOneExonOverlap()) {
            // determine which exons are affected
            int firstAffectedExon = exonPair.getFirstAffectedExon();
            int lastAffectedExon = exonPair.getLastAffectedExon();
            if (firstAffectedExon == lastAffectedExon) {
                String msg = String.format("%s/%s[exon %d]",
                        tmod.getGeneSymbol(),
                        tmod.getAccession(),
                        firstAffectedExon);
                return new Overlap(SINGLE_EXON_IN_TRANSCRIPT, 0, 0, msg);
            } else {
                String msg = String.format("%s/%s[exon %d-%d]",
                        tmod.getGeneSymbol(),
                        tmod.getAccession(),
                        firstAffectedExon,
                        lastAffectedExon);
                return new Overlap(MULTIPLE_EXON_IN_TRANSCRIPT, 0, 0, msg);
            }
        } else {
            // if we get here, then both positions must be in the same intron
            int intronNum = getIntronNumber(tmod, start.getPos(), end.getPos());
            String msg = String.format("%s/%s[intron %d]", tmod.getGeneSymbol(), tmod.getAccession(), intronNum);
            return new Overlap(INTRONIC, 0, 0, msg);
        }
    }

    /**
     * We check for overlap in three ways. The structural variant has an interval (b,e). b or e can
     * be contained within an exon. Alternatively, the entire exon can be contained within (b,e). Note
     * that if we call this method then
     * @param tmod A transcript
     * @param svInterval a structural variant interval
     * @return object representing the number of the first and last affected exon
     */
    private ExonPair getAffectedExons(TranscriptModel tmod, GenomeInterval svInterval) {
        Optional<Integer> firstAffectedExonNumber = Optional.empty();
        Optional<Integer> lastAffectedExonNumber = Optional.empty();
        List<GenomeInterval> exons = tmod.getExonRegions();
        GenomePosition svStartPos = svInterval.getGenomeBeginPos();
        GenomePosition svEndPos = svInterval.getGenomeEndPos();
        boolean [] affected = new boolean[exons.size()]; // initializes to false
        for (int i=0; i<exons.size(); i++) {
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
        for (int i=0;i<affected.length;i++) {
            if (first < 0 && affected[i]) {
                first = i+1;
                last = first;
            } else if (first > 0 && affected[i]) {
                last = i+1;
            }
        }
        return new ExonPair(first,last);
    }



    /**
     * Calculate overlap for a non-coding transcript. By assumption, if we get here then we have already
     * determined that the SV overlaps with a non-coding transcript
     *
     * @param tmod  a non-coding transcript (this is checked by calling code)
     * @return
     */
    public Overlap coding(TranscriptModel tmod, GenomeInterval svInt) {

        GenomePosition start = svInt.getGenomeBeginPos();
        GenomePosition end = svInt.getGenomeEndPos();
        //boolean exonic = overlapsExon(tmod, svInt);
        ExonPair exonPair = getAffectedExons(tmod, svInt);
        if (exonPair.atLeastOneExonOverlap()) {
            // determine which exons are affected
            int firstAffectedExon = exonPair.getFirstAffectedExon();
            int lastAffectedExon = exonPair.getLastAffectedExon();

            if (firstAffectedExon == lastAffectedExon){
                String msg = String.format("%s/%s[exon %d]", tmod.getGeneSymbol(), tmod.getAccession(), firstAffectedExon);
                return new Overlap(SINGLE_EXON_IN_TRANSCRIPT, 0, 0, msg);
            } else {
                String msg = String.format("%s/%s[exon %d-%d]",
                        tmod.getGeneSymbol(),
                        tmod.getAccession(),
                        firstAffectedExon,
                        lastAffectedExon);
                return new Overlap(MULTIPLE_EXON_IN_TRANSCRIPT, 0, 0, msg);
            }
        } else {
            // if we get here, then both positions must be in the same intron
            int intronNum = getIntronNumber(tmod, start.getPos(), end.getPos());
            String msg = String.format("%s/%s[intron %d]", tmod.getGeneSymbol(), tmod.getAccession(), intronNum);
            return new Overlap(INTRONIC, 0, 0, msg);
        }
    }


    /**
     * Calculate the overlap type
     *
     * @param start start position of SV, assumed to be on the same chromosome (asssumed to be POS strand)
     * @param end   end position of SV, assumed to be on the same chromosome (asssumed to be POS strand)
     * @param tmod  an overlapping Jannovar TranscriptModel (already identified to overlap somehow with the SV)
     * @return corresponding VcfOverlap object
     */
    @Deprecated
    public Overlap getOverlap(Contig chr, Position start, Position end, TranscriptModel tmod) {
        GenomeInterval cdsRegion = tmod.getCDSRegion();
        List<GenomeInterval> exons = tmod.getExonRegions();
        int indexStart = -1, indexEnd = -1;
        boolean overlapsExon = false;
        boolean overlapsCds = false;
        int leftDistance = 0;
        int rightDistance = 0;
        Set<Integer> downstreamOfStart = new HashSet<>(); // exons downstream of or including 'start'
        Set<Integer> upstreamOfEnd = new HashSet<>(); // exons upstream of or including 'end'
        int firstInBlock = UNINITIALIZED_EXON;
        int lastInBlack = UNINITIALIZED_EXON;
        for (int i = 0; i < exons.size(); i++) {
            var gi = exons.get(i);
            int gi_endpos = gi.withStrand(Strand.FWD).getEndPos();
            if (start.getPos() <= gi_endpos) {
                // current and all remaining exons are downstream
                firstInBlock = i + 1;
                // if the start is in the intron, calculate the distance
                int gi_beginpos = gi.withStrand(Strand.FWD).getBeginPos();
                if (start.getPos() < gi_beginpos) {
                    leftDistance = gi_beginpos - start.getPos();
                }
                break;
            }
        }
        for (int i = 0; i < exons.size(); i++) {
            var gi = exons.get(i);
            int gi_beginpos = gi.withStrand(Strand.FWD).getBeginPos();
            if (end.getPos() >= gi_beginpos) {
                // current and all previous exons are upstream
                lastInBlack = i + 1;
                // if the end is in the intron, calculate the distance
                int gi_endpos = gi.withStrand(Strand.FWD).getEndPos();
                if (gi_endpos < end.getPos()) {
                    rightDistance = end.getPos() - gi_endpos;
                }
                break;
            }
        }
        if (lastInBlack == UNINITIALIZED_EXON) {
            throw new SvAnnRuntimeException("Unable to localized lastInBlock exon for " + tmod.getAccession());
        }
        if (firstInBlock == UNINITIALIZED_EXON) {
            throw new SvAnnRuntimeException("Unable to localized firstInBlock exon for " + tmod.getAccession());
        }
        if (lastInBlack - firstInBlock == -1) {
            // in this case, both ends of the SV are in the same intron
        }
        // the intersection of exons in downstreamOfStart and upstreamOfEnd are the exons
        // included within the interval of the SV (for the current transcript).
        upstreamOfEnd.retainAll(downstreamOfStart);
        // create a list to sort the exons
        List<Integer> includedExons = new ArrayList<>(upstreamOfEnd);
        Collections.sort(includedExons);
        int firstExon = includedExons.get(0);
        int lastExon = includedExons.get(includedExons.size() - 1);
        String desc = String.format("SV comprises exons %d-%d", firstExon, lastExon);
        return new Overlap(MULTIPLE_EXON_IN_TRANSCRIPT, leftDistance, rightDistance, desc);

    }
}
