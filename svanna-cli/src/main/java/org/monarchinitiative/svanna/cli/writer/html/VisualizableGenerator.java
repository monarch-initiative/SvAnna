package org.monarchinitiative.svanna.cli.writer.html;

import org.monarchinitiative.svanna.core.reference.SvannaVariant;

public interface VisualizableGenerator {

    VariantLandscape prepareLandscape(SvannaVariant variant);

    Visualizable makeVisualizable(VariantLandscape variantLandscape);

}
