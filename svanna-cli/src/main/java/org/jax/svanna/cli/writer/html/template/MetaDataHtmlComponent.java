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
                "</div>\n";
    }

    public String getHtml() {
        return html;
    }
}
