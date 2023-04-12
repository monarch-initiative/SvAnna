package org.monarchinitiative.svanna.ingest.similarity;

import org.monarchinitiative.svanna.core.LogUtils;
import org.monarchinitiative.svanna.core.hpo.TermPair;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseaseAnnotation;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.phenol.ontology.data.TermIds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Precompute information content (IC) of most informative common ancestors (MICA) for term pairs.
 */
public class IcMicaCalculator {

    private static final Logger LOGGER = LoggerFactory.getLogger(IcMicaCalculator.class);

    private static final TermId[] TOP_LEVEL_TERMS = toplevelTerms();

    private IcMicaCalculator() {
    }

    public static Map<TermPair, Double> precomputeIcMicaValues(Ontology ontology,
                                                               HpoDiseases diseases) {
        LogUtils.logInfo(LOGGER, "Computing information contents of most informative common ancestor terms");
        Map<TermId, Collection<TermId>> diseaseIdToTermIds = new HashMap<>();
        Map<TermId, Collection<TermId>> termIdToDiseaseIds = new HashMap<>();
        for (HpoDisease disease : diseases) {
            TermId diseaseId = disease.id();
            diseaseIdToTermIds.putIfAbsent(diseaseId, new HashSet<>());

            // add term ancestors
            Set<TermId> hpoTerms = disease.presentAnnotationsStream()
                    .map(HpoDiseaseAnnotation::id)
                    .collect(Collectors.toSet());
            Set<TermId> inclAncestorTermIds = TermIds.augmentWithAncestors(ontology, hpoTerms, true);
            for (TermId tid : inclAncestorTermIds) {
                termIdToDiseaseIds.putIfAbsent(tid, new HashSet<>());
                termIdToDiseaseIds.get(tid).add(diseaseId);
                diseaseIdToTermIds.get(diseaseId).add(tid);
            }
        }

        Map<TermId, Double> termToIc = new HashMap<>();
        TermId ROOT_HPO = TermId.of("HP:0000118"); // Phenotypic abnormality
        int totalPopulationHpoTerms = termIdToDiseaseIds.get(ROOT_HPO).size();
        for (TermId tid : termIdToDiseaseIds.keySet()) {
            int annotatedCount = termIdToDiseaseIds.get(tid).size();
            double ic = -1 * Math.log((double) annotatedCount / totalPopulationHpoTerms);
            termToIc.put(tid, ic);
        }

        Map<TermPair, Double> termPairIcMicaMap = new HashMap<>();

        // Compute for relevant sub-ontologies in HPO
        for (TermId topTerm : TOP_LEVEL_TERMS) {
            if (!ontology.containsTerm(topTerm)) {
                continue; // should never happen, but avoid crash in testing.
            }
            Ontology subOntology = ontology.subOntology(topTerm);
            List<TermId> list = List.copyOf(subOntology.getNonObsoleteTermIds());
            for (int i = 0; i < list.size(); i++) {
                // start the second interaction at i to get self-similarity
                for (int j = i; j < list.size(); j++) {
                    TermId a = list.get(i);
                    TermId b = list.get(j);
                    double informationContent = computeIcMica(a, b, termToIc, subOntology);
                    TermPair termPair = TermPair.symmetric(a, b);
                    // a few terms belong to multiple sub-ontologies. This will take the maximum similarity.
                    double d = termPairIcMicaMap.getOrDefault(termPair, 0.0);
                    if (informationContent > d) {
                        termPairIcMicaMap.put(termPair, informationContent);
                    }
                }
            }
        }
        return termPairIcMicaMap;
    }


    private static TermId[] toplevelTerms() {
        TermId ABNORMAL_CELLULAR_ID = TermId.of("HP:0025354");
        TermId BLOOD_ID = TermId.of("HP:0001871");
        TermId CONNECTIVE_TISSUE_ID = TermId.of("HP:0003549");
        TermId HEAD_AND_NECK_ID = TermId.of("HP:0000152");
        TermId LIMBS_ID = TermId.of("HP:0040064");
        TermId METABOLISM_ID = TermId.of("HP:0001939");
        TermId PRENATAL_ID = TermId.of("HP:0001197");
        TermId BREAST_ID = TermId.of("HP:0000769");
        TermId CARDIOVASCULAR_ID = TermId.of("HP:0001626");
        TermId DIGESTIVE_ID = TermId.of("HP:0025031");
        TermId EAR_ID = TermId.of("HP:0000598");
        TermId ENDOCRINE_ID = TermId.of("HP:0000818");
        TermId EYE_ID = TermId.of("HP:0000478");
        TermId GENITOURINARY_ID = TermId.of("HP:0000119");
        TermId IMMUNOLOGY_ID = TermId.of("HP:0002715");
        TermId INTEGUMENT_ID = TermId.of("HP:0001574");
        TermId NERVOUS_SYSTEM_ID = TermId.of("HP:0000707");
        TermId RESPIRATORY_ID = TermId.of("HP:0002086");
        TermId MUSCULOSKELETAL_ID = TermId.of("HP:0033127");
        TermId THORACIC_CAVITY_ID = TermId.of("HP:0045027");
        TermId VOICE_ID = TermId.of("HP:0001608");
        TermId CONSTITUTIONAL_ID = TermId.of("HP:0025142");
        TermId GROWTH_ID = TermId.of("HP:0001507");
        TermId NEOPLASM_ID = TermId.of("HP:0002664");
        return new TermId[]{ABNORMAL_CELLULAR_ID, BLOOD_ID, CONNECTIVE_TISSUE_ID, HEAD_AND_NECK_ID,
                LIMBS_ID, METABOLISM_ID, PRENATAL_ID, BREAST_ID, CARDIOVASCULAR_ID, DIGESTIVE_ID,
                EAR_ID, ENDOCRINE_ID, EYE_ID, GENITOURINARY_ID, IMMUNOLOGY_ID, INTEGUMENT_ID,
                NERVOUS_SYSTEM_ID, RESPIRATORY_ID, MUSCULOSKELETAL_ID, THORACIC_CAVITY_ID,
                VOICE_ID, GROWTH_ID, CONSTITUTIONAL_ID, NEOPLASM_ID};
    }

    private static double computeIcMica(TermId a, TermId b, Map<TermId, Double> termToIc, Ontology ontology) {
        final Set<TermId> commonAncestors = ontology.getCommonAncestors(a, b);
        double maxValue = 0.0;
        for (TermId termId : commonAncestors) {
            maxValue = Double.max(maxValue, termToIc.getOrDefault(termId, 0.0));
        }
        return maxValue;
    }

}
