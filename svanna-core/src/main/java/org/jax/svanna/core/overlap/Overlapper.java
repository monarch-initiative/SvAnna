package org.jax.svanna.core.overlap;

import org.monarchinitiative.svart.Variant;

import java.util.List;

public interface Overlapper {

    List<Overlap> getOverlapList(Variant variant);

}
