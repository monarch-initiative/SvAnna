package org.jax.svanna.autoconfigure.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "svanna.prioritization")
public class PrioritizationProperties {

    private TermSimilarityMeasure termSimilarityMeasure = TermSimilarityMeasure.RESNIK_SYMMETRIC;
    private IcMicaMode icMicaMode = IcMicaMode.DATABASE;
    private int promoterLength = 2000;
    private double promoterFitnessGain = .6;
    private double geneFactor = 1.;
    private double enhancerFactor = .25;

    public TermSimilarityMeasure termSimilarityMeasure() {
        return termSimilarityMeasure;
    }

    public void setTermSimilarityMeasure(TermSimilarityMeasure termSimilarityMeasure) {
        this.termSimilarityMeasure = termSimilarityMeasure;
    }

    public IcMicaMode icMicaMode() {
        return icMicaMode;
    }

    public void setIcMicaMode(IcMicaMode icMicaMode) {
        this.icMicaMode = icMicaMode;
    }

    public double geneFactor() {
        return geneFactor;
    }

    public void setGeneFactor(double geneFactor) {
        this.geneFactor = geneFactor;
    }

    public int promoterLength() {
        return promoterLength;
    }

    public void setPromoterLength(int promoterLength) {
        this.promoterLength = promoterLength;
    }

    public double enhancerFactor() {
        return enhancerFactor;
    }

    public void setEnhancerFactor(double enhancerFactor) {
        this.enhancerFactor = enhancerFactor;
    }

    public double promoterFitnessGain() {
        return promoterFitnessGain;
    }

    public void setPromoterFitnessGain(double promoterFitnessGain) {
        this.promoterFitnessGain = promoterFitnessGain;
    }

    public enum TermSimilarityMeasure {
        RESNIK_SYMMETRIC,
        RESNIK_ASYMMETRIC
    }

    /**
     * Describe the way how to get information content (IC) of the most informative common ancestor (MICA) for two
     * HPO terms.
     */
    public enum IcMicaMode {
        /**
         * All positive <code>IC(MICA(t1,t2))</code> values are loaded into main memory. This mode is faster but more
         * memory intensive.
         */
        IN_MEMORY,

        /**
         * The <code>IC(MICA(t1,t2))</code> values are retrieved using individual database lookups, when required.
         */
        DATABASE
    }
}
