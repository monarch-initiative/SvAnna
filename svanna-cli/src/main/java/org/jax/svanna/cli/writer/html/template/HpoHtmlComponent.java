package org.jax.svanna.cli.writer.html.template;

import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.Map;

/**
 * This class provides some convenience functions for creating the HPO tables for the HTML output.
 */
public class HpoHtmlComponent {

    private final String html;

    public HpoHtmlComponent(Map<TermId, String> originalHpoTerms) {
        this.html = "<div class=\"row\">\n" +
                "<div class=\"column\">\n" +
                originalTermsTable(originalHpoTerms) +
                "</div>\n" +
                "</div>\n";
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

    private String originalTermsTable(Map<TermId, String> originalHpoTerms) {
        StringBuilder sb = new StringBuilder();
        sb.append(header("HPO terms"));
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
