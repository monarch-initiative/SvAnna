package org.jax.svann.structuralvar;

public class SvDeletion extends SvAnn {



    public SvDeletion(String id, String contig, int start, int end) {
        super(SvType.DELETION, id, contig, start, end);
    }

    public String getContigB() {
        throw new UnsupportedOperationException("CNVs do not have a chromosome B");
    }
}
