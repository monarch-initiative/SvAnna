package org.jax.svann.reference;

public interface GenomePosition {

    Contig getContig();

    int getPos();

    Strand getStrand();

    GenomePosition withStrand(Strand strand);

    default ConfidenceInterval getCI() {
        return ConfidenceInterval.precise();
    }

}
