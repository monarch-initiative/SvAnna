package org.jax.svanna.core.reference;

import org.monarchinitiative.phenol.ontology.data.Term;

import java.util.Objects;

public class EnhancerTissueSpecificity {

    private final Term tissue;
    private final Term hpo;
    private final double specificity;

    private EnhancerTissueSpecificity(Term tissue, Term hpo, double specificity) {
        this.tissue = Objects.requireNonNull(tissue);
        this.hpo = Objects.requireNonNull(hpo);
        this.specificity = specificity;
    }

    public static EnhancerTissueSpecificity of(Term tissueTerm, Term hpoTerm, double value) {
        return new EnhancerTissueSpecificity(tissueTerm, hpoTerm, value);
    }

    public double specificityValue() {
        return specificity;
    }

    public Term hpoTerm() {
        return hpo;
    }

    public Term tissueTerm() {
        return tissue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EnhancerTissueSpecificity that = (EnhancerTissueSpecificity) o;
        return Double.compare(that.specificity, specificity) == 0 && Objects.equals(tissue, that.tissue) && Objects.equals(hpo, that.hpo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tissue, hpo, specificity);
    }

    @Override
    public String toString() {
        return "TissueSpecificity{" +
                "tissueTerm=" + tissue +
                ", hpoTerm=" + hpo +
                ", specificityValue=" + specificity +
                '}';
    }
}
