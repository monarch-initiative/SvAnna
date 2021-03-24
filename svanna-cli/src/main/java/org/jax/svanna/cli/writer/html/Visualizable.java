package org.jax.svanna.cli.writer.html;

import org.jax.svanna.core.hpo.HpoDiseaseSummary;
import org.jax.svanna.core.landscape.Enhancer;
import org.jax.svanna.core.overlap.GeneOverlap;
import org.jax.svanna.core.reference.Gene;
import org.jax.svanna.core.reference.SvannaVariant;

import java.util.List;
import java.util.Set;

public interface Visualizable {

    String getType();

    SvannaVariant variant();

    List<HtmlLocation> locations();

    Set<HpoDiseaseSummary> diseaseSummaries();

    List<GeneOverlap> overlaps();

    List<Gene> genes();

    /** @return the total number of genes affected by this structural variant. */
    int getGeneCount();

    List<Enhancer> enhancers();

}
