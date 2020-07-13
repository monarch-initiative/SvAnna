package org.jax.l2o.lirical;

/**
 * Objects of this class represent one above threshold candidate
 */
public class LiricalHit {

    private final String diseaseName;
    private final TermId diseaseCurie;


    public LiricalHit(String dname, String dcurie, double prob, double lr){
        this.diseaseName = dname;
    }

}
