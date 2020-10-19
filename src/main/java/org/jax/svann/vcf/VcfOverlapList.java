package org.jax.svann.vcf;

import de.charite.compbio.jannovar.impl.intervals.IntervalArray;
import de.charite.compbio.jannovar.reference.GenomeInterval;
import de.charite.compbio.jannovar.reference.GenomePosition;
import de.charite.compbio.jannovar.reference.Strand;
import de.charite.compbio.jannovar.reference.TranscriptModel;
import org.jax.svann.except.SvAnnRuntimeException;

import java.util.*;

import static org.jax.svann.vcf.IntronOverlap.getIntronNumber;
import static org.jax.svann.vcf.VcfOverlapType.*;

public class VcfOverlapList {

    private final static int NOT_APPLICABLE = -1;

    private final static int UNINITIALIZED_EXON = -1;

    private final static String N_A = "n/a";


    private List<VcfOverlap> overlaps;

    public VcfOverlapList(List<VcfOverlap> overlaps) {
        if (overlaps.isEmpty()) {
            throw new SvAnnRuntimeException("Empty overlap list");
        }
        this.overlaps = List.copyOf(overlaps);
    }


    /**
     * Calculate the overlap type
     *
     * @param start start position of SV, assumed to be on the same chromosome (asssumed to be POS strand)
     * @param end   end position of SV, assumed to be on the same chromosome (asssumed to be POS strand)
     * @param tmod  an overlapping Jannovar TranscriptModel (already identified to overlap somehow with the SV)
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
        return new VcfOverlap(MULTIPLE_EXON_CODING_TRANSCRIPT, leftDistance, rightDistance, desc);

    }


    static private VcfOverlapList intergenic(GenomePosition start,
                                             GenomePosition end,
                                             IntervalArray<TranscriptModel>.QueryResult qresult) {
        List<VcfOverlap> overlaps = new ArrayList<>();
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
                overlaps.add(new VcfOverlap(VcfOverlapType.UPSTREAM_GENE_VARIANT_2KB, leftDistance, rightDistance, leftGene));
            } else if (minDistance <= 5_000) {
                overlaps.add(new VcfOverlap(VcfOverlapType.UPSTREAM_GENE_VARIANT_5KB, leftDistance, rightDistance, leftGene));
            } else if (minDistance <= 500_000) {
                overlaps.add(new VcfOverlap(VcfOverlapType.UPSTREAM_GENE_VARIANT_500KB, leftDistance, rightDistance, leftGene));
            } else {
                overlaps.add(new VcfOverlap(VcfOverlapType.UPSTREAM_GENE_VARIANT, leftDistance, rightDistance, leftGene));
            }
        } else {
            if (minDistance <= 2_000) {
                overlaps.add(new VcfOverlap(VcfOverlapType.DOWNSTREAM_GENE_VARIANT_2KB, leftDistance, rightDistance, leftGene));
            } else if (minDistance <= 5_000) {
                overlaps.add(new VcfOverlap(VcfOverlapType.DOWNSTREAM_GENE_VARIANT_5KB, leftDistance, rightDistance, leftGene));
            } else if (minDistance <= 500_000) {
                overlaps.add(new VcfOverlap(VcfOverlapType.DOWNSTREAM_GENE_VARIANT_500KB, leftDistance, rightDistance, leftGene));
            } else {
                overlaps.add(new VcfOverlap(VcfOverlapType.DOWNSTREAM_GENE_VARIANT, leftDistance, rightDistance, leftGene));
            }
        }
        return new VcfOverlapList(overlaps);
    }

    static boolean overlapsExon(TranscriptModel tmod, GenomePosition start, GenomePosition end) {
        List<GenomeInterval> exons = tmod.getExonRegions();
        if (exons.stream().anyMatch(gi -> gi.contains(start))) {
            return true;
        } else if (!start.equals(end) && exons.stream().anyMatch(gi -> gi.contains(end))) {
            return true;
        } else {
            return false;
        }
    }

    static private Optional<Integer> getExonNumber(TranscriptModel tmod, GenomePosition pos) {
        int i = 0;

        for (var ex : tmod.getExonRegions()) {
            i += 1;
            if (ex.contains(pos))
                return Optional.of(i);
        }
        // we can get here if one end of the SV is exonic and the other is not
        // because we can for start and end separately. TODO MAKE MORE ROBUST
        return Optional.empty();
    }


    /**
     * Calculate overlap for a non-coding transcript. By assumption, if we get here then we have already
     * determined that the SV overlaps with a non-coding transcript
     *
     * @param tmod  a non-coding transcript (this is checked by calling code)
     * @return
     */
    static public VcfOverlap coding(TranscriptModel tmod, GenomePosition start , GenomePosition end) {
        boolean exonic = overlapsExon(tmod, start, end);
        if (exonic) {
            // determine which exons are affected
            Optional<Integer> firstAffectedExonNumber = getExonNumber(tmod, start);
            Optional<Integer> secondAffectedExonNumber = getExonNumber(tmod, end);
            if (firstAffectedExonNumber.isEmpty() && secondAffectedExonNumber.isEmpty()) {
                throw new SvAnnRuntimeException("Both positions empty");
            } else if (firstAffectedExonNumber.isEmpty()) {
                firstAffectedExonNumber = secondAffectedExonNumber;
            } else {
                secondAffectedExonNumber = firstAffectedExonNumber;
            }

            if (firstAffectedExonNumber.get().equals(secondAffectedExonNumber.get())){
                String msg = String.format("%s/%s[exon %d]", tmod.getGeneSymbol(), tmod.getAccession(), firstAffectedExonNumber.get());
                return new VcfOverlap(SINGLE_EXON_CODING_TRANSCRIPT, 0, 0, msg);
            } else {
                String msg = String.format("%s/%s[exon %d-%d]",
                        tmod.getGeneSymbol(),
                        tmod.getAccession(),
                        firstAffectedExonNumber.get(),
                        secondAffectedExonNumber.get());
                return new VcfOverlap(MULTIPLE_EXON_CODING_TRANSCRIPT, 0, 0, msg);
            }
        } else {
            // if we get here, then both positions must be in the same intron
            int intronNum = getIntronNumber(tmod, start.getPos(), end.getPos());
            String msg = String.format("%s/%s[intron %d]", tmod.getGeneSymbol(), tmod.getAccession(), intronNum);
            return new VcfOverlap(INTRONIC_CODING_TRANSCRIPT, 0, 0, msg);
        }
    }

    /**
     * Calculate overlap for a non-coding transcript. By assumption, if we get here then we have already
     * determined that the SV overlaps with a non-coding transcript
     *
     * @param tmod  a non-coding transcript (this is checked by calling code)
     * @param start start position of the overlapping structural variant
     * @param end   end position of the overlapping structural variant
     * @return
     */
    static public VcfOverlap noncoding(TranscriptModel tmod, GenomePosition start, GenomePosition end) {
        boolean exonic = overlapsExon(tmod, start, end);
        if (exonic) {
            // determine which exons are affected
            Optional<Integer> firstAffectedExonNumber = getExonNumber(tmod, start);
            Optional<Integer> secondAffectedExonNumber = getExonNumber(tmod, end);
            if (firstAffectedExonNumber.isEmpty() && secondAffectedExonNumber.isEmpty()) {
                throw new SvAnnRuntimeException("Both positions empty");
            } else if (firstAffectedExonNumber.isEmpty()) {
                firstAffectedExonNumber = secondAffectedExonNumber;
            } else {
                secondAffectedExonNumber = firstAffectedExonNumber;
            }
            if (firstAffectedExonNumber.get() == secondAffectedExonNumber.get()) {
                String msg = String.format("%s/%s[exon %d]",
                        tmod.getGeneSymbol(),
                        tmod.getAccession(),
                        firstAffectedExonNumber.get());
                return new VcfOverlap(SINGLE_EXON_NONCODING_TRANSCRIPT, 0, 0, msg);
            } else {
                String msg = String.format("%s/%s[exon %d-%d]",
                        tmod.getGeneSymbol(),
                        tmod.getAccession(),
                        firstAffectedExonNumber.get(),
                        secondAffectedExonNumber.get());
                return new VcfOverlap(MULTIPLE_EXON_NONCODING_TRANSCRIPT, 0, 0, msg);
            }
        } else {
            // if we get here, then both positions must be in the same intron
            int intronNum = getIntronNumber(tmod, start.getPos(), end.getPos());
            String msg = String.format("%s/%s[intron %d]", tmod.getGeneSymbol(), tmod.getAccession(), intronNum);
            return new VcfOverlap(INTRONIC_NONCODING_TRANSCRIPT, 0, 0, msg);
        }
    }

    static private VcfOverlap containedIn(TranscriptModel tmod, GenomePosition start, GenomePosition end) {
        int left = start.getPos() - tmod.getTXRegion().getBeginPos();
        int right = end.getPos() - tmod.getTXRegion().getEndPos();
        String msg = String.format("%s/%s", tmod.getGeneSymbol(), tmod.getAccession());
        return new VcfOverlap(TRANSCRIPT_CONTAINED_IN_SV, left, right, msg);
    }


    static public VcfOverlapList factory(GenomeInterval svInt,
                                         IntervalArray<TranscriptModel>.QueryResult qresult) {
        GenomePosition start = svInt.getGenomeBeginPos();
        GenomePosition end = svInt.getGenomeEndPos();
        List<VcfOverlap> overlaps = new ArrayList<>();
        if (qresult.getEntries().isEmpty()) {
            return intergenic(start, end, qresult);
        }
        // if we get here, then we overlap with one or more genes
        List<TranscriptModel> overlappingTranscripts = qresult.getEntries();
        for (var tmod : overlappingTranscripts) {
            if (svInt.contains(tmod.getTXRegion())) {
                // the transcript is completely contained in the SV
                VcfOverlap vover = containedIn(tmod, start, end);
                overlaps.add(vover);
                continue;
            }
            if (! tmod.getTXRegion().contains(start) && ! tmod.getTXRegion().contains(end)) {
                System.err.printf("[ERROR] Warning, transcript model (%s;%s) retrieved that does not overlap (chr%s:%d-%d): ",
                        tmod.getGeneSymbol(), tmod.getAccession(), start.getChr(), start.getPos(), end.getPos());
                //throw new L2ORuntimeException(tmod.getGeneSymbol());
            }
            if (tmod.isCoding()) {
                VcfOverlap voverlap = coding(tmod, start, end);
                overlaps.add(voverlap);
            } else {
                VcfOverlap voverlap = noncoding(tmod, start, end);
                overlaps.add(voverlap);
            }
        }
        if (overlaps.isEmpty()) {
            System.out.println("QOVER" + qresult);
            throw new SvAnnRuntimeException("Empty overlap list");
        }
        return new VcfOverlapList(overlaps);
    }

    public boolean isCoding() {
        return this.overlaps.stream().anyMatch(VcfOverlap::isCoding);
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
        sb.append("[VcfOverlapList]\n");
        for (var v : overlaps) {
            sb.append(v).append("\n");
        }
        return sb.toString();
    }
}
