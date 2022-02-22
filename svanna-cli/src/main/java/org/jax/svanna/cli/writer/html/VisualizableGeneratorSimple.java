package org.jax.svanna.cli.writer.html;

import org.jax.svanna.core.LogUtils;
import org.jax.svanna.core.overlap.GeneOverlap;
import org.jax.svanna.core.overlap.GeneOverlapper;
import org.jax.svanna.core.reference.SvannaVariant;
import org.jax.svanna.core.service.AnnotationDataService;
import org.jax.svanna.core.service.PhenotypeDataService;
import org.jax.svanna.model.HpoDiseaseSummary;
import org.jax.svanna.model.landscape.dosage.Dosage;
import org.jax.svanna.model.landscape.dosage.DosageRegion;
import org.jax.svanna.model.landscape.dosage.DosageSensitivity;
import org.jax.svanna.model.landscape.dosage.DosageSensitivityEvidence;
import org.jax.svanna.model.landscape.enhancer.Enhancer;
import org.jax.svanna.model.landscape.repeat.RepetitiveRegion;
import org.monarchinitiative.svart.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.monarchinitiative.sgenes.model.Gene;
import org.monarchinitiative.sgenes.model.GeneIdentifier;
import org.monarchinitiative.sgenes.model.Located;

import java.util.*;
import java.util.stream.Collectors;

public class VisualizableGeneratorSimple implements VisualizableGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(VisualizableGeneratorSimple.class);

    private final GeneOverlapper overlapper;

    private final AnnotationDataService annotationDataService;

    private final PhenotypeDataService phenotypeDataService;

    private final Map<String, List<GeneIdentifier>> hgvsSymbolToGeneIdentifier;

    public VisualizableGeneratorSimple(GeneOverlapper overlapper,
                                       AnnotationDataService annotationDataService,
                                       PhenotypeDataService phenotypeDataService) {
        this.overlapper = overlapper;
        this.annotationDataService = annotationDataService;
        this.phenotypeDataService = phenotypeDataService;
        this.hgvsSymbolToGeneIdentifier = phenotypeDataService.geneByHgvsSymbol();
    }

    private static List<DosageRegion> mergeOverlappingDosageRegions(List<DosageRegion> dosageRegions) {
        Map<Integer, List<DosageRegion>> regionsByContigId = dosageRegions.stream()
                .collect(Collectors.groupingBy(Located::contigId, Collectors.toUnmodifiableList()));

        List<DosageRegion> dosages = new ArrayList<>(dosageRegions.size());
        LinkedList<DosageRegion> inProcess = new LinkedList<>();
        for (Map.Entry<Integer, List<DosageRegion>> contigEntry : regionsByContigId.entrySet()) {

            Map<DosageSensitivity, List<DosageRegion>> regionsByDosage = contigEntry.getValue().stream()
                    .collect(Collectors.groupingBy(dr -> dr.dosage().dosageSensitivity(), Collectors.toUnmodifiableList()));

            for (Map.Entry<DosageSensitivity, List<DosageRegion>> dosageEntry : regionsByDosage.entrySet()) {
                List<DosageRegion> sorted = dosageEntry.getValue().stream()
                        .sorted((l, r) -> GenomicRegion.compare(l.location(), r.location()))
                        .collect(Collectors.toUnmodifiableList());

                if (sorted.isEmpty()) continue;
                inProcess.add(sorted.get(0));

                for (int i = 1; i < sorted.size(); i++) {
                    DosageRegion current = sorted.get(i);
                    if (!current.location().overlapsWith(inProcess.getLast().location())) {
                        // merge all regions that are inProcess
                        DosageRegion merged = mergeDosageRegions(inProcess);
                        dosages.add(merged);
                        inProcess.clear();
                    }
                    inProcess.add(current);

                }

                if (!inProcess.isEmpty()) // process the last dosage region
                    dosages.add(mergeDosageRegions(inProcess));

            }
        }

        return dosages;
    }

    private static DosageRegion mergeDosageRegions(LinkedList<DosageRegion> dosageRegions) {
        // the dosage regions are assumed to be overlapping and located on one contig.
        DosageRegion first = dosageRegions.getFirst();
        if (dosageRegions.size() == 1)
            return first;

        // find start/end bounds and the lower bound of the evidence
        int start = Integer.MAX_VALUE, end = Integer.MIN_VALUE;
        List<String> dosageIds = new ArrayList<>(dosageRegions.size());
        DosageSensitivityEvidence evidence = DosageSensitivityEvidence.SUFFICIENT_EVIDENCE; // max evidence level, we only decrease here
        for (DosageRegion region : dosageRegions) {
            start = Math.min(start, region.startOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased()));
            end = Math.max(end, region.endOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased()));
            dosageIds.add(region.dosage().id());

            DosageSensitivityEvidence currentDosageSensitivityEvidence = region.dosage().dosageSensitivityEvidence();
            if (currentDosageSensitivityEvidence.ordinal() < evidence.ordinal())
                evidence = currentDosageSensitivityEvidence;
        }

        // merge the region
        GenomicRegion mergedRegion = GenomicRegion.of(first.contig(), Strand.POSITIVE, CoordinateSystem.zeroBased(), start, end);
        Dosage dosage = Dosage.of(String.join("; ", dosageIds), first.dosage().dosageSensitivity(), evidence);
        return DosageRegion.of(mergedRegion, dosage);
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

    @Override
    public VariantLandscape prepareLandscape(SvannaVariant variant) {
        GenomicVariant v = variant.genomicVariant();
        List<GeneOverlap> overlaps = overlapper.getOverlaps(v);
        List<Enhancer> enhancers = annotationDataService.overlappingEnhancers(v);
        List<DosageRegion> dosageRegions = annotationDataService.dosageElements(v);
        return SimpleVariantLandscape.of(variant, overlaps, enhancers, dosageRegions);
    }

    @Override
    public Visualizable makeVisualizable(VariantLandscape variantLandscape) {
        SvannaVariant variant = variantLandscape.variant();
        List<GeneOverlap> overlaps = variantLandscape.overlaps();
        List<RepetitiveRegion> repetitiveRegions = prepareRepetitiveRegions(variant.genomicVariant(), overlaps);
        List<DosageRegion> dosageRegions = mergeOverlappingDosageRegions(variantLandscape.dosageRegions());

        List<HpoDiseaseSummary> diseaseSummaries = overlaps.stream()
                // get gene IDs from the overlaps
                .map(geneOverlap -> geneOverlap.gene().symbol())
                .map(key -> hgvsSymbolToGeneIdentifier.getOrDefault(key, List.of()))
                .flatMap(Collection::stream)
                // get HGNC IDs from gene IDs
                .flatMap(id -> id.hgncId().stream()) // only work with gene IDs that have HGNC id
                .distinct()
                // get associated diseases for HGNC IDs
                .map(phenotypeDataService::getDiseasesForGene)
                .flatMap(Collection::stream)
                .collect(Collectors.toUnmodifiableList());

        return SimpleVisualizable.of(variantLandscape, diseaseSummaries, repetitiveRegions, dosageRegions);
    }

    private List<RepetitiveRegion> prepareRepetitiveRegions(GenomicVariant variant, List<GeneOverlap> overlaps) {
        List<RepetitiveRegion> repetitiveRegions = new LinkedList<>();
        if (overlaps.isEmpty()) {
            LogUtils.logWarn(LOGGER, "No gene for variant {}", variant.id());
        } else {
            if (!(variant instanceof GenomicBreakendVariant)) {
                GenomicRegion viewport = calculateUpperLowerBounds(variant, overlaps);
                repetitiveRegions.addAll(annotationDataService.overlappingRepetitiveRegions(viewport));
            } else {
                Map<Integer, List<GeneOverlap>> overlapsByContig = overlaps.stream()
                        .collect(Collectors.groupingBy(go -> go.gene().contigId(), Collectors.toUnmodifiableList()));
                GenomicBreakendVariant bv = (GenomicBreakendVariant) variant;
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

}
