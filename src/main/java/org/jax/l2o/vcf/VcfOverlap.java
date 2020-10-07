package org.jax.l2o.vcf;

import de.charite.compbio.jannovar.impl.intervals.IntervalArray;
import de.charite.compbio.jannovar.reference.GenomeInterval;
import de.charite.compbio.jannovar.reference.GenomePosition;
import de.charite.compbio.jannovar.reference.TranscriptModel;
import org.jax.l2o.tspec.GenomicPosition;

import java.util.List;

public class VcfOverlap {

    private final static int NOT_APPLICABLE = -1;

    private final static String N_A = "n/a";

    private final VcfOverlapType overlapType;
    /** This field's meaning depends on the type. For INTERGENIC, it is the distance to the 5' (left) nearest gene.
     * For INTRONIC, it is the distance to the 5' (left) nearest exon.
     */
    private final int leftDistance;

    private final int rightDistance;

    private final String leftString;

    private final String rightString;

    public VcfOverlap(VcfOverlapType type, int left, String leftString, int right, String rightString) {
        this.overlapType = type;
        this.leftDistance = left;
        this.rightDistance = right;
        this.leftString = leftString;
        this.rightString = rightString;

    }


    /**
     * Calculate the overlap type
     * @param start start position of SV, assumed to be on the same chromosome
     * @param end end position of SV, assumed to be on the same chromosome
     * @param tmod an overlapping Jannovar TranscriptModel (already identified to overlap somehow with the SV)
     * @return corresponding VcfOverlap object
     */
    public static VcfOverlap getOverlap(String chr, GenomePosition start, GenomePosition end, TranscriptModel tmod) {
        GenomeInterval cdsRegion = tmod.getCDSRegion();
        List<GenomeInterval> exons = tmod.getExonRegions();
        int indexStart = -1, indexEnd = -1;
        boolean overlapsExon = false;
        boolean overlapsCds = false;
        for (int i=0;i<exons.size();i++) {
            var gi = exons.get(i);
            if (gi.contains(start)) {
                indexStart = i+1; // one-based
                overlapsExon = true;
            }
            if (gi.contains(end)) {
                indexEnd = i+1;
                overlapsExon = true;
                break;
            }
        }
        if (cdsRegion.contains(start) || cdsRegion.contains(end)) {
            overlapsCds = true;
        }


        return new VcfOverlap(VcfOverlapType.UNKNOWN, NOT_APPLICABLE, N_A, NOT_APPLICABLE, N_A);
    }


    static public VcfOverlap factory(GenomePosition start,
                                     GenomePosition end,
                                     IntervalArray<TranscriptModel>.QueryResult qresult) {
        if (qresult.getEntries().isEmpty()) {
            // This means that the SV does not overlap with any annotated transcript
            // get distance to nearest transcripts to the left and right
            TranscriptModel tmodelLeft = qresult.getLeft();
            TranscriptModel tmodelRight = qresult.getRight();
            int leftDistance = start.getPos() -  tmodelLeft.getTXRegion().getEndPos();
            String leftGene = String.format("%s[%s]",tmodelLeft.getGeneSymbol(), tmodelLeft.getGeneID());
            int rightDistance = tmodelRight.getTXRegion().getBeginPos() - end.getPos();
            String rightGene = String.format("%s[%s]",tmodelRight.getGeneSymbol(), tmodelRight.getGeneID());
            return new VcfOverlap(VcfOverlapType.INTERGENIC, leftDistance, leftGene, rightDistance, rightGene);
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
        return new VcfOverlap(VcfOverlapType.UNKNOWN, NOT_APPLICABLE, N_A, NOT_APPLICABLE, N_A);
    }


    @Override
    public String toString() {
        if (overlapType.equals(VcfOverlapType.UNKNOWN)) {
            return "[ERROR] Could not determine overlap.";
        }
        return String.format("VcfOverlap [%s] 5': %s (%dbp); 3': %s(%dbp)", overlapType, leftString, leftDistance, rightString, rightDistance);
    }
}
