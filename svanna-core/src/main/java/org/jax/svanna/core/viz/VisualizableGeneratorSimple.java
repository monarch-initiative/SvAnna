package org.jax.svanna.core.viz;

import org.jax.svanna.core.hpo.GeneWithId;
import org.jax.svanna.core.hpo.HpoDiseaseSummary;
import org.jax.svanna.core.hpo.PhenotypeDataService;
import org.jax.svanna.core.landscape.AnnotationDataService;
import org.jax.svanna.core.landscape.Enhancer;
import org.jax.svanna.core.overlap.Overlap;
import org.jax.svanna.core.overlap.Overlapper;
import org.jax.svanna.core.reference.SvannaVariant;
import org.jax.svanna.core.reference.Transcript;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class VisualizableGeneratorSimple implements VisualizableGenerator {

    private final Overlapper overlapper;

    private final AnnotationDataService annotationDataService;

    private final PhenotypeDataService phenotypeDataService;

    private final Map<String, GeneWithId> geneMap;

    public VisualizableGeneratorSimple(Overlapper overlapper,
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
        List<Overlap> overlaps = overlapper.getOverlapList(variant);
        List<Transcript> transcripts = overlaps.stream().map(Overlap::getTranscriptModel).collect(Collectors.toList());
        List<Enhancer> enhancers = annotationDataService.overlappingEnhancers(variant);

        List<HpoDiseaseSummary> diseaseSummaries = overlaps.stream()
                .map(Overlap::getGeneSymbol)
                .filter(geneMap::containsKey)
                .map(geneMap::get)
                .map(id -> phenotypeDataService.getDiseasesForGene(id.getGeneId()))
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        return SomeVisualizable.of(variant, diseaseSummaries, transcripts, enhancers, overlaps);
    }
}
