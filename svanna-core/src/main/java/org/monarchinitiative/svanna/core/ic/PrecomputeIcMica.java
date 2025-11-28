package org.monarchinitiative.svanna.core.ic;

import org.monarchinitiative.phenol.annotations.constants.hpo.HpoSubOntologyRootTermIds;
import org.monarchinitiative.phenol.annotations.formats.hpo.AnnotatedItem;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.ontology.data.Identified;
import org.monarchinitiative.phenol.ontology.data.MinimalOntology;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.phenol.ontology.similarity.HpoResnikSimilarityPrecompute;
import org.monarchinitiative.phenol.ontology.similarity.TermPair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Precompute map with information content of the most informative common ancestor for HPO term pairs.
 */
public class PrecomputeIcMica {

    private static final Logger LOGGER = LoggerFactory.getLogger(PrecomputeIcMica.class);

    private PrecomputeIcMica() {
        // static utility class
    }

    public static Map<TermPair, Double> precomputeIcMicaMap(MinimalOntology hpo, HpoDiseases diseases, boolean assumeAnnotated) {
        Map<TermId, Double> termToIc = calculateMica(hpo, diseases, assumeAnnotated);
        return HpoResnikSimilarityPrecompute.precomputeSimilaritiesForTermPairs(hpo, termToIc);
    }

    private static Map<TermId, Double> calculateMica(MinimalOntology hpo, HpoDiseases diseases, boolean assumeAnnotated) {
        Instant start = Instant.now();
        Map<TermId, Integer> phenotypeIdToDiseaseIds = new HashMap<>();
        Map<TermId, Collection<TermId>> diseaseIdToTermIds = new HashMap<>();

        Set<TermId> termsAndAncestorsBuilder = new HashSet<>();
        for (AnnotatedItem disease : diseases) {
            for (Identified annotation : disease.annotations()) {
                hpo.graph().extendWithAncestors(annotation.id(), true, termsAndAncestorsBuilder);
            }

            for (TermId tid : termsAndAncestorsBuilder) {
                // We can't do this within the hot loop above because there is no guarantee of uniqueness of the ancestors.
                phenotypeIdToDiseaseIds.compute(tid, (key, val) -> val == null ? 0 : val + 1);
                diseaseIdToTermIds.computeIfAbsent(disease.id(), key -> new HashSet<>()).add(tid); // Note that this MUST be a Set
            }
            termsAndAncestorsBuilder.clear();
        }

        Map<TermId, Double> termToIc = new HashMap<>();
        double totalPopulationHpoTerms = phenotypeIdToDiseaseIds.get(HpoSubOntologyRootTermIds.PHENOTYPIC_ABNORMALITY);
        for (Map.Entry<TermId, Integer> e : phenotypeIdToDiseaseIds.entrySet()) {
            int annotatedCount = assumeAnnotated ? Math.max(e.getValue(), 1) : e.getValue();
            double ic = Math.log(totalPopulationHpoTerms / annotatedCount);
            termToIc.put(e.getKey(), ic);
        }

        Duration duration = Duration.between(start, Instant.now());
        long seconds = duration.toMillis() / 1000;
        double ms = (double) duration.toMillis() % 1000;
        LOGGER.debug("Calculated information content in {}s {}us", seconds, ms);

        return termToIc;
    }
}
