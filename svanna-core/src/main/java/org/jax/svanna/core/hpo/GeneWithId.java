package org.jax.svanna.core.hpo;

import org.monarchinitiative.phenol.ontology.data.TermId;

public class GeneWithId {
    private final TermId geneId;
    private final String symbol;

    public GeneWithId(String symbol, TermId tid) {
        this.geneId = tid;
        this.symbol = symbol;
    }

    public TermId getGeneId() {
        return geneId;
    }

    public String getSymbol() {
        return symbol;
    }
}
