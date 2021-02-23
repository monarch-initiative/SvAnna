package org.jax.svanna.core.priority.additive.landscape;

import org.monarchinitiative.svart.Variant;

@Deprecated(forRemoval = true)
public interface LandscapeEvaluator<L extends Landscape> {

    double evaluateLandscape(Variant variant, L landscape);
}
