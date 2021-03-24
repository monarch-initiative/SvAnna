package org.jax.svanna.core.overlap;

import org.monarchinitiative.svart.Variant;

import java.util.List;

public interface GeneOverlapper {

    List<GeneOverlap> getOverlaps(Variant variant);

}
