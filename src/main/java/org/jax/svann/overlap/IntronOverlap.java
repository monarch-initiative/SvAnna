package org.jax.svann.overlap;

import de.charite.compbio.jannovar.reference.GenomeInterval;
import de.charite.compbio.jannovar.reference.Strand;
import de.charite.compbio.jannovar.reference.TranscriptModel;
import org.jax.svann.except.SvAnnRuntimeException;

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
     * of the transcript. If the method is called for an SV that is only partially in the intron, it can
     * return incorrect results. It does not check this.
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
                if (endPos > exons.get(i).getEndPos() && endPos < exons.get(i+1).getBeginPos()) {
                    return i+1;
                }
            }
        } else {
            // reverse order for neg-strand genes.
            for (int i=0;i<exons.size()-1;i++) {
                if (startPos < exons.get(i).withStrand(Strand.FWD).getEndPos() && startPos > exons.get(i+1).withStrand(Strand.FWD).getBeginPos()) {
                    return i+1;
                }
                if (endPos < exons.get(i).withStrand(Strand.FWD).getEndPos() && endPos > exons.get(i+1).withStrand(Strand.FWD).getBeginPos()) {
                    return i+1;
                }
            }
        }
        throw new SvAnnRuntimeException("Could not find intron number");
    }

}
