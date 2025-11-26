package org.monarchinitiative.svanna.core.service;

import org.monarchinitiative.phenol.ontology.data.MinimalOntology;
import org.monarchinitiative.svanna.model.HpoDiseaseSummary;
import org.monarchinitiative.phenol.ontology.data.Term;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public interface PhenotypeDataService {

    Logger LOGGER = LoggerFactory.getLogger(PhenotypeDataService.class);

    MinimalOntology ontology();

    List<HpoDiseaseSummary> getDiseasesForGene(String entrezId);

    List<TermId> phenotypicAbnormalitiesForDiseaseId(TermId diseaseId);

    Set<Term> getTopLevelTerms(Collection<Term> hpoTermIds);

    // --------------------------------- DERIVED METHODS ---------------------------------------------------------------

    default Collection<TermId> getDiseaseIdsForGene(String entrezId) {
        return getDiseasesForGene(entrezId).stream()
                .map(HpoDiseaseSummary::getDiseaseId)
                .collect(Collectors.toList());
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
                .flatMap(termId -> ontology().termForTermId(termId).stream())
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
        Set<TermId> relevant = new HashSet<>();
        for (TermId candidate : candidates) {
            if (ancestorTerms.contains(candidate))
                relevant.add(candidate);
            for (TermId ancestor : ontology().graph().getAncestors(candidate)) {
                if (ancestorTerms.contains(ancestor))
                    relevant.add(ancestor);
            }
        }
        return relevant;
    }

    private Predicate<? super TermId> validateTerm() {
        return termId -> {
            if (!ontology().containsTermId(termId)) {
                LOGGER.warn("Term ID `{}` is not present in the used ontology", termId);
                return false;
            }
            return true;
        };
    }
}
