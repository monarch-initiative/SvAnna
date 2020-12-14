package org.jax.svanna.core.prioritizer;

import org.monarchinitiative.variant.api.Variant;


public interface SvPrioritizer {

    SvPriority prioritize(Variant variant);

}
