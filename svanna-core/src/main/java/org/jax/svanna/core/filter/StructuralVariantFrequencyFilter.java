package org.jax.svanna.core.filter;

import org.jax.svanna.core.reference.SvannaVariant;
import org.monarchinitiative.variant.api.GenomicRegion;
import org.monarchinitiative.variant.api.Variant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * This filter flags variant that are present in given {@link SvFeatureSource} and match the query coordinates:
 * <ul>
 *     <li>similarity > similarity threshold</li>
 *     <li>frequency > frequency threshold</li>
 *     <li>variant type matches query variant type</li>
 * </ul>
 */
public class StructuralVariantFrequencyFilter implements Filter<SvannaVariant> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StructuralVariantFrequencyFilter.class);

    private static final FilterType FILTER_TYPE = FilterType.FREQUENCY_FILTER;

    private static final FilterResult FAIL = FilterResult.fail(FILTER_TYPE);
    private static final FilterResult PASS = FilterResult.pass(FILTER_TYPE);
    private static final FilterResult NOT_RUN = FilterResult.notRun(FILTER_TYPE);

    private final SvFeatureSource featureSource;
    private final float similarityThreshold;
    private final float frequencyThreshold;

    public StructuralVariantFrequencyFilter(SvFeatureSource featureSource, float similarityThreshold, float frequencyThreshold) {
        this.featureSource = featureSource;
        this.similarityThreshold = similarityThreshold;
        this.frequencyThreshold = frequencyThreshold;
    }

    static float reciprocalOverlap(GenomicRegion first, GenomicRegion second) {
        if (!first.overlapsWith(second)) {
            return 0;
        }
        first = first.toZeroBased();
        second = second.toZeroBased().withStrand(first.strand());
        int maxStart = Math.max(first.start(), second.start());
        int minEnd = Math.min(first.end(), second.end());

        float intersection = minEnd - maxStart;
        return Math.min(intersection / first.length(), intersection / second.length());
    }

    @Override
    public FilterType getFilterType() {
        return FILTER_TYPE;
    }

    @Override
    public FilterResult runFilter(SvannaVariant variant) {
        switch (variant.variantType().baseType()) {
            // this filter only supports filtering of some SV types
            case INS:
            case DUP:
            case DEL:
            case INV:
            case CNV:
                return performFiltering(variant);
            default:
                return NOT_RUN;
        }
    }

    private FilterResult performFiltering(Variant variant) {
        // get features from benign SV feature origin that share at least 1bp with the query region
        List<SvFeature> features = featureSource.getOverlappingFeatures(variant, SVFeatureOrigin.benign());

        return features.stream()
                .anyMatch(feature -> feature.variantType().baseType() == variant.variantType().baseType()
                        && feature.frequency() >= frequencyThreshold
                        && reciprocalOverlap(feature, variant) > similarityThreshold)
                ? FAIL
                : PASS;
    }
}
