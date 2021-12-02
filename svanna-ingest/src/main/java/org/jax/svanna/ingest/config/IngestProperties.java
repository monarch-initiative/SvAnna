package org.jax.svanna.ingest.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

@ConfigurationProperties(prefix = "svanna.ingest")
public class IngestProperties {

    private String repetitiveRegionsUrl;
    private String hg19toHg38ChainUrl;
    private String ncbiGeneToHgnc;
    @NestedConfigurationProperty
    private EnhancerProperties enhancers;
    @NestedConfigurationProperty
    private VariantProperties variants;
    @NestedConfigurationProperty
    private PhenotypeProperties phenotype;
    @NestedConfigurationProperty
    private TadProperties tad;
    @NestedConfigurationProperty
    private GeneDosageProperties dosage;
    @NestedConfigurationProperty
    private GeneProperties genes;

    public String repetitiveRegionsUrl() {
        return repetitiveRegionsUrl;
    }

    public String hg19toHg38ChainUrl() {
        return hg19toHg38ChainUrl;
    }

    public void setHg19toHg38ChainUrl(String hg19toHg38ChainUrl) {
        this.hg19toHg38ChainUrl = hg19toHg38ChainUrl;
    }

    public String getRepetitiveRegionsUrl() {
        return repetitiveRegionsUrl;
    }

    public void setRepetitiveRegionsUrl(String repetitiveRegionsUrl) {
        this.repetitiveRegionsUrl = repetitiveRegionsUrl;
    }

    public String ncbiGeneToHgnc() {
        return ncbiGeneToHgnc;
    }

    public void setNcbiGeneToHgnc(String ncbiGeneToHgnc) {
        this.ncbiGeneToHgnc = ncbiGeneToHgnc;
    }

    public EnhancerProperties enhancers() {
        return enhancers;
    }

    public void setEnhancers(EnhancerProperties enhancers) {
        this.enhancers = enhancers;
    }
    
    public VariantProperties variants() {
        return variants;
    }

    public void setVariants(VariantProperties variants) {
        this.variants = variants;
    }

    public PhenotypeProperties phenotype() {
        return phenotype;
    }

    public void setPhenotype(PhenotypeProperties phenotype) {
        this.phenotype = phenotype;
    }


    public TadProperties tad() {
        return tad;
    }

    public void setTad(TadProperties tad) {
        this.tad = tad;
    }

    public GeneDosageProperties getDosage() {
        return dosage;
    }

    public void setDosage(GeneDosageProperties dosage) {
        this.dosage = dosage;
    }

    public GeneProperties getGenes() {
        return genes;
    }

    public void setGenes(GeneProperties genes) {
        this.genes = genes;
    }

}
