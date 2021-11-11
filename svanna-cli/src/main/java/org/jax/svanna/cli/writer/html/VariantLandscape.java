package org.jax.svanna.cli.writer.html;

import org.jax.svanna.core.overlap.GeneOverlap;
import org.jax.svanna.core.reference.SvannaVariant;
import org.jax.svanna.model.landscape.enhancer.Enhancer;
import xyz.ielis.silent.genes.model.Gene;

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
