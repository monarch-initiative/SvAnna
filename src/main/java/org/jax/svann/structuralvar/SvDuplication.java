package org.jax.svann.structuralvar;

public class SvDuplication extends SvAnn {


    public SvDuplication(String id, String contig, int start, int end) {
        super(SvType.DUPLICATION, id, contig, start, end);
    }

    public String getContigB() {
        throw new UnsupportedOperationException("Duplications do not have a chromosome B");
    }
}
