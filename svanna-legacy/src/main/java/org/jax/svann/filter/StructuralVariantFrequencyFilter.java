package org.jax.svann.filter;

import org.jax.svann.reference.GenomicRegion;
import org.jax.svann.reference.StandardGenomicRegion;
import org.jax.svann.reference.StructuralVariant;

import java.util.List;
import java.util.function.Predicate;

public class StructuralVariantFrequencyFilter implements Filter<StructuralVariant> {

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
    public FilterResult runFilter(StructuralVariant variant) {
        switch (variant.getType()) {
            // this filter only supports filtering of some SV types
            case DELETION:
            case INSERTION:
            case CNV:
            case INVERSION:
                return performFiltering(variant);
            default:
                return NOT_RUN;
        }
    }

    private FilterResult performFiltering(StructuralVariant variant) {
        // when calling this method, we know that the variant is either deletion, insertion, cnv, or inversion.
        GenomicRegion region;
        switch (variant.getType()) {
            case DELETION:
            case INSERTION:
            case INVERSION:
                region = StandardGenomicRegion.of(variant.getLeftmostBreakend(), variant.getRightmostBreakend());
                break;
            default:
                return NOT_RUN;
        }

        // get features from benign SV feature origin that share at least 1bp with the query region
        List<SvFeature> features = featureSource.getOverlappingFeatures(region, SVFeatureOrigin.benign());

        return features.stream().anyMatch(isSimilarEnough(region))
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
