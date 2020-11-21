package org.jax.svann.parse;

import org.jax.svann.reference.ConfidenceInterval;
import org.jax.svann.reference.Zygosity;
import org.jax.svann.reference.genome.Contig;

/**
 * Class represents the most basic data that is extracted from a VCF line.
 */
class CoreData {

    private final Contig contig;
    private final int start;
    private final int end;
    private final ConfidenceInterval ciStart;
    private final ConfidenceInterval ciEnd;
    private final int depthOfCoverage;
    private final Zygosity zygosity;

    CoreData(Contig contig,
             int start,
             ConfidenceInterval ciStart,
             int end,
             ConfidenceInterval ciEnd,
             int depthOfCoverage,
             Zygosity zygosity) {
        this.contig = contig;
        this.start = start;
        this.ciStart = ciStart;
        this.end = end;
        this.ciEnd = ciEnd;
        this.depthOfCoverage = depthOfCoverage;
        this.zygosity = zygosity;
    }

    public int depthOfCoverage() {
        return depthOfCoverage;
    }

    public Zygosity zygosity() {
        return zygosity;
    }

    /**
     * @return contig corresponding to content of the 1st VCF column
     */
    public Contig getContig() {
        return contig;
    }

    /**
     * @return position  associated with POS - 2nd VCF column
     */
    public int getStart() {
        return start;
    }

    /**
     * @return confidence interval associated with POS - 2nd VCF column
     */
    public ConfidenceInterval getCiStart() {
        return ciStart;
    }

    /**
     * @return position associated with `END` INFO field
     */
    public int getEnd() {
        return end;
    }


    /**
     * @return position (+confidence interval) associated with `END` INFO field
     */
    public ConfidenceInterval getCiEnd() {
        return ciEnd;
    }

}
