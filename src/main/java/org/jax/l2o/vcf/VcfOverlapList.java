package org.jax.l2o.vcf;

import de.charite.compbio.jannovar.impl.intervals.IntervalArray;
import de.charite.compbio.jannovar.reference.GenomeInterval;
import de.charite.compbio.jannovar.reference.GenomePosition;
import de.charite.compbio.jannovar.reference.Strand;
import de.charite.compbio.jannovar.reference.TranscriptModel;
import org.jax.l2o.except.L2ORuntimeException;

import java.util.*;

public class VcfOverlapList {

    private final static int NOT_APPLICABLE = -1;

    private final static int UNINITIALIZED_EXON = -1;

    private final static String N_A = "n/a";



    private List<VcfOverlap> overlaps;

    public VcfOverlapList(List<VcfOverlap> overlaps) {
        System.out.println("in CTOR overlaps=" + overlaps );
        this.overlaps = List.copyOf(overlaps);
    }


    /**
     * Calculate the overlap type
     * @param start start position of SV, assumed to be on the same chromosome (asssumed to be POS strand)
     * @param end end position of SV, assumed to be on the same chromosome (asssumed to be POS strand)
     * @param tmod an overlapping Jannovar TranscriptModel (already identified to overlap somehow with the SV)
     * @return corresponding VcfOverlap object
     */
    public static VcfOverlap getOverlap(String chr, GenomePosition start, GenomePosition end, TranscriptModel tmod) {
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
        int lastInBlack  = UNINITIALIZED_EXON;
        for (int i=0;i<exons.size();i++) {
            var gi = exons.get(i);
            int gi_endpos = gi.withStrand(Strand.FWD).getEndPos();
            if (start.getPos() <= gi_endpos) {
                // current and all remaining exons are downstream
                firstInBlock = i+1;
                // if the start is in the intron, calculate the distance
                int gi_beginpos = gi.withStrand(Strand.FWD).getBeginPos();
                if (start.getPos() < gi_beginpos) {
                    leftDistance = gi_beginpos - start.getPos();
                }
                break;
            }
        }
        for (int i=0;i<exons.size();i++) {
            var gi = exons.get(i);
            int gi_beginpos = gi.withStrand(Strand.FWD).getBeginPos();
            if (end.getPos() >= gi_beginpos) {
                // current and all previous exons are upstream
                lastInBlack = i+1;
                // if the end is in the intron, calculate the distance
                int gi_endpos = gi.withStrand(Strand.FWD).getEndPos();
                if (gi_endpos < end.getPos()) {
                    rightDistance = end.getPos() - gi_endpos;
                }
                break;
            }
        }
        if (lastInBlack == UNINITIALIZED_EXON) {
            throw new L2ORuntimeException("Unable to localized lastInBlock exon for " + tmod.getAccession());
        }
        if (firstInBlock == UNINITIALIZED_EXON) {
            throw new L2ORuntimeException("Unable to localized firstInBlock exon for " + tmod.getAccession());
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
        int lastExon = includedExons.get(includedExons.size()-1);
        String desc = String.format("SV comprises exons %d-%d", firstExon, lastExon);
        return new VcfOverlap(VcfOverlapType.MULTIPLE_EXON_ONE_GENE, leftDistance, rightDistance, desc);

    }


    static public VcfOverlapList factory(GenomePosition start,
                                         GenomePosition end,
                                         IntervalArray<TranscriptModel>.QueryResult qresult) {
        List<VcfOverlap> overlaps = new ArrayList<>();
        if (qresult.getEntries().isEmpty()) {
            // This means that the SV does not overlap with any annotated transcript
            // get distance to nearest transcripts to the left and right
            TranscriptModel tmodelLeft = qresult.getLeft();
            TranscriptModel tmodelRight = qresult.getRight();
            int leftDistance = start.getPos() -  tmodelLeft.getTXRegion().withStrand(Strand.FWD).getEndPos();
            //String leftGene = String.format("%s[%s]",tmodelLeft.getGeneSymbol(), tmodelLeft.getGeneID());
            int rightDistance = tmodelRight.getTXRegion().withStrand(Strand.FWD).getBeginPos() - end.getPos();
          //  String rightGene = String.format("%s[%s]",tmodelRight.getGeneSymbol(), tmodelRight.getGeneID());
            if (overlaps.isEmpty()) {
                // we should never get here, but if we do create an object that will show we failed to find things
                overlaps.add(new VcfOverlap(VcfOverlapType.UNKNOWN, NOT_APPLICABLE,NOT_APPLICABLE, N_A));
                return new VcfOverlapList(overlaps);
            }

            //return new VcfOverlapList(VcfOverlapType.INTERGENIC, leftDistance, leftGene, rightDistance, rightGene);
        }
        // if we get here, then we overlap with one or more genes
        List<TranscriptModel> overlappingTranscripts = qresult.getEntries();
        boolean exonic = false;
        boolean locatedInCds = false;
        for (var tmod : overlappingTranscripts) {
            GenomeInterval cds = tmod.getCDSRegion();
            List<GenomeInterval> exons = tmod.getExonRegions();
            for (int i=0;i<exons.size();++i) {
                var exon = exons.get(i);
                if (exon.contains(start) || exon.contains(end)) {
                    exonic = true;
                    if (cds.contains(start) || cds.contains(end)) {
                        locatedInCds = true;
                    }
                    break;
                }
            }
        }
        return new VcfOverlapList(overlaps);
    }


    @Override
    public String toString() {
        if (overlaps == null) {
            return "OVERLAPS WAS NULL";
        }
        if (this.overlaps.isEmpty()) {
            return "[ERROR] Could not perform overlap analysis";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("[VcfOverlapList\n");
        for (var v : overlaps) {
            sb.append(v + "\n");
        }
        return sb.toString();
    }
}
