package org.jax.svanna.cli.writer.html.template;

import org.jax.svanna.cli.writer.OutputOptions;
import org.jax.svanna.cli.writer.html.AnalysisParameters;
import org.monarchinitiative.phenol.ontology.data.Term;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

public class MetaDataHtmlComponent {
    private final String html;


    public MetaDataHtmlComponent(Collection<Term> originalHpoTerms, AnalysisParameters analysisParameters, OutputOptions outputOptions) {
        Map<TermId, String> originalTermMap = originalHpoTerms.stream()
                .collect(Collectors.toMap(Term::id, Term::getName));
        HpoHtmlComponent hpoHtmlComponent = new HpoHtmlComponent(originalTermMap);
        AnalysisHtmlComponent analysisHtmlComponent = new AnalysisHtmlComponent(analysisParameters, outputOptions);
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
