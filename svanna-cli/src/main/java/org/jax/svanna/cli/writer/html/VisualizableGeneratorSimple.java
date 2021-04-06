package org.jax.svanna.cli.writer.html;

import org.jax.svanna.core.hpo.GeneWithId;
import org.jax.svanna.core.hpo.HpoDiseaseSummary;
import org.jax.svanna.core.hpo.PhenotypeDataService;
import org.jax.svanna.core.landscape.AnnotationDataService;
import org.jax.svanna.core.landscape.Enhancer;
import org.jax.svanna.core.landscape.RepetitiveRegion;
import org.jax.svanna.core.overlap.GeneOverlap;
import org.jax.svanna.core.overlap.GeneOverlapper;
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
    public VariantLandscape prepareLandscape(SvannaVariant variant) {
        List<GeneOverlap> overlaps = overlapper.getOverlaps(variant);
        List<Enhancer> enhancers = annotationDataService.overlappingEnhancers(variant);
        return SimpleVariantLandscape.of(variant, overlaps, enhancers);
    }

    @Override
    public Visualizable makeVisualizable(VariantLandscape variantLandscape) {
        List<RepetitiveRegion> repetitiveRegions = annotationDataService.overlappingRepetitiveRegions(variantLandscape.variant());

        Set<HpoDiseaseSummary> diseaseSummaries = variantLandscape.overlaps().stream()
                .map(geneOverlap -> geneOverlap.gene().geneSymbol())
                .filter(geneMap::containsKey)
                .map(geneMap::get)
                .map(id -> phenotypeDataService.getDiseasesForGene(id.getGeneId()))
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());

        return SimpleVisualizable.of(variantLandscape, diseaseSummaries, repetitiveRegions);
    }
}
