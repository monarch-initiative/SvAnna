package org.jax.svann.structuralvar;

import org.jax.svann.overlap.OverlapperOld;

/**
 * This is the superclass for all structural variant annotations. The goal of this class is to
 * provide a standardized way of interrogating different classes of SV that will make it easier for
 * us to display them and prioritize them. For instance, inversions will have different needs as compared
 * to deletions.
 * @author Peter N Robinson
 */
public abstract class SvAnn {

    /** The category of structural variant. */
    private final SvType svType;
    /** Position of the start of the SV, or of breakend A. */
    private final int startPos;
    /** Position of the end of the SV, or of breakend B. */
    private final int endPos;
    /** Name of the contig (usually Chromosome) of breakend A. For all but translocations, there
     * is only one contig, which is stored here.
     */
    private final String contigA;
    /** The identifier of this SV, corresponding to the ID column of the VCF file. */
    private final String id;
    private int svLen;

    private OverlapperOld vcfOverlapList = null;


    public SvAnn(SvType svtype, String svid, String contig, int start, int end) {
        this.svType = svtype;
        this.id = svid;
        this.contigA = contig;
        this.startPos = start;
        this.endPos = end;
    }

    public String getID() {
        return this.id;
    }

    public int getStartPos() {
        return startPos;
    }

    public String getContigA() {
        return contigA;
    }

    public SvType getSvType() {
        return svType;
    }


    public int getEndPos() {
        return endPos;
    }

    public void setVcfOverlapList(OverlapperOld vcfOverlapList) {
        this.vcfOverlapList = vcfOverlapList;
    }

    public abstract String getContigB();

    @Override
    public String toString() {
        return String.format("%s [%s] %s:%d-%d", this.svType, this.id, this.contigA, this.startPos, this.endPos);

    }





}
