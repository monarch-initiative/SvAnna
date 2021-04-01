package org.jax.svanna.cli.writer.html.template;

import org.jax.svanna.cli.writer.html.AnalysisParameters;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.Map;

public class MetaDataHtmlComponent {
    private final String html;


    public MetaDataHtmlComponent(Map<TermId, String> originalHpoTerms, AnalysisParameters analysisParameters) {
        HpoHtmlComponent hpoHtmlComponent = new HpoHtmlComponent(originalHpoTerms);
        AnalysisHtmlComponent analysisHtmlComponent = new AnalysisHtmlComponent(analysisParameters);
        this.html = "<div class=\"row\">\n" +
                "<div class=\"column\">\n" +
                hpoHtmlComponent.getHtml() +
                "</div>\n" +
                "<div class=\"column\">\n" +
                analysisHtmlComponent.getHtml() +
                "</div>\n" +
                "</div>\n" +
                "<div class=\"row\">\n" +
                "<p>svanna performs simple phenotype-base prioritization by semantic similarity analysis (Resnik)," +
                " corresponding to the OSS approach in <a href=\"https://pubmed.ncbi.nlm.nih.gov/19800049/\" " +
                " target=\"_blank\">K&ouml;hler et al (2009)</a>.</p>\n" +
                "</div>\n";
    }

    public String getHtml() {
        return html;
    }
}
