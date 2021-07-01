package org.jax.svanna.core.filter;

import org.jax.svanna.core.landscape.AnnotationDataService;
import org.jax.svanna.core.landscape.RepetitiveRegion;
import org.jax.svanna.core.reference.SvannaVariant;
import org.monarchinitiative.svart.BreakendVariant;

import java.util.List;

@Deprecated(forRemoval = true)
public class RepetitiveRegionVariantFilter implements Filter<SvannaVariant> {

    private static final FilterType FILTER_TYPE = FilterType.REPETITIVE_REGION_FILTER;

    private static final FilterResult FAIL = FilterResult.fail(FILTER_TYPE);
    private static final FilterResult PASS = FilterResult.pass(FILTER_TYPE);
    private static final FilterResult NOT_RUN = FilterResult.notRun(FILTER_TYPE);

    private final AnnotationDataService annotationDataService;
    private final float similarityThreshold;

    public RepetitiveRegionVariantFilter(AnnotationDataService annotationDataService, float similarityThreshold) {
        this.annotationDataService = annotationDataService;
        this.similarityThreshold = similarityThreshold;
    }

    @Override
    public FilterType getFilterType() {
        return FILTER_TYPE;
    }

    @Override
    public FilterResult runFilter(SvannaVariant variant) {
        switch (variant.variantType().baseType()) {
            case INS:
            case DUP:
            case DEL:
            case INV:
            case CNV:
                return filterIntrachromosomalVariant(variant);
            case BND:
            case TRA:
                return filterBreakend((BreakendVariant) variant);
            default:
                return NOT_RUN;
        }

    }

    private FilterResult filterIntrachromosomalVariant(SvannaVariant variant) {
        return annotationDataService.overlappingRepetitiveRegions(variant).stream()
                .anyMatch(region -> FilterUtils.fractionShared(region, variant) * 100.F > similarityThreshold)
                ? FAIL
                : PASS;
    }

    private FilterResult filterBreakend(BreakendVariant variant) {
        List<RepetitiveRegion> left = annotationDataService.overlappingRepetitiveRegions(variant.left());
        if (left.isEmpty()) {
            return annotationDataService.overlappingRepetitiveRegions(variant.right()).isEmpty()
                    ? PASS
                    : FAIL;
        }
        return FAIL;
    }
}
