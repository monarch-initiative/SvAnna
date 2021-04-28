package org.jax.svanna.core.filter;

import org.jax.svanna.core.exception.LogUtils;
import org.jax.svanna.core.landscape.AnnotationDataService;
import org.jax.svanna.core.landscape.PopulationVariant;
import org.jax.svanna.core.landscape.PopulationVariantOrigin;
import org.jax.svanna.core.reference.SvannaVariant;
import org.monarchinitiative.svart.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class PopulationFrequencyAndCoverageFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(PopulationFrequencyAndCoverageFilter.class);

    private static final int BATCH = 100;

    // FREQUENCY
    private static final FilterType FF_FILTER_TYPE = FilterType.FREQUENCY_FILTER;
    private static final FilterResult FF_FAIL = FilterResult.fail(FF_FILTER_TYPE);
    private static final FilterResult FF_PASS = FilterResult.pass(FF_FILTER_TYPE);
    private static final FilterResult FF_NOT_RUN = FilterResult.notRun(FF_FILTER_TYPE);

    // READ_DEPTH
    private static final FilterType COVERAGE_FILTER_TYPE = FilterType.COVERAGE_FILTER;
    private static final FilterResult COVERAGE_FAIL = FilterResult.fail(COVERAGE_FILTER_TYPE);
    private static final FilterResult COVERAGE_PASS = FilterResult.pass(COVERAGE_FILTER_TYPE);

    // VARIANT_LENGTH
    private static final FilterType MAX_LENGTH_FILTER_TYPE = FilterType.MAX_LENGTH_FILTER;
    private static final FilterResult MAX_LENGTH_FAIL = FilterResult.fail(MAX_LENGTH_FILTER_TYPE);
    private static final FilterResult MAX_LENGTH_PASS = FilterResult.pass(MAX_LENGTH_FILTER_TYPE);

    private static final Set<VariantType> FREQ_FILTER_RECOGNIZED_VARIANTS = Set.of(
            VariantType.INS, VariantType.DUP, VariantType.DEL, VariantType.INV, VariantType.CNV);

    private final AnnotationDataService annotationDataService;
    private final float similarityThreshold;
    private final float frequencyThreshold;
    private final int minReads;
    private final int maxLength;

    public PopulationFrequencyAndCoverageFilter(AnnotationDataService annotationDataService,
                                                float similarityThreshold,
                                                float frequencyThreshold,
                                                int minReads,
                                                int maxLength) {
        this.annotationDataService = annotationDataService;
        this.similarityThreshold = similarityThreshold;
        this.frequencyThreshold = frequencyThreshold;
        this.minReads = minReads;
        this.maxLength = maxLength;
    }


    public <T extends SvannaVariant> List<T> filter(Collection<T> variants) {
        Map<Integer, List<T>> variantsByContig = variants.stream()
                .collect(Collectors.groupingBy(Variant::contigId));
        List<T> results = new LinkedList<>();
        for (Integer contigId : variantsByContig.keySet()) {
            List<T> contigVariants = variantsByContig.get(contigId).stream()
                    .sorted(byPositionOnPositiveStrand())
                    .collect(Collectors.toList());
            LogUtils.logDebug(LOGGER, "Filtering variants on contig `{}`", contigId);
            int start = 0;
            int end = Math.min(start + BATCH, contigVariants.size());
            while (true) {
                List<T> sublist = contigVariants.subList(start, end);
                results.addAll(processSublist(sublist));

                if (end == contigVariants.size())
                    break;
                start = end;
                end = Math.min(end + BATCH, contigVariants.size());
            }
        }

        return results;
    }

    private <T extends SvannaVariant> Collection<? extends T> processSublist(List<T> sublist) {
        if (sublist.isEmpty()) {
            return sublist;
        }

        int minPos = -1, maxPos = -1;

        for (T t : sublist) {
            int startPos = t.startOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased());
            if (minPos == -1 || startPos < minPos) {
                minPos = startPos;
            }
            int endPos = t.endOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased());
            if (maxPos == -1 || endPos > maxPos) {
                maxPos = startPos;
            }
        }
        List<T> results = new LinkedList<>();
        T variant = sublist.get(0);
        GenomicRegion query = GenomicRegion.of(variant.contig(), Strand.POSITIVE, CoordinateSystem.zeroBased(), minPos, maxPos);
        LogUtils.logTrace(LOGGER, "Filtering variants on contig `{}-{}-{}`", query.contigId(), query.start(), query.end());
        List<PopulationVariant> populationVariants = annotationDataService.overlappingPopulationVariants(query, PopulationVariantOrigin.benign());
        for (T item : sublist) {
            FilterResult freqFilterResult = runFrequencyFilter(populationVariants, item);
            item.addFilterResult(freqFilterResult);

            FilterResult coverageFilterResult = runCoverageFilter(item);
            item.addFilterResult(coverageFilterResult);

            FilterResult maxLengthResult = runMaxLengthFilter(item);
            item.addFilterResult(maxLengthResult);

            results.add(item);
        }

        return results;
    }

    private <T extends SvannaVariant> FilterResult runFrequencyFilter(List<PopulationVariant> populationVariants, T item) {
        FilterResult freqFilterResult = null;
        if (FREQ_FILTER_RECOGNIZED_VARIANTS.contains(item.variantType().baseType())) {
            for (PopulationVariant populationVariant : populationVariants) {
                if (populationVariant.variantType().baseType() == item.variantType().baseType()
                        && populationVariant.alleleFrequency() >= frequencyThreshold
                        && FilterUtils.reciprocalOverlap(populationVariant, item) * 100.F > similarityThreshold) {
                    freqFilterResult = FF_FAIL;
                    break;
                }
            }
            if (freqFilterResult == null)
                freqFilterResult = FF_PASS;
        } else {
            freqFilterResult = FF_NOT_RUN;
        }
        return freqFilterResult;
    }

    private <T extends SvannaVariant> FilterResult runCoverageFilter(T item) {
        return (item.numberOfAltReads() < minReads)
                ? COVERAGE_FAIL
                : COVERAGE_PASS;
    }

    private <T extends SvannaVariant> FilterResult runMaxLengthFilter(T item) {
        return item.length() > maxLength
                ? MAX_LENGTH_FAIL
                : MAX_LENGTH_PASS;
    }

    private static <T extends SvannaVariant> Comparator<? super T> byPositionOnPositiveStrand() {
        return Comparator
                .comparingInt(t -> ((SvannaVariant) t).startOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased()))
                .thenComparingInt(v -> (((SvannaVariant) v).endOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased())));
    }

}
