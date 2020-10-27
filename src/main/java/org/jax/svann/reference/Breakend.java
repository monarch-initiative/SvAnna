package org.jax.svann.reference;

public interface Breakend extends ChromosomalRegion {

    /**
     * @return id corresponding to id of the record (e.g. VCF) this breakend was created from
     */
    String getId();

    /**
     * @return ref allele string
     */
    String getRef();

    /**
     * @return alt allele sequence
     */
    String getInserted();

    @Override
    Breakend withStrand(Strand strand);
}
