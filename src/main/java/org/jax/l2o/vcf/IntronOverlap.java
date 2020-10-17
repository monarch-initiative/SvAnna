package org.jax.l2o.vcf;

import de.charite.compbio.jannovar.reference.GenomeInterval;
import de.charite.compbio.jannovar.reference.GenomePosition;
import de.charite.compbio.jannovar.reference.Strand;
import de.charite.compbio.jannovar.reference.TranscriptModel;
import org.jax.l2o.except.L2ORuntimeException;

import java.util.List;

/**
 * This class offers a static method designed to calculate the intron number affected by a given variant.
 */
public class IntronOverlap {

    private IntronOverlap() {
        // do not allow instantiation
    }

    /**
     * By assumption, if we get here we have previously determined that the SV is located within an intron
     * of the transcript.
     *
     * @param tmod
     * @return
     */
    static public int getIntronNumber(TranscriptModel tmod, int startPos, int endPos) {

        List<GenomeInterval> exons = tmod.getExonRegions();
        if (tmod.getStrand().equals(Strand.FWD)) {
            for (int i=0;i<exons.size()-1;i++) {
                if (startPos > exons.get(i).getEndPos() && startPos < exons.get(i+1).getBeginPos()) {
                    return i+1;
                }
            }
        } else {
            // reverse order for neg-strand genes.
            for (int i=0;i<exons.size()-1;i++) {
                if (startPos < exons.get(i).getEndPos() && startPos > exons.get(i+1).getBeginPos()) {
                    return i+1;
                }
            }
        }
        // rarely, only one of the two ends of the SV is in an intron.
        // This can be the case of the SV overlaps exon 1, for instance, starting 5' to the transcript and
        // ending in intron 1
        if (tmod.getStrand().equals(Strand.FWD)) {
            for (int i=0;i<exons.size()-1;i++) {
                if (endPos > exons.get(i).getEndPos() && endPos < exons.get(i+1).getBeginPos()) {
                    return i+1;
                }
            }
        } else {
            // reverse order for neg-strand genes.
            for (int i=0;i<exons.size()-1;i++) {
                if (endPos < exons.get(i).getEndPos() && endPos > exons.get(i+1).getBeginPos()) {
                    return i+1;
                }
            }
        }
        throw new L2ORuntimeException("Could not find intron number");
    }

}
