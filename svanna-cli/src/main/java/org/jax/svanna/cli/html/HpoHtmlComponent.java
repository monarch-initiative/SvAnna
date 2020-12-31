package org.jax.svanna.cli.html;

import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HpoHtmlComponent {

    private final String html;

    public HpoHtmlComponent(Map<TermId, String> topLevelHpoTerms, Map<TermId, String> originalHpoTerms) {

        StringBuilder sb = new StringBuilder();
        sb.append(originalTermsTable(originalHpoTerms));
        sb.append(topLevelPara(topLevelHpoTerms));
        this.html = sb.toString();
    }

    private String topLevelPara(Map<TermId, String> topLevelHpoTerms) {
        StringBuilder sb = new StringBuilder();
        sb.append("<p>Prioritization is performed using the top-level HPO terms corresponding to the original HPO terms.</p>");
        for (var entry : topLevelHpoTerms.entrySet()) {
            sb.append("<p> ").append(hpoLabelLink(entry.getKey(), entry.getValue())).append("</p>");
        }

        return sb.toString();
    }

    private String header() {
        StringBuilder sb = new StringBuilder();
        sb.append("<table class=\"hpo\">\n");
        sb.append("<caption>HPO Terms</caption>");
        sb.append("  <thead><tr>");
        sb.append("<th>Term</th>");
        sb.append("<th>Id</th>");
        sb.append("</tr></thead>\n");
        return sb.toString();
    }

    private String hpoLink( TermId tid) {
        return String.format("<a href=\"https://hpo.jax.org/app/browse/term/%s\" target=\"_blank\">%s</a>)",
                tid.getValue(),tid.getValue());
    }

    private String hpoLabelLink( TermId tid, String label) {
        return String.format("<a href=\"https://hpo.jax.org/app/browse/term/%s\" target=\"_blank\">%s</a>)",
                tid.getValue(),label);
    }

    private String originalTermsTable(Map<TermId, String> originalHpoTerms) {
        StringBuilder sb = new StringBuilder();
        sb.append(header());
        for (var entry : originalHpoTerms.entrySet()) {
            sb.append("<tr><td>").append(entry.getValue()).append("</td><td>")
                    .append(hpoLink(entry.getKey())).append("</td></tr>\n");
        }
        sb.append("</table>\n");

        return sb.toString();
    }

    public String getHtml() {
        return html;
    }
}
