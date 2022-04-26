package org.monarchinitiative.svanna.core.priority.additive;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class GranularEvaluatorUtils {

    private GranularEvaluatorUtils() {}

    public static Map<String, Double> calculateDeltas(Set<String> geneAccessions,
                                                      Map<String, Double> referenceScores,
                                                      Map<String, Double> alternateScores) {
        Map<String, Double> scoreMap = new HashMap<>();

        for (String geneAccession : geneAccessions) {
            double refScore = referenceScores.getOrDefault(geneAccession, 0.D);
            double altScore = alternateScores.getOrDefault(geneAccession, 0.D);

            double score = Math.abs(refScore - altScore);
            if (score > EvaluatorUtils.CLOSE_TO_ZERO)
                scoreMap.put(geneAccession, score);
        }

        return Map.copyOf(scoreMap);
    }
}
