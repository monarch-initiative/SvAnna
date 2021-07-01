package org.jax.svanna.cli.writer;

import org.jax.svanna.core.reference.SvannaVariant;
import org.monarchinitiative.phenol.ontology.data.Term;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public class AnalysisResults {

    private final String variantSource;

    private final Set<Term> probandPhenotypeTerms;

    private final Set<Term> topLevelPhenotypeTerms;

    private final List<? extends SvannaVariant> variants;

    public AnalysisResults(String variantSource,
                           Set<Term> probandPhenotypeTerms,
                           Set<Term> topLevelPhenotypeTerms,
                           List<? extends SvannaVariant> variants) {
        this.variantSource = Objects.requireNonNull(variantSource, "Variant source cannot be null");
        this.probandPhenotypeTerms = Objects.requireNonNull(probandPhenotypeTerms, "Phenotype terms cannot be null");
        this.topLevelPhenotypeTerms = Objects.requireNonNull(topLevelPhenotypeTerms, "Top level phenotype terms cannot be null");
        this.variants = Objects.requireNonNull(variants, "Variants cannot be null");
    }

    public Set<Term> probandPhenotypeTerms() {
        return probandPhenotypeTerms;
    }

    public Set<Term> topLevelPhenotypeTerms() {
        return topLevelPhenotypeTerms;
    }

    public List<? extends SvannaVariant> variants() {
        return variants;
    }


    public String variantSource() {
        return variantSource;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AnalysisResults that = (AnalysisResults) o;
        return Objects.equals(variantSource, that.variantSource) && Objects.equals(probandPhenotypeTerms, that.probandPhenotypeTerms) && Objects.equals(topLevelPhenotypeTerms, that.topLevelPhenotypeTerms) && Objects.equals(variants, that.variants);
    }

    @Override
    public int hashCode() {
        return Objects.hash(variantSource, probandPhenotypeTerms, topLevelPhenotypeTerms, variants);
    }
}
