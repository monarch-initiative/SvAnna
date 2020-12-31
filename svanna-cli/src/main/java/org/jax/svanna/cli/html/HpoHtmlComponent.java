package org.jax.svanna.cli.html;

import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.Map;

public class HpoHtmlComponent {

    private final String html;

    public HpoHtmlComponent(Map<TermId, String> topLevelHpoTerms, Map<TermId, String> originalHpoTerms) {

        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"row\">\n");
        sb.append("<div class=\"column\">\n");
        sb.append(originalTermsTable(originalHpoTerms));
        sb.append("</div>\n");
        sb.append("<div class=\"column\">\n");
        sb.append(topLevelTerms(topLevelHpoTerms));
        sb.append("</div>\n");
        sb.append("</div>\n");
        this.html = sb.toString();
    }

    private String topLevelTerms(Map<TermId, String> topLevelHpoTerms) {
        StringBuilder sb = new StringBuilder();
        sb.append(header("Top-level terms"));
        for (var entry : topLevelHpoTerms.entrySet()) {
            sb.append("<tr><td>").append(entry.getValue()).append("</td><td>")
                    .append(hpoLink(entry.getKey())).append("</td></tr>\n");
        }
        sb.append("</table>\n");
        sb.append("<p>The top-level terms are the general (ancestor) terms to which the observed HPO terms belong");
        sb.append(" svanna performs simple phenotype-base prioritization by matching diseases with HPO annotations that ");
        sb.append(" match any of these top-level terms</p>\n");
        return sb.toString();
    }

    private String header(String caption) {
        StringBuilder sb = new StringBuilder();
        sb.append("<table class=\"hpotable\">\n");
        sb.append("<caption>").append(caption).append("</caption>");
        sb.append("  <thead><tr>");
        sb.append("<th>Term</th>");
        sb.append("<th>Id</th>");
        sb.append("</tr></thead>\n");
        return sb.toString();
    }

    private String hpoLink( TermId tid) {
        return String.format("<a href=\"https://hpo.jax.org/app/browse/term/%s\" target=\"_blank\">%s</a>",
                tid.getValue(),tid.getValue());
    }

    private String hpoLabelLink( TermId tid, String label) {
        return String.format("<a href=\"https://hpo.jax.org/app/browse/term/%s\" target=\"_blank\">%s</a>",
                tid.getValue(),label);
    }

    private String originalTermsTable(Map<TermId, String> originalHpoTerms) {
        StringBuilder sb = new StringBuilder();
        sb.append(header("Original HPO terms"));
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
