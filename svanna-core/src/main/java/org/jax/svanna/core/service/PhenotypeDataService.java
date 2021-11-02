package org.jax.svanna.core.service;

import org.jax.svanna.model.HpoDiseaseSummary;
import org.jax.svanna.model.gene.GeneIdentifier;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.Term;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public interface PhenotypeDataService {

    Ontology ontology();

    Set<GeneIdentifier> geneWithIds();

    default Map<String, GeneIdentifier> geneBySymbol() {
        return geneWithIds().stream().collect(Collectors.toUnmodifiableMap(GeneIdentifier::symbol, Function.identity()));
    }

    /**
     * Create a map with key=GeneId, value=Set of {@link HpoDiseaseSummary} objects corresponding to the gene.
     * We create entries for diseases associated with the target term ids.
     *
     * @param hpoTermIds HPO ids describing proband's phenotype
     * @return map with key=GeneId, value=Set of {@link HpoDiseaseSummary} objects
     */
    Map<TermId, Set<HpoDiseaseSummary>> getRelevantGenesAndDiseases(Collection<TermId> hpoTermIds);


    Set<HpoDiseaseSummary> getDiseasesForGene(String accession);

    default Set<HpoDiseaseSummary> getDiseasesForGene(TermId gene) {
        return getDiseasesForGene(gene.getValue());
    }

    Set<Term> getTopLevelTerms(Collection<Term> hpoTermIds);

    /**
     * Validate the input hpo terms and return a subset with the valid terms.
     *
     * @param hpoTermIds input HPO terms
     * @return subset with the valid terms
     */
    Set<Term> validateTerms(Collection<TermId> hpoTermIds);

    /**
     * Get set of ancestors of the {@code candidates} terms (including the candidate terms) and retain the intersection
     * of ancestors and {@code ancestorTerms}.
     *
     * @param candidates    candidate terms
     * @param ancestorTerms ancestor terms to be used to filter ancestors of {@code candidates}
     * @return set {@code candidates} ancestors that are in {#code ancestorTerms}
     */
    Set<TermId> getRelevantAncestors(Collection<TermId> candidates, Collection<TermId> ancestorTerms);

    double computeSimilarityScore(Collection<TermId> query, Collection<TermId> target);
}
