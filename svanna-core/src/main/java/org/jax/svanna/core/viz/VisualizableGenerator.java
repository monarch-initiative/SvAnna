package org.jax.svanna.core.viz;

import org.jax.svanna.core.reference.SvannaVariant;

public interface VisualizableGenerator {

    Visualizable makeVisualizable(SvannaVariant variant);

}
