package org.jax.svanna.core.configuration;

import org.jax.svanna.core.hpo.IcMicaMode;
import org.jax.svanna.core.hpo.TermSimilarityMeasure;

import java.util.Objects;

public class PrioritizationProperties {

    private final TermSimilarityMeasure termSimilarityMeasure;
    private final IcMicaMode icMicaMode;
    private final int promoterLength;
    private final double promoterFitnessGain;
    private final double geneFactor;
    private final double enhancerFactor;

    public static PrioritizationProperties of(TermSimilarityMeasure termSimilarityMeasure,
                                              IcMicaMode icMicaMode,
                                              int promoterLength,
                                              double promoterFitnessGain,
                                              double geneFactor,
                                              double enhancerFactor) {
        return new PrioritizationProperties(termSimilarityMeasure,
                icMicaMode,
                promoterLength,
                promoterFitnessGain,
                geneFactor,
                enhancerFactor);
    }

    private PrioritizationProperties(TermSimilarityMeasure termSimilarityMeasure,
                                     IcMicaMode icMicaMode,
                                     int promoterLength,
                                     double promoterFitnessGain,
                                     double geneFactor,
                                     double enhancerFactor) {
        this.termSimilarityMeasure = Objects.requireNonNull(termSimilarityMeasure);
        this.icMicaMode = Objects.requireNonNull(icMicaMode);
        this.promoterLength = promoterLength;
        this.promoterFitnessGain = promoterFitnessGain;
        this.geneFactor = geneFactor;
        this.enhancerFactor = enhancerFactor;
    }

    public TermSimilarityMeasure termSimilarityMeasure() {
        return termSimilarityMeasure;
    }

    public IcMicaMode icMicaMode() {
        return icMicaMode;
    }

    public double geneFactor() {
        return geneFactor;
    }

    public int promoterLength() {
        return promoterLength;
    }

    public double enhancerFactor() {
        return enhancerFactor;
    }

    public double promoterFitnessGain() {
        return promoterFitnessGain;
    }

    @Override
    public String toString() {
        return "PrioritizationProperties{" +
                "termSimilarityMeasure=" + termSimilarityMeasure +
                ", icMicaMode=" + icMicaMode +
                ", promoterLength=" + promoterLength +
                ", promoterFitnessGain=" + promoterFitnessGain +
                ", geneFactor=" + geneFactor +
                ", enhancerFactor=" + enhancerFactor +
                '}';
    }
}
