package org.jax.svanna.core.filter;

import org.jax.svanna.core.reference.AnnotatedVariant;
import org.monarchinitiative.variant.api.GenomicRegion;
import org.monarchinitiative.variant.api.Variant;

import java.util.List;
import java.util.function.Predicate;

public class StructuralVariantFrequencyFilter implements Filter<AnnotatedVariant> {

    private static final FilterType FILTER_TYPE = FilterType.FREQUENCY_FILTER;

    private static final FilterResult FAIL = FilterResult.fail(FILTER_TYPE);
    private static final FilterResult PASS = FilterResult.pass(FILTER_TYPE);
    private static final FilterResult NOT_RUN = FilterResult.notRun(FILTER_TYPE);

    private final SvFeatureSource featureSource;

    public StructuralVariantFrequencyFilter(SvFeatureSource featureSource) {
        this.featureSource = featureSource;
    }

    @Override
    public FilterType getFilterType() {
        return FILTER_TYPE;
    }

    @Override
    public FilterResult runFilter(AnnotatedVariant variant) {
        switch (variant.variantType().baseType()) {
            // this filter only supports filtering of some SV types
            case DEL:
            case INS:
            case CNV:
            case INV:
                return performFiltering(variant);
            default:
                return NOT_RUN;
        }
    }

    private FilterResult performFiltering(Variant variant) {
        // get features from benign SV feature origin that share at least 1bp with the query region
        List<SvFeature> features = featureSource.getOverlappingFeatures(variant, SVFeatureOrigin.benign());

        return features.stream().anyMatch(isSimilarEnough(variant))
                ? FAIL
                : PASS;
    }


    /**
     * Return predicate for evaluating similarity of the <code>feature</code> to given <code>region</code>.
     *
     * @param region
     * @return
     */
    private Predicate<GenomicRegion> isSimilarEnough(GenomicRegion region) {
        // TODO - implement similarity calculation
        return feature -> false;
    }
}
