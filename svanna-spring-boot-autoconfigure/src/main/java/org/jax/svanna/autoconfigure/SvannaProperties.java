package org.jax.svanna.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

@ConfigurationProperties(prefix = "svanna")
public class SvannaProperties {

    /**
     * Path to directory with SvAnna files.
     */
    private String dataDirectory;
    private String jannovarCachePath;

    @NestedConfigurationProperty
    private DataParameters dataParameters = new DataParameters();

    @NestedConfigurationProperty
    private PrioritizationParameters prioritizationParameters = new PrioritizationParameters();


    public String dataDirectory() {
        return dataDirectory;
    }

    public void setDataDirectory(String dataDirectory) {
        this.dataDirectory = dataDirectory;
    }

    public String jannovarCachePath() {
        return jannovarCachePath;
    }

    public void setJannovarCachePath(String jannovarCachePath) {
        this.jannovarCachePath = jannovarCachePath;
    }

    public DataParameters dataParameters() {
        return dataParameters;
    }

    public void setDataParameters(DataParameters dataParameters) {
        this.dataParameters = dataParameters;
    }


    public PrioritizationParameters prioritizationParameters() {
        return prioritizationParameters;
    }

    public void setPrioritizationParameters(PrioritizationParameters prioritizationParameters) {
        this.prioritizationParameters = prioritizationParameters;
    }

    public static class DataParameters {

        private double tadStabilityThreshold = .25;

        private double enhancerSpecificityThreshold = .5;

        public double enhancerSpecificityThreshold() {
            return enhancerSpecificityThreshold;
        }

        public void setEnhancerSpecificityThreshold(double enhancerSpecificityThreshold) {
            this.enhancerSpecificityThreshold = enhancerSpecificityThreshold;
        }

        public double tadStabilityThreshold() {
            return tadStabilityThreshold;
        }

        public void setTadStabilityThreshold(double tadStabilityThreshold) {
            this.tadStabilityThreshold = tadStabilityThreshold;
        }

    }

    public enum TermSimilarityMeasure {
        RESNIK_SYMMETRIC,
        RESNIK_ASYMMETRIC
    }

    public static class PrioritizationParameters {

        private TermSimilarityMeasure termSimilarityMeasure = TermSimilarityMeasure.RESNIK_SYMMETRIC;

        private int maxGenes = 100;

        public int maxGenes() {
            return maxGenes;
        }

        public void setMaxGenes(int maxGenes) {
            this.maxGenes = maxGenes;
        }

        public TermSimilarityMeasure termSimilarityMeasure() {
            return termSimilarityMeasure;
        }

        public void setTermSimilarityMeasure(TermSimilarityMeasure termSimilarityMeasure) {
            this.termSimilarityMeasure = termSimilarityMeasure;
        }
    }
}
