package org.monarchinitiative.svanna.core.configuration;


public class DataProperties {

    private final boolean useVista;
    private final boolean useFantom5;
    private final double fantom5TissueSpecificity;
    private final double tadStabilityThreshold;

    public static DataProperties of(boolean useVista,
                                    boolean useFantom5,
                                    double fantom5TissueSpecificity,
                                    double tadStabilityThreshold) {
        return new DataProperties(useVista, useFantom5, fantom5TissueSpecificity, tadStabilityThreshold);
    }

    private DataProperties(boolean useVista,
                           boolean useFantom5,
                           double fantom5TissueSpecificity,
                           double tadStabilityThreshold) {
        this.useVista = useVista;
        this.useFantom5 = useFantom5;
        this.fantom5TissueSpecificity = fantom5TissueSpecificity;
        this.tadStabilityThreshold = tadStabilityThreshold;
    }

    public boolean useVista() {
        return useVista;
    }

    public boolean useFantom5() {
        return useFantom5;
    }

    public double fantom5TissueSpecificity() {
        return fantom5TissueSpecificity;
    }

    public double tadStabilityThresholdAsPercentage() {
        return tadStabilityThreshold;
    }

    public double tadStabilityThresholdAsFraction() {
        return tadStabilityThreshold / 100.;
    }

    @Override
    public String toString() {
        return "DataProperties{" +
                "useVista=" + useVista +
                ", useFantom5=" + useFantom5 +
                ", fantom5TissueSpecificity=" + fantom5TissueSpecificity +
                ", tadStabilityThreshold=" + tadStabilityThreshold +
                '}';
    }
}
