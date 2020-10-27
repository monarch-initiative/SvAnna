package org.jax.svann.parse;

import org.jax.svann.reference.Position;
import org.jax.svann.reference.genome.Contig;

/**
 * Class represents the most basic data that is extracted from a VCF line.
 */
class CoreData {

    private final Contig contig;
    private final Position begin, end;

    CoreData(Contig contig, Position begin, Position end) {
        this.contig = contig;
        this.begin = begin;
        this.end = end;
    }

    /**
     * @return contig corresponding to content of the 1st VCF column
     */
    public Contig getContig() {
        return contig;
    }

    /**
     * @return position (+confidence interval) associated with POS - 2nd VCF column
     */
    public Position getBegin() {
        return begin;
    }

    /**
     * @return position (+confidence interval) associated with `END` INFO field
     */
    public Position getEnd() {
        return end;
    }

}
