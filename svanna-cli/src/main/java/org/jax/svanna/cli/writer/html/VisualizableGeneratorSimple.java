package org.jax.svanna.cli.writer.html;

import org.jax.svanna.core.LogUtils;
import org.jax.svanna.core.overlap.GeneOverlap;
import org.jax.svanna.core.overlap.GeneOverlapper;
import org.jax.svanna.core.reference.SvannaVariant;
import org.jax.svanna.core.service.AnnotationDataService;
import org.jax.svanna.core.service.PhenotypeDataService;
import org.jax.svanna.model.HpoDiseaseSummary;
import org.jax.svanna.model.gene.Gene;
import org.jax.svanna.model.gene.GeneIdentifier;
import org.jax.svanna.model.landscape.Enhancer;
import org.jax.svanna.model.landscape.RepetitiveRegion;
import org.monarchinitiative.svart.BreakendVariant;
import org.monarchinitiative.svart.GenomicRegion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class VisualizableGeneratorSimple implements VisualizableGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(VisualizableGeneratorSimple.class);

    private final GeneOverlapper overlapper;

    private final AnnotationDataService annotationDataService;

    private final PhenotypeDataService phenotypeDataService;

    private final Map<String, GeneIdentifier> geneMap;

    public VisualizableGeneratorSimple(GeneOverlapper overlapper,
                                       AnnotationDataService annotationDataService,
                                       PhenotypeDataService phenotypeDataService) {
        this.overlapper = overlapper;
        this.annotationDataService = annotationDataService;
        this.phenotypeDataService = phenotypeDataService;
        this.geneMap = phenotypeDataService.geneWithIds().stream()
                .collect(Collectors.toMap(GeneIdentifier::symbol, Function.identity()));
    }

    @Override
    public VariantLandscape prepareLandscape(SvannaVariant variant) {
        List<GeneOverlap> overlaps = overlapper.getOverlaps(variant);
        List<Enhancer> enhancers = annotationDataService.overlappingEnhancers(variant);
        return SimpleVariantLandscape.of(variant, overlaps, enhancers);
    }

    @Override
    public Visualizable makeVisualizable(VariantLandscape variantLandscape) {
        SvannaVariant variant = variantLandscape.variant();
        List<GeneOverlap> overlaps = variantLandscape.overlaps();
        List<RepetitiveRegion> repetitiveRegions = prepareRepetitiveRegions(variant, overlaps);

        Set<HpoDiseaseSummary> diseaseSummaries = overlaps.stream()
                .map(geneOverlap -> geneOverlap.gene().geneSymbol())
                .filter(geneMap::containsKey)
                .map(geneMap::get)
                .map(id -> phenotypeDataService.getDiseasesForGene(id.accessionId()))
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());

        return SimpleVisualizable.of(variantLandscape, diseaseSummaries, repetitiveRegions);
    }

    private List<RepetitiveRegion> prepareRepetitiveRegions(SvannaVariant variant, List<GeneOverlap> overlaps) {
        List<RepetitiveRegion> repetitiveRegions = new LinkedList<>();
        if (overlaps.isEmpty()) {
            LogUtils.logWarn(LOGGER, "No gene for variant {}", variant.id());
        } else {
            if (!(variant instanceof BreakendVariant)) {
                GenomicRegion viewport = calculateUpperLowerBounds(variant, overlaps);
                repetitiveRegions.addAll(annotationDataService.overlappingRepetitiveRegions(viewport));
            } else {
                Map<Integer, List<GeneOverlap>> overlapsByContig = overlaps.stream()
                        .collect(Collectors.groupingBy(go -> go.gene().contigId(), Collectors.toUnmodifiableList()));
                BreakendVariant bv = (BreakendVariant) variant;
                List<GeneOverlap> leftOverlaps = overlapsByContig.getOrDefault(bv.left().contigId(), List.of());
                if (!leftOverlaps.isEmpty()) {
                    GenomicRegion viewport = calculateUpperLowerBounds(bv.left(), leftOverlaps);
                    repetitiveRegions.addAll(annotationDataService.overlappingRepetitiveRegions(viewport));
                }
                List<GeneOverlap> rightOverlaps = overlapsByContig.getOrDefault(bv.right().contigId(), List.of());
                if (!rightOverlaps.isEmpty()) {
                    GenomicRegion viewport = calculateUpperLowerBounds(bv.right(), rightOverlaps);
                    repetitiveRegions.addAll(annotationDataService.overlappingRepetitiveRegions(viewport));
                }
            }
        }
        return repetitiveRegions;
    }

    private static GenomicRegion calculateUpperLowerBounds(GenomicRegion variant, List<GeneOverlap> overlaps) {
        int start = variant.start();
        int end = variant.end();

        for (GeneOverlap overlap : overlaps) {
            Gene gene = overlap.gene();
            int s = gene.startOnStrandWithCoordinateSystem(variant.strand(), variant.coordinateSystem());
            start = Math.min(s, start);

            int e = gene.endOnStrandWithCoordinateSystem(variant.strand(), variant.coordinateSystem());
            end = Math.max(e, end);
        }

        return GenomicRegion.of(variant.contig(), variant.strand(), variant.coordinateSystem(), start, end);
    }

}
