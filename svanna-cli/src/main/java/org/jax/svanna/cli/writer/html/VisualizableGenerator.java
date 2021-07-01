package org.jax.svanna.cli.writer.html;

import org.jax.svanna.core.reference.SvannaVariant;

public interface VisualizableGenerator {

    VariantLandscape prepareLandscape(SvannaVariant variant);

    Visualizable makeVisualizable(VariantLandscape variantLandscape);

}
