package org.jax.svanna.cli.writer.html;

import org.jax.svanna.core.overlap.GeneOverlap;
import org.jax.svanna.core.reference.SvannaVariant;
import org.jax.svanna.model.landscape.dosage.DosageRegion;
import org.jax.svanna.model.landscape.enhancer.Enhancer;
import org.monarchinitiative.svart.VariantType;
import org.monarchinitiative.sgenes.model.Gene;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Information related to a single variant that is required to generate analysis summary.
 */
public interface VariantLandscape {

    SvannaVariant variant();

    default VariantType variantType() {
        return variant().genomicVariant().variantType();
    }

    List<GeneOverlap> overlaps();

    default List<Gene> genes() {
        return overlaps().stream()
                .map(GeneOverlap::gene)
                .collect(Collectors.toUnmodifiableList());
    }

    List<Enhancer> enhancers();

    List<DosageRegion> dosageRegions();

}
