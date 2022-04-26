package org.monarchinitiative.svanna.model.landscape.dosage;

import org.monarchinitiative.sgenes.model.Located;
import org.monarchinitiative.svart.GenomicRegion;

/**
 * The region shown to be dosage sensitive.
 * <p>
 * The region corresponds to a single-gene region, or to a multi-gene region.
 */
public interface DosageRegion extends Located {

    static DosageRegion of(GenomicRegion location, Dosage dosage) {
        return DosageRegionDefault.of(location, dosage);
    }

    Dosage dosage();


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
        return dosage().dosageSensitivityEvidence().isAtLeast(evidence)
                && dosage().dosageSensitivity() == DosageSensitivity.HAPLOINSUFFICIENCY;
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
        return dosage().dosageSensitivity() == DosageSensitivity.TRIPLOSENSITIVITY
                && dosage().dosageSensitivityEvidence().isAtLeast(evidence);
    }
}
