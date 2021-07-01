package org.jax.svanna.cli.writer.html;

import org.jax.svanna.core.landscape.Enhancer;
import org.jax.svanna.core.overlap.GeneOverlap;
import org.jax.svanna.core.reference.Gene;
import org.jax.svanna.core.reference.SvannaVariant;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Information related to a single variant that is required to generate analysis summary.
 */
public interface VariantLandscape {

    SvannaVariant variant();

    default String getType() {
        return variant().variantType().toString();
    }

    List<GeneOverlap> overlaps();

    default List<Gene> genes() {
        return overlaps().stream().map(GeneOverlap::gene).collect(Collectors.toList());
    }

    List<Enhancer> enhancers();

}
