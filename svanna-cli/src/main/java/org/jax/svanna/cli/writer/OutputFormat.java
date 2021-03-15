package org.jax.svanna.cli.writer;

public enum OutputFormat {

    HTML(".html"),
    TSV(".tsv"),
    CSV(".csv"),
    VCF(".vcf");

    private final String fileSuffix;

    OutputFormat(String fileSuffix) {
        this.fileSuffix = fileSuffix;
    }

    public String fileSuffix() {
        return fileSuffix;
    }
}
