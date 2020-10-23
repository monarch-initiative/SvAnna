package org.jax.svann.structuralvar;

@Deprecated
public class SvCnv extends SvAnn {


    public SvCnv(String id, String contig, int start, int end) {
        super(SvType.CNV, id, contig, start, end);
    }

    public String getContigB() {
        throw new UnsupportedOperationException("CNVs do not have a chromosome B");
    }
}
