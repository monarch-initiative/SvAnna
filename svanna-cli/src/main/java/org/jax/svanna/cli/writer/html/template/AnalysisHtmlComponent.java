package org.jax.svanna.cli.writer.html.template;

import org.jax.svanna.cli.writer.html.AnalysisParameters;

public class AnalysisHtmlComponent {

    private final String html;

     AnalysisHtmlComponent(AnalysisParameters analysisParameters) {

         this.html = parametersTable(analysisParameters);
     }

    private String header(String caption) {
        return  "<table class=\"hpotable\">\n" +
                "<caption>" + caption + "</caption>" +
                "  <thead><tr>" +
                "<th>Parameter</th>" +
                "<th>Value</th>" +
                "</tr></thead>\n";
    }

    private String keyValueTableRow(String key, String value) {
        return "<tr><td>" + key + "</td><td>" + value + "</td></tr>\n";
    }

    private String keyValueTableRow(String key, double value) {
        return "<tr><td>" + key + "</td><td>" + String.format("%.1f", value) + "</td?</tr>\n";
    }

    private String keyValueTableRow(String key, int value) {
        return "<tr><td>" + key + "</td><td>" + value + "</td?</tr>\n";
    }

    private String keyValueTableRow(String key, boolean value) {
        return "<tr><td>" + key + "</td><td>" + (value ? "Yes": "No") + "</td?</tr>\n";
    }

    private String parametersTable(AnalysisParameters analysisParameters) {
        StringBuilder sb = new StringBuilder();
        sb.append(header("Analysis parameters"));
        sb.append(keyValueTableRow("Frequency threshold",analysisParameters.frequencyThreshold()));
        sb.append(keyValueTableRow("Minimum alt read support",analysisParameters.minAltReadSupport()));
        sb.append(keyValueTableRow("Phenotype Term Similarity Measure",analysisParameters.phenotypeTermSimilarityMeasure()));
        sb.append(keyValueTableRow("Similarity Threshold", analysisParameters.similarityThreshold()));
        sb.append(keyValueTableRow("Number of variants reported", analysisParameters.topNVariantsReported()));
        sb.append(keyValueTableRow("Include VISTA enhancer definitions?",  analysisParameters.useVistaEnhancers()));
        sb.append(keyValueTableRow("Include FANTOM5 enhancer definitions?",  analysisParameters.useFantom5Enhancers()));
        sb.append("</table>\n");
        sb.append("<p>Summary of analysis parameters used in the current run.</p>\n");

        return sb.toString();
    }

    public String getHtml() {
        return html;
    }
}
