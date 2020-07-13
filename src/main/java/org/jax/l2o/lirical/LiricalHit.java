package org.jax.l2o.lirical;

import org.monarchinitiative.phenol.ontology.data.TermId;

/**
 * Objects of this class represent one above threshold candidate
 */
public class LiricalHit {

    private final String diseaseName;
    private final TermId diseaseCurie;
    private final double posttestProbability;
    private final double likelihoodRatio;


    public LiricalHit(String dname, String dcurie, double prob, double lr){
        this.diseaseName = dname;
        this.diseaseCurie = TermId.of(dcurie);
        this.posttestProbability = prob;
        this.likelihoodRatio = lr;
    }

}
