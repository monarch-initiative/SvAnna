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

        private double tadStabilityThreshold = .8;

        @NestedConfigurationProperty
        private EnhancerParameters enhancers = new EnhancerParameters();

        public void setEnhancers(EnhancerParameters enhancers) {
            this.enhancers = enhancers;
        }

        public EnhancerParameters enhancers() {
            return enhancers;
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

        private double geneFactor = 1.;

        private double enhancerFactor = .25;

        private int promoterLength = 2000;

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

        public double geneFactor() {
            return geneFactor;
        }

        public void setGeneFactor(double geneFactor) {
            this.geneFactor = geneFactor;
        }

        public double enhancerFactor() {
            return enhancerFactor;
        }

        public void setEnhancerFactor(double enhancerFactor) {
            this.enhancerFactor = enhancerFactor;
        }

        public int promoterLength() {
            return promoterLength;
        }

        public void setPromoterLength(int promoterLength) {
            this.promoterLength = promoterLength;
        }

    }

    public static class EnhancerParameters {

        private boolean useVista = true;
        private boolean useFantom5 = false;
        private double fantom5TissueSpecificity = .5;

        public boolean useVista() {
            return useVista;
        }

        public void setUseVista(boolean useVista) {
            this.useVista = useVista;
        }

        public boolean useFantom5() {
            return useFantom5;
        }

        public void setUseFantom5(boolean useFantom5) {
            this.useFantom5 = useFantom5;
        }

        public double fantom5TissueSpecificity() {
            return fantom5TissueSpecificity;
        }

        public void setFantom5TissueSpecificity(double fantom5TissueSpecificity) {
            this.fantom5TissueSpecificity = fantom5TissueSpecificity;
        }

    }

}
