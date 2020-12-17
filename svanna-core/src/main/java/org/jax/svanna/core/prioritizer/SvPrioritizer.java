package org.jax.svanna.core.prioritizer;

import org.monarchinitiative.variant.api.Variant;


public interface SvPrioritizer<T extends Variant> {

    SvPriority prioritize(T variant);

}
