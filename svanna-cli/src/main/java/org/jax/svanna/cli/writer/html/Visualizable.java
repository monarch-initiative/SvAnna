package org.jax.svanna.cli.writer.html;

import org.jax.svanna.core.overlap.GeneOverlap;
import org.jax.svanna.model.HpoDiseaseSummary;
import org.jax.svanna.model.landscape.repeat.RepetitiveRegion;
import xyz.ielis.silent.genes.model.Gene;

import java.util.List;
import java.util.Set;

/**
 * Information related to a single variant that is required to generate an entry within the analysis report.
 */
public interface Visualizable extends VariantLandscape {

    List<HtmlLocation> locations();

    Set<HpoDiseaseSummary> diseaseSummaries();

    List<RepetitiveRegion> repetitiveRegions();

    /**
     * @return the total number of genes affected by this structural variant.
     */
    default int getGeneCount() {
        return (int) overlaps().stream()
                .map(GeneOverlap::gene)
                .map(Gene::symbol)
                .distinct()
                .count();
    }

}
