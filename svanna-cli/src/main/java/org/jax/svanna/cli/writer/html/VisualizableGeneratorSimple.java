package org.jax.svanna.cli.writer.html;

import org.jax.svanna.core.hpo.GeneWithId;
import org.jax.svanna.core.hpo.HpoDiseaseSummary;
import org.jax.svanna.core.hpo.PhenotypeDataService;
import org.jax.svanna.core.landscape.AnnotationDataService;
import org.jax.svanna.core.landscape.Enhancer;
import org.jax.svanna.core.overlap.GeneOverlap;
import org.jax.svanna.core.overlap.GeneOverlapper;
import org.jax.svanna.core.reference.Gene;
import org.jax.svanna.core.reference.SvannaVariant;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class VisualizableGeneratorSimple implements VisualizableGenerator {

    private final GeneOverlapper overlapper;

    private final AnnotationDataService annotationDataService;

    private final PhenotypeDataService phenotypeDataService;

    private final Map<String, GeneWithId> geneMap;

    public VisualizableGeneratorSimple(GeneOverlapper overlapper,
                                       AnnotationDataService annotationDataService,
                                       PhenotypeDataService phenotypeDataService) {
        this.overlapper = overlapper;
        this.annotationDataService = annotationDataService;
        this.phenotypeDataService = phenotypeDataService;
        this.geneMap = phenotypeDataService.geneWithIds().stream()
                .collect(Collectors.toMap(GeneWithId::getSymbol, Function.identity()));
    }

    @Override
    public Visualizable makeVisualizable(SvannaVariant variant) {
        List<GeneOverlap> overlaps = overlapper.getOverlaps(variant);
        List<String> geneSymbols = overlaps.stream()
                .map(GeneOverlap::gene)
                .map(Gene::geneSymbol)
                .collect(Collectors.toList());
        List<Enhancer> enhancers = annotationDataService.overlappingEnhancers(variant);

        Set<HpoDiseaseSummary> diseaseSummaries = geneSymbols.stream()
                .filter(geneMap::containsKey)
                .map(geneMap::get)
                .map(id -> phenotypeDataService.getDiseasesForGene(id.getGeneId()))
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
        return SomeVisualizable.of(variant, diseaseSummaries, overlaps, enhancers);
    }
}
