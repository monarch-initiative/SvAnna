package org.jax.svanna.cli.writer.html;

import org.jax.svanna.model.landscape.variant.PopulationVariantOrigin;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class AnalysisParameters {

    /**
     * Path to SvAnna resource folder used in this analysis.
     */
    private String dataDirectory;
    /**
     * Path to phenopacket (null if not used).
     */
    private String phenopacketPath;
    /**
     * Path to the used VCF file.
     */
    private String vcfPath;
    /**
     * Variants having reciprocal overlap greater than this threshold are considered as a match. Threshold unit
     * is provided as percentage.
     */
    private double similarityThreshold;
    /**
     * Variants having frequency greater than this threshold are considered common. Threshold unit is provided
     * as percentage.
     */
    private double frequencyThreshold;
    /**
     * Set of variant datasets used to remove common variants not associated with Mendelian diseases.
     */
    private final Set<PopulationVariantOrigin> populationVariantOrigin = new HashSet<>();
    /**
     * Minimum number of reads supporting the ALT allele required to include the variant into the report.
     */
    private int minAltReadSupport;
    /**
     * Number of variants reported in HTML result. The variants are sorted by priority.
     */
    private int topNVariantsReported;
    /**
     * TAD must be present in at least this percentage of the tissues to be included in the analysis.
     * Threshold is provided as percentage.
     */
    private double tadStabilityThreshold;
    /**
     * Use Vista enhancer dataset in the analysis.
     */
    private boolean useVistaEnhancers;
    /**
     * Use FANTOM5 enhancer dataset in the analysis.
     */
    private boolean useFantom5Enhancers;
    /**
     * A measure of phenotype term similarity between proband and a disease. Currently supported measures are
     * Resnik symmetric similarity and Resnik asymmetric similarity.
     */
    private String phenotypeTermSimilarityMeasure;

    public String dataDirectory() {
        return dataDirectory;
    }

    public void setDataDirectory(String dataDirectory) {
        this.dataDirectory = dataDirectory;
    }

    public String phenopacketPath() {
        return phenopacketPath;
    }

    public void setPhenopacketPath(String phenopacketPath) {
        this.phenopacketPath = phenopacketPath;
    }

    public String vcfPath() {
        return vcfPath;
    }

    public void setVcfPath(String vcfPath) {
        this.vcfPath = vcfPath;
    }

    public double similarityThreshold() {
        return similarityThreshold;
    }

    public void setSimilarityThreshold(double similarityThreshold) {
        this.similarityThreshold = similarityThreshold;
    }

    public double frequencyThreshold() {
        return frequencyThreshold;
    }

    public void setFrequencyThreshold(double frequencyThreshold) {
        this.frequencyThreshold = frequencyThreshold;
    }

    public Set<PopulationVariantOrigin> populationVariantOrigins() {
        return populationVariantOrigin;
    }

    public void addAllPopulationVariantOrigins(Collection<PopulationVariantOrigin> populationVariantOrigin) {
        this.populationVariantOrigin.addAll(populationVariantOrigin);
    }

    public int minAltReadSupport() {
        return minAltReadSupport;
    }

    public void setMinAltReadSupport(int minAltReadSupport) {
        this.minAltReadSupport = minAltReadSupport;
    }

    public int topNVariantsReported() {
        return topNVariantsReported;
    }

    public void setTopNVariantsReported(int topNVariantsReported) {
        this.topNVariantsReported = topNVariantsReported;
    }

    public double tadStabilityThreshold() {
        return tadStabilityThreshold;
    }

    public void setTadStabilityThreshold(double tadStabilityThreshold) {
        this.tadStabilityThreshold = tadStabilityThreshold;
    }

    public boolean useVistaEnhancers() {
        return useVistaEnhancers;
    }

    public void setUseVistaEnhancers(boolean useVistaEnhancers) {
        this.useVistaEnhancers = useVistaEnhancers;
    }

    public boolean useFantom5Enhancers() {
        return useFantom5Enhancers;
    }

    public void setUseFantom5Enhancers(boolean useFantom5Enhancers) {
        this.useFantom5Enhancers = useFantom5Enhancers;
    }

    public String phenotypeTermSimilarityMeasure() {
        return phenotypeTermSimilarityMeasure;
    }

    public void setPhenotypeTermSimilarityMeasure(String phenotypeTermSimilarityMeasure) {
        this.phenotypeTermSimilarityMeasure = phenotypeTermSimilarityMeasure;
    }

}
