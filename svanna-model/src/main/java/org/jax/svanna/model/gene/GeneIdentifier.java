package org.jax.svanna.model.gene;

import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.Objects;

@Deprecated
public class GeneIdentifier {
    private final TermId accessionId;
    private final String symbol;

    private GeneIdentifier(TermId accessionId, String symbol) {
        this.accessionId = accessionId;
        this.symbol = symbol;
    }

    public static GeneIdentifier of(String symbol, TermId accessionId) {
        return new GeneIdentifier(accessionId, symbol);
    }

    public TermId accessionId() {
        return accessionId;
    }

    public String symbol() {
        return symbol;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GeneIdentifier that = (GeneIdentifier) o;
        return Objects.equals(accessionId, that.accessionId) && Objects.equals(symbol, that.symbol);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accessionId, symbol);
    }

    @Override
    public String toString() {
        return "GeneIdentifier{" +
                "geneId=" + accessionId +
                ", symbol='" + symbol + '\'' +
                '}';
    }
}
