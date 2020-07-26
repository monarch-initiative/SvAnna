package org.jax.l2o.lirical;

import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.Collection;
import java.util.Set;

/**
 * Objects of this class represent one above threshold candidate
 */
public class LiricalHit {

    private final String diseaseName;
    private final TermId diseaseCurie;
    private final double posttestProbability;
    private final double likelihoodRatio;
    private Set<String> geneSymbols = Set.of();



    public LiricalHit(String dname, String dcurie, double prob, double lr){
        this.diseaseName = dname;
        this.diseaseCurie = TermId.of(dcurie);
        this.posttestProbability = prob;
        this.likelihoodRatio = lr;
    }

    public void setGeneSymbols(Set<String> termIds) {
        geneSymbols = termIds;
    }

    public String getDiseaseName() {
        return diseaseName;
    }

    public TermId getDiseaseCurie() {
        return diseaseCurie;
    }

    public double getPosttestProbability() {
        return posttestProbability;
    }

    public double getLikelihoodRatio() {
        return likelihoodRatio;
    }

    public Set<String> getGeneSymbols() {
        return geneSymbols;
    }
}
