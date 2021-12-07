package org.jax.svanna.ingest.parse.dosage;

import org.jax.svanna.model.landscape.dosage.*;
import org.monarchinitiative.svart.Contig;
import org.monarchinitiative.svart.Coordinates;
import org.monarchinitiative.svart.GenomicRegion;
import org.monarchinitiative.svart.Strand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

class DosageElementsUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(DosageElementsUtil.class);

    private DosageElementsUtil() {
    }


    static List<DosageRegion> makeDosageElements(Contig contig,
                                                     Strand strand,
                                                     Coordinates coordinates,
                                                     String id,
                                                     String haploinsufficiency,
                                                     String triplosensitivity) {

        DosageSensitivity haploDosageSensitivity = parseHaploinsufficiency(haploinsufficiency);
        DosageSensitivityEvidence haploinsufficiencyEvidence = parseEvidence(haploinsufficiency);
        Dosage haploinsufficiencyDosage = Dosage.of(id, haploDosageSensitivity, haploinsufficiencyEvidence);

        DosageSensitivity triploDosageSensitivity = parseTriplosensitivity(triplosensitivity);
        DosageSensitivityEvidence triplosensitivityEvidence = parseEvidence(triplosensitivity);
        Dosage triplosensitivityDosage = Dosage.of(id, triploDosageSensitivity, triplosensitivityEvidence);

        List<Dosage> dosages = new ArrayList<>(2);
        if (DosageSensitivity.HAPLOINSUFFICIENCY.equals(haploDosageSensitivity)) {
            dosages.add(haploinsufficiencyDosage);
        }
        if (DosageSensitivity.TRIPLOSENSITIVITY.equals(triploDosageSensitivity)) {
            dosages.add(triplosensitivityDosage);
        }

        if (dosages.isEmpty()) {
            return List.of();
        } else {
            GenomicRegion location = GenomicRegion.of(contig, strand, coordinates);
            return dosages.stream()
                    .map(d -> DosageRegion.of(location, d))
                    .collect(Collectors.toUnmodifiableList());
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
