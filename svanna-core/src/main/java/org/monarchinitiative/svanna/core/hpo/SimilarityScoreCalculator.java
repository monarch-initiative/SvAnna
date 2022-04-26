package org.monarchinitiative.svanna.core.hpo;

import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.Collection;

public interface SimilarityScoreCalculator {

    double computeSimilarityScore(Collection<TermId> query, Collection<TermId> target);
}
