package org.jax.svanna.model.landscape.dosage;

import org.monarchinitiative.svart.GenomicRegion;
import xyz.ielis.silent.genes.model.Located;

import java.util.List;

/**
 * The region shown to be dosage sensitive.
 * <p>
 * The region corresponds to a single-gene region, or to a multi-gene region.
 */
public interface DosageRegion extends Located {

    static DosageRegion of(GenomicRegion location, List<Dosage> dosages) {
        return DosageRegionDefault.of(location, dosages);
    }

    List<Dosage> dosages();


    /* ------------------------------------------- DERIVED METHODS -------------------------------------------------- */

    /**
     * @return true if there is at least little evidence for haploinsufficiency in this region.
     */
    default boolean isHaploinsufficient() {
        return isHaploinsufficient(DosageSensitivityEvidence.LITTLE_EVIDENCE);
    }

    /**
     * @param evidence minimal {@link DosageSensitivityEvidence} to consider
     * @return true if there is at least provided <code>evidence</code> for haploinsufficiency in this region.
     */
    default boolean isHaploinsufficient(DosageSensitivityEvidence evidence) {
        return dosages().stream()
                .filter(d -> d.dosageSensitivityEvidence().isAtLeast(evidence))
                .anyMatch(d -> d.dosageSensitivity() == DosageSensitivity.HAPLOINSUFFICIENCY);
    }

    /**
     * @return true if there is at least little evidence for triplosufficiency in this region.
     */
    default boolean isTriplosensitive() {
        return isTriplosensitive(DosageSensitivityEvidence.LITTLE_EVIDENCE);
    }

    /**
     * @param evidence minimal {@link DosageSensitivityEvidence} to consider
     * @return true if there is at least provided <code>evidence</code> for haploinsufficiency in this region.
     */
    default boolean isTriplosensitive(DosageSensitivityEvidence evidence) {
        return dosages().stream()
                .filter(d -> d.dosageSensitivityEvidence().isAtLeast(evidence))
                .anyMatch(d -> d.dosageSensitivity() == DosageSensitivity.TRIPLOSENSITIVITY);
    }
}
