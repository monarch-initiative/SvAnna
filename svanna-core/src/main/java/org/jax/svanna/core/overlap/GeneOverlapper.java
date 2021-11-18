package org.jax.svanna.core.overlap;

import org.jax.svanna.core.service.GeneService;
import org.monarchinitiative.svart.Variant;

import java.util.List;

public interface GeneOverlapper {

    static GeneOverlapper of(GeneService geneService) {
        return new GeneOverlapperImpl(geneService);
    }

    List<GeneOverlap> getOverlaps(Variant variant);

}
