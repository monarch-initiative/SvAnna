package org.jax.svann.structuralvar;

public class SvInversion extends SvAnn {


    public SvInversion(String id, String contig, int start, int end) {
        super(SvType.INVERSION, id, contig, start, end);
    }

    public String getContigB() {
        throw new UnsupportedOperationException("Inversions do not have a chromosome B");
    }
}
