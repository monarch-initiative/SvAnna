package org.jax.svann.reference;

public interface Breakend extends ChromosomalRegion, GenomicPosition {

    /**
     * @return id corresponding to id of the record (e.g. VCF) this breakend was created from
     */
    String getId();

    /**
     * @return ref allele string
     */
    String getRef();

    @Override
    Breakend withStrand(Strand strand);

    /**
     * Convert the breakend to opposite strand no matter what.
     */
    @Override
    default Breakend toOppositeStrand() {
        return getStrand().equals(Strand.FWD)
                ? withStrand(Strand.REV)
                : withStrand(Strand.FWD);
    }
}
