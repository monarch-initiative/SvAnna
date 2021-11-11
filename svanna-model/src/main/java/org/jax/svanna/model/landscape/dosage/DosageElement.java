package org.jax.svanna.model.landscape.dosage;

import org.monarchinitiative.svart.GenomicRegion;
import xyz.ielis.silent.genes.model.Located;

public interface DosageElement extends Located {

    static DosageElement of(GenomicRegion location,
                            String id,
                            DosageSensitivity dosageSensitivity,
                            DosageSensitivityEvidence evidence) {
        return new DosageElementDefault(location, id, dosageSensitivity, evidence);
    }

    String id();

    DosageSensitivity dosageSensitivity();

    DosageSensitivityEvidence dosageSensitivityEvidence();

}
