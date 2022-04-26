package org.monarchinitiative.svanna.ingest.parse.enhancer.fantom;

import java.util.Collections;
import java.util.List;

/**
 * We use this to calculate some statistics about read counts and tau
 * We want to drop the lowest N percentile
 *
 */
public class DescriptiveStats {

    private final List<Double> values;

    public DescriptiveStats(List<Double> vals) {
        Collections.sort(vals);
        this.values = vals;
    }

    public double getMedian() {
        int middle = values.size() / 2;
        if (values.size() % 2 == 1) {
            return values.get(middle);
        } else {
            return  (values.get(middle - 1) + values.get(middle)) / 2.0;
        }
    }

    public double getMean() {
        return values.stream().mapToDouble(Double::doubleValue)
                .average()
                .orElse(Double.NaN);
    }

    public double getValueAtPercentile(int percentile) {
        int index = (percentile * values.size()/100);
        return this.values.get(index);
    }


}
