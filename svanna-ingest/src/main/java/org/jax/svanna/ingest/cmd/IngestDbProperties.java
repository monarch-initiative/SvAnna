package org.jax.svanna.ingest.cmd;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

@ConfigurationProperties(prefix = "svanna.ingest")
public class IngestDbProperties {

    private String repetitiveRegionsUrl;
    private String hg19toHg38ChainUrl;
    @NestedConfigurationProperty
    private EnhancerProperties enhancers;
    @NestedConfigurationProperty
    private VariantProperties variants;
    @NestedConfigurationProperty
    private PhenotypeProperties phenotype;
    @NestedConfigurationProperty
    private TadProperties tad;

    public String repetitiveRegionsUrl() {
        return repetitiveRegionsUrl;
    }

    public String hg19toHg38ChainUrl() {
        return hg19toHg38ChainUrl;
    }

    public void setHg19toHg38ChainUrl(String hg19toHg38ChainUrl) {
        this.hg19toHg38ChainUrl = hg19toHg38ChainUrl;
    }

    public TadProperties tad() {
        return tad;
    }

    public void setTad(TadProperties tad) {
        this.tad = tad;
    }

    public PhenotypeProperties phenotype() {
        return phenotype;
    }

    public void setPhenotype(PhenotypeProperties phenotype) {
        this.phenotype = phenotype;
    }

    public String getRepetitiveRegionsUrl() {
        return repetitiveRegionsUrl;
    }

    public void setRepetitiveRegionsUrl(String repetitiveRegionsUrl) {
        this.repetitiveRegionsUrl = repetitiveRegionsUrl;
    }

    public VariantProperties variants() {
        return variants;
    }

    public void setVariants(VariantProperties variants) {
        this.variants = variants;
    }

    public EnhancerProperties enhancers() {
        return enhancers;
    }

    public void setEnhancers(EnhancerProperties enhancers) {
        this.enhancers = enhancers;
    }

    @ConfigurationProperties(prefix = "svanna.ingest.enhancers")
    public static class EnhancerProperties {

        private String vista;

        private String fantomMatrix;

        private String fantomSample;

        public String vista() {
            return vista;
        }

        public void setVista(String vista) {
            this.vista = vista;
        }

        public String fantomMatrix() {
            return fantomMatrix;
        }

        public void setFantomMatrix(String fantomMatrix) {
            this.fantomMatrix = fantomMatrix;
        }

        public String fantomSample() {
            return fantomSample;
        }

        public void setFantomSample(String fantomSample) {
            this.fantomSample = fantomSample;
        }

    }

    @ConfigurationProperties(prefix = "svanna.ingest.variants")
    public static class VariantProperties {

        private String dgvUrl;
        private String gnomadSvRegionsUrl;
        private String gnomadSvVcfUrl;
        private String hgsvc2VcfUrl;

        public String hgsvc2VcfUrl() {
            return hgsvc2VcfUrl;
        }

        public void setHgsvc2VcfUrl(String hgsvc2VcfUrl) {
            this.hgsvc2VcfUrl = hgsvc2VcfUrl;
        }

        public String gnomadSvVcfUrl() {
            return gnomadSvVcfUrl;
        }

        public void setGnomadSvVcfUrl(String gnomadSvVcfUrl) {
            this.gnomadSvVcfUrl = gnomadSvVcfUrl;
        }

        public String gnomadSvRegionsUrl() {
            return gnomadSvRegionsUrl;
        }

        public void setGnomadSvRegionsUrl(String gnomadSvRegionsUrl) {
            this.gnomadSvRegionsUrl = gnomadSvRegionsUrl;
        }

        public String dgvUrl() {
            return dgvUrl;
        }

        public void setDgvUrl(String dgvUrl) {
            this.dgvUrl = dgvUrl;
        }
    }

    @ConfigurationProperties(prefix = "svanna.ingest.phenotype")
    public static class PhenotypeProperties {

        private String hpoOboUrl;
        private String hpoAnnotationsUrl;
        private String mim2geneMedgenUrl;
        private String geneInfoUrl;
        private String gencodeUrl;

        public String hpoOboUrl() {
            return hpoOboUrl;
        }

        public void setHpoOboUrl(String hpoOboUrl) {
            this.hpoOboUrl = hpoOboUrl;
        }

        public String hpoAnnotationsUrl() {
            return hpoAnnotationsUrl;
        }

        public void setHpoAnnotationsUrl(String hpoAnnotationsUrl) {
            this.hpoAnnotationsUrl = hpoAnnotationsUrl;
        }

        public String mim2geneMedgenUrl() {
            return mim2geneMedgenUrl;
        }

        public void setMim2geneMedgenUrl(String mim2geneMedgenUrl) {
            this.mim2geneMedgenUrl = mim2geneMedgenUrl;
        }

        public String geneInfoUrl() {
            return geneInfoUrl;
        }

        public void setGeneInfoUrl(String geneInfoUrl) {
            this.geneInfoUrl = geneInfoUrl;
        }

        public String gencodeUrl() {
            return gencodeUrl;
        }

        public void setGencodeUrl(String gencodeUrl) {
            this.gencodeUrl = gencodeUrl;
        }
    }

    @ConfigurationProperties(prefix = "svanna.tad.phenotype")
    public static class TadProperties {

        private String mcArthur2021Supplement;

        public String mcArthur2021Supplement() {
            return mcArthur2021Supplement;
        }

        public void setMcArthur2021Supplement(String mcArthur2021Supplement) {
            this.mcArthur2021Supplement = mcArthur2021Supplement;
        }

    }
}
