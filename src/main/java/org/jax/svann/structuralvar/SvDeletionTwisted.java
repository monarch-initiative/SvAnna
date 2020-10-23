package org.jax.svann.structuralvar;

public class SvDeletionTwisted extends SvAnn {

    private final String contig_b_id;
    public SvDeletionTwisted(String mateAid, String mateBid, String contig, int start, int end) {
        super(SvType.DELETION_SIMPLE, mateAid, contig, start, end);
        this.contig_b_id = mateBid;
    }

    public String getContigB() {
        throw new UnsupportedOperationException("CNVs do not have a chromosome B");
    }
}