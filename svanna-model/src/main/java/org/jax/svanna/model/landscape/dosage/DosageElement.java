package org.jax.svanna.model.landscape.dosage;

import org.monarchinitiative.svart.Contig;
import org.monarchinitiative.svart.Coordinates;
import org.monarchinitiative.svart.GenomicRegion;
import org.monarchinitiative.svart.Strand;

public interface DosageElement extends GenomicRegion {

    static DosageElement of(Contig contig,
                            Strand strand,
                            Coordinates coordinates,
                            String id,
                            DosageSensitivity dosageSensitivity,
                            DosageSensitivityEvidence evidence) {
        return new DosageElementDefault(contig, strand, coordinates, id, dosageSensitivity, evidence);
    }

    String id();

    DosageSensitivity dosageSensitivity();

    DosageSensitivityEvidence dosageSensitivityEvidence();

}
