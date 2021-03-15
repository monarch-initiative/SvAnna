package org.jax.svanna.core.hpo;

import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.Objects;

public class GeneWithId {
    private final TermId geneId;
    private final String symbol;

    private GeneWithId(String symbol, TermId tid) {
        this.geneId = tid;
        this.symbol = symbol;
    }

    public static GeneWithId of(String symbol, TermId tid) {
        return new GeneWithId(symbol, tid);
    }

    public TermId getGeneId() {
        return geneId;
    }

    public String getSymbol() {
        return symbol;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GeneWithId that = (GeneWithId) o;
        return Objects.equals(geneId, that.geneId) && Objects.equals(symbol, that.symbol);
    }

    @Override
    public int hashCode() {
        return Objects.hash(geneId, symbol);
    }

    @Override
    public String toString() {
        return "GeneWithId{" +
                "geneId=" + geneId +
                ", symbol='" + symbol + '\'' +
                '}';
    }
}
