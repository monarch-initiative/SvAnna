package org.jax.svanna.core.hpo;

import org.monarchinitiative.phenol.ontology.data.TermId;

public interface TermSimilarityCalculator {

    double calculate(TermId a, TermId b);

}
