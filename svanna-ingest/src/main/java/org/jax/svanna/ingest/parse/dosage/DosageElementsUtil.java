package org.jax.svanna.ingest.parse.dosage;

import org.jax.svanna.model.landscape.dosage.DosageElement;
import org.jax.svanna.model.landscape.dosage.DosageSensitivity;
import org.jax.svanna.model.landscape.dosage.DosageSensitivityEvidence;
import org.monarchinitiative.svart.Contig;
import org.monarchinitiative.svart.Coordinates;
import org.monarchinitiative.svart.Strand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;

class DosageElementsUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(DosageElementsUtil.class);

    private DosageElementsUtil() {
    }


    static Collection<DosageElement> makeDosageElements(Contig contig,
                                                        Strand strand,
                                                        Coordinates coordinates,
                                                        String id,
                                                        String haploinsufficiency,
                                                        String triplosensitivity) {
        DosageSensitivity haploinsufficiencySensitivity = parseHaploinsufficiency(haploinsufficiency);
        DosageSensitivityEvidence haploinsufficiencyEvidence = parseEvidence(haploinsufficiency);
        DosageElement haploDosage = DosageElement.of(contig,
                strand,
                coordinates,
                id,
                haploinsufficiencySensitivity,
                haploinsufficiencyEvidence);

        DosageSensitivity triplosensitivitySensitivity = parseTriplosensitivity(triplosensitivity);
        DosageSensitivityEvidence triplosensitivityEvidence = parseEvidence(triplosensitivity);
        DosageElement triploDosage = DosageElement.of(contig,
                strand,
                coordinates,
                id,
                triplosensitivitySensitivity,
                triplosensitivityEvidence);

        if (DosageSensitivity.HAPLOINSUFFICIENCY.equals(haploinsufficiencySensitivity)) {
            if (DosageSensitivity.TRIPLOSENSITIVITY.equals(triplosensitivitySensitivity)) {
                return List.of(haploDosage, triploDosage);
            }
            return List.of(haploDosage);
        } else {
            if (DosageSensitivity.TRIPLOSENSITIVITY.equals(triplosensitivitySensitivity)) {
                return List.of(triploDosage);
            }
            return List.of();
        }
    }


    private static DosageSensitivity parseHaploinsufficiency(String payload) {
        switch (payload.toUpperCase()) {
            case "0":
            case "NOT YET EVALUATED":
            case "40":
                return DosageSensitivity.NONE;
            case "1":
            case "2":
            case "3":
            case "30":
                return DosageSensitivity.HAPLOINSUFFICIENCY;
            default:
                LOGGER.warn("Unknown evidence level: `{}`", payload);
                return DosageSensitivity.NONE;
        }
    }

    private static DosageSensitivity parseTriplosensitivity(String payload) {
        switch (payload.toUpperCase()) {
            case "0":
            case "NOT YET EVALUATED":
            case "30":
            case "40":
                return DosageSensitivity.NONE;
            case "1":
            case "2":
            case "3":
                return DosageSensitivity.TRIPLOSENSITIVITY;
            default:
                LOGGER.warn("Unknown evidence level: `{}`", payload);
                return DosageSensitivity.NONE;
        }
    }

    private static DosageSensitivityEvidence parseEvidence(String payload) {
        switch (payload.toUpperCase()) {
            case "0":
            case "NOT YET EVALUATED":
                return DosageSensitivityEvidence.NO_EVIDENCE;
            case "1":
                return DosageSensitivityEvidence.LITTLE_EVIDENCE;
            case "2":
                return DosageSensitivityEvidence.SOME_EVIDENCE;
            case "3":
            case "30":
            case "40":
                return DosageSensitivityEvidence.SUFFICIENT_EVIDENCE;
            default:
                LOGGER.warn("Unknown evidence level: `{}`", payload);
                return DosageSensitivityEvidence.NO_EVIDENCE;
        }
    }
}
