package org.jax.svanna.cli.writer.html.template;

import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.Map;

/**
 * This class provides some convenience functions for creating the HPO tables for the HTML output.
 */
public class HpoHtmlComponent {

    private final String html;

    public HpoHtmlComponent(Map<TermId, String> originalHpoTerms) {

        String sb = "<div class=\"row\">\n" +
                "<div class=\"column\">\n" +
                originalTermsTable(originalHpoTerms) +
                "</div>\n" +
                "</div>\n" +
                "<div class=\"row\">\n" +
                "<p>svanna performs simple phenotype-base prioritization by semantic similarity analysis (Resnik)," +
                " corresponding to the OSS approach in <a href=\"https://pubmed.ncbi.nlm.nih.gov/19800049/\" " +
                " target=\"_blank\">K&ouml;hler et al (2009)</a>.</p>\n" +
                "</div>\n";
        this.html = sb;
    }

    private String topLevelTerms(Map<TermId, String> topLevelHpoTerms) {
        StringBuilder sb = new StringBuilder();
        sb.append(header("Top-level terms"));
        for (var entry : topLevelHpoTerms.entrySet()) {
            sb.append("<tr><td>").append(entry.getValue()).append("</td><td>")
                    .append(hpoLink(entry.getKey())).append("</td></tr>\n");
        }
        sb.append("</table>\n");
        sb.append("<p>svanna performs simple phenotype-base prioritization by semantic similarity analysis (Resnik),");
        sb.append(" corresponding to the OSS approach in <a href=\"https://pubmed.ncbi.nlm.nih.gov/19800049/\" ");
        sb.append(" target=\"_blank\">K&ouml;hler et al (2009)</a></p>\n");
        return sb.toString();
    }

    private String header(String caption) {
        return  "<table class=\"hpotable\">\n" +
                "<caption>" + caption + "</caption>" +
                "  <thead><tr>" +
                "<th>Term</th>" +
                "<th>Id</th>" +
                "</tr></thead>\n";
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
