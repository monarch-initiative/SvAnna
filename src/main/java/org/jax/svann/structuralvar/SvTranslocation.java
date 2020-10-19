package org.jax.svann.structuralvar;

public class SvTranslocation extends SvAnn {

    private final String contigB;
    private final String mate_b_id;
    public SvTranslocation(String mateAid, String mateBid, String contigA, String contigB, int start, int end) {
        super(SvType.DELETION_SIMPLE, mateAid, contigA, start, end);
        this.contigB = contigB;
        this.mate_b_id = mateBid;
    }

    public String getContigB() {
        throw new UnsupportedOperationException("CNVs do not have a chromosome B");
    }
}
