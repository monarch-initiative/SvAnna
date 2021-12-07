package org.jax.svanna.core.hpo;

import org.monarchinitiative.phenol.ontology.data.TermId;

/**
 * Most informative common ancestor calculator.
 */
public interface MicaCalculator {

    /**
     * @param a term
     * @param b term
     * @return information content of the most informative common ancestor of the terms <code>a</code> and <code>b</code>
     */
    double calculate(TermId a, TermId b);

}
