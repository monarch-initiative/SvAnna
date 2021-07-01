package org.jax.svanna.core.filter;

import org.jax.svanna.core.landscape.AnnotationDataService;
import org.jax.svanna.core.landscape.PopulationVariant;
import org.jax.svanna.core.landscape.PopulationVariantOrigin;
import org.jax.svanna.core.reference.SvannaVariant;
import org.monarchinitiative.svart.Variant;

import java.util.Collection;

/**
 * This filter flags variant that are present in given {@link AnnotationDataService} and match the query coordinates:
 * <ul>
 *     <li>similarity > similarity threshold</li>
 *     <li>frequency > frequency threshold</li>
 *     <li>variant type matches query variant type</li>
 * </ul>
 */
public class StructuralVariantFrequencyFilter implements Filter<SvannaVariant> {

    private static final FilterType FILTER_TYPE = FilterType.FREQUENCY_FILTER;

    private static final FilterResult FAIL = FilterResult.fail(FILTER_TYPE);
    private static final FilterResult PASS = FilterResult.pass(FILTER_TYPE);
    private static final FilterResult NOT_RUN = FilterResult.notRun(FILTER_TYPE);

    private final AnnotationDataService annotationDataService;
    private final float similarityThreshold;
    private final float frequencyThreshold;

    public StructuralVariantFrequencyFilter(AnnotationDataService annotationDataService, float similarityThreshold, float frequencyThreshold) {
        this.annotationDataService = annotationDataService;
        this.similarityThreshold = similarityThreshold;
        this.frequencyThreshold = frequencyThreshold;
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
        // get features from benign origins that share at least 1bp with the query region
        Collection<PopulationVariant> features = annotationDataService.overlappingPopulationVariants(variant, PopulationVariantOrigin.benign());

        return features.stream()
                .anyMatch(feature -> feature.variantType().baseType() == variant.variantType().baseType()
                        && feature.alleleFrequency() >= frequencyThreshold
                        && FilterUtils.reciprocalOverlap(feature, variant) * 100.F > similarityThreshold)
                ? FAIL
                : PASS;
    }
}
