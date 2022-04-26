package org.monarchinitiative.svanna.cli.writer.html;

import org.monarchinitiative.svanna.core.overlap.GeneOverlap;
import org.monarchinitiative.svanna.model.HpoDiseaseSummary;
import org.monarchinitiative.svanna.model.landscape.repeat.RepetitiveRegion;
import org.monarchinitiative.sgenes.model.Gene;

import java.util.List;

/**
 * Information related to a single variant that is required to generate an entry within the analysis report.
 */
public interface Visualizable extends VariantLandscape {

    List<HtmlLocation> locations();

    List<HpoDiseaseSummary> diseaseSummaries();

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
