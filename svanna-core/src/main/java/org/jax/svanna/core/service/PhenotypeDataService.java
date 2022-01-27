package org.jax.svanna.core.service;

import org.jax.svanna.model.HpoDiseaseSummary;
import org.monarchinitiative.phenol.ontology.algo.OntologyAlgorithm;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.Term;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.ielis.silent.genes.model.GeneIdentifier;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface PhenotypeDataService {

    Logger LOGGER = LoggerFactory.getLogger(PhenotypeDataService.class);

    Ontology ontology();

    Stream<GeneIdentifier> geneWithIds();

    List<HpoDiseaseSummary> getDiseasesForGene(String hgncId);

    List<TermId> phenotypicAbnormalitiesForDiseaseId(String diseaseId);

    Set<Term> getTopLevelTerms(Collection<Term> hpoTermIds);

    // --------------------------------- DERIVED METHODS ---------------------------------------------------------------

    default Map<String, List<GeneIdentifier>> geneByHgvsSymbol() {
        return geneWithIds()
                .collect(Collectors.groupingBy(GeneIdentifier::symbol, Collectors.toUnmodifiableList()));
    }

    /**
     * Validate the input hpo terms and return a subset with the valid terms.
     *
     * @param hpoTermIds input HPO terms
     * @return subset with the valid terms
     */
    default Set<Term> validateTerms(Collection<TermId> hpoTermIds) {
        return hpoTermIds.stream()
                .filter(validateTerm())
                .map(termId -> ontology().getTermMap().get(termId))
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Get set of ancestors of the {@code candidates} terms (including the candidate terms) and retain the intersection
     * of ancestors and {@code ancestorTerms}.
     *
     * @param candidates    candidate terms
     * @param ancestorTerms ancestor terms to be used to filter ancestors of {@code candidates}
     * @return set {@code candidates} ancestors that are in {#code ancestorTerms}
     */
    default Set<TermId> getRelevantAncestors(Collection<TermId> candidates, Collection<TermId> ancestorTerms) {
        Set<TermId> candidateAncestors = OntologyAlgorithm.getAncestorTerms(ontology(), Set.copyOf(candidates), true);
        return candidateAncestors.stream()
                .filter(ancestorTerms::contains)
                .collect(Collectors.toSet());
    }

    private Predicate<? super TermId> validateTerm() {
        return termId -> {
            if (!ontology().getTermMap().containsKey(termId)) {
                LOGGER.warn("Term ID `{}` is not present in the used ontology", termId);
                return false;
            }
            return true;
        };
    }
}
