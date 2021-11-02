package org.jax.svanna.core.overlap;

import org.jax.svanna.core.LogUtils;
import org.jax.svanna.core.reference.Transcript;
import org.monarchinitiative.svart.CoordinateSystem;
import org.monarchinitiative.svart.Coordinates;
import org.monarchinitiative.svart.GenomicRegion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

class Utils {

    private static final Logger LOGGER = LoggerFactory.getLogger(Utils.class);

    /**
     * We check for overlap in three ways. The structural variant has an interval (b,e). b or e can
     * be contained within an exon. Alternatively, the entire exon can be contained within (b,e). Note
     * that if we call this method then
     *
     * @param event a structural variant interval
     * @param tx    A transcript
     * @return object representing the number of the first and last affected exon
     */
    static ExonPair getAffectedExons(GenomicRegion event, Transcript tx) {
        event = event.withStrand(tx.strand());
        List<Coordinates> exons = tx.exons();
        boolean[] affected = new boolean[exons.size()]; // initializes to false
        for (int i = 0; i < exons.size(); i++) {
            Coordinates exon = exons.get(i);
            if (exon.overlaps(event.coordinates())) {
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
     * By assumption, if we get here we have previously determined that the SV is located within an intron
     * of the transcript. If the method is called for an SV that is only partially in the intron, it can
     * return incorrect results. It does not check this.
     *
     * @param tx transcript
     * @return intron distance summary
     */
    static IntronDistance getIntronNumber(GenomicRegion region, Transcript tx) {
        // we use zero based coordinates for calculations
        int variantStart = region.startOnStrandWithCoordinateSystem(tx.strand(), CoordinateSystem.zeroBased());
        int variantEnd = region.endOnStrandWithCoordinateSystem(tx.strand(), CoordinateSystem.zeroBased());
        List<Coordinates> exons = tx.exons();

        for (int i = 0; i < exons.size() - 1; i++) {
            // current exon end
            int intronStart = exons.get(i).endWithCoordinateSystem(CoordinateSystem.zeroBased());
            // next exon start
            int intronEnd = exons.get(i + 1).startWithCoordinateSystem(CoordinateSystem.zeroBased());

            if (intronStart <= variantStart && variantEnd <= intronEnd) {
                // we start the intron numbering at 1
                int intronNumber = i + 1;
                int up = variantStart - intronStart;
                int down = intronEnd - variantEnd;
                return new IntronDistance(intronNumber, up, down);
            }

        }
        LogUtils.logWarn(LOGGER, "Could not find intron number");
        return IntronDistance.empty();
    }
}
