package org.jax.svann.structuralvar;

public class SvInsertion extends SvAnn{


    public SvInsertion(String id, String contig, int start, int end) {
        super(SvType.INSERTION, id, contig, start, end);
    }

    public String getContigB() {
        throw new UnsupportedOperationException("Insertions do not have a chromosome B");
    }
}
