package org.jax.svanna.cli.writer.html;

import org.jax.svanna.core.reference.SvannaVariant;

public interface VisualizableGenerator {

    Visualizable makeVisualizable(SvannaVariant variant);

}
