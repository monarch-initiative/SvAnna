package org.jax.svann.viz;

import de.charite.compbio.jannovar.reference.GenomeInterval;
import de.charite.compbio.jannovar.reference.Strand;
import org.jax.svann.except.SvAnnRuntimeException;
import org.jax.svann.hpo.HpoDiseaseSummary;
import org.jax.svann.priority.SvImpact;
import org.jax.svann.priority.SvPriority;
import org.jax.svann.reference.Adjacency;
import org.jax.svann.reference.Breakend;
import org.jax.svann.reference.SequenceRearrangement;
import org.jax.svann.reference.SvType;
import org.jax.svann.reference.genome.Contig;

import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * To get started, lets just make a simple HTML table with no CSS or styles.
 */
public class HtmlVisualizer implements Visualizer {

    private final static String HTML_TABLE_HEADER = "<table>\n" +
            "  <thead>\n" +
            "    <tr>\n" +
            "      <th>Item</th>\n" +
            "      <th>Value</th>\n" +
            "    </tr>\n" +
            "  </thead>\n";


    private final Visualizable visualizable;

    public HtmlVisualizer(Visualizable vis) {
        this.visualizable = vis;
    }


    private String keyValueRow(String key, String value) {
        return String.format("<tr><td>%s</td><td>%s</td></tr>\n", key, value);
    }



    /**
     * This method decides if we can ask for a single line location string.
     * If we need to return two strings (e.g., a translocation) or more, it returns false
     * @return
     */
    private boolean requiresSingleLocationRow() {
        return true;
    }


    String getUnorderedListWithDiseases() {
        List<HpoDiseaseSummary> diseases = visualizable.getDiseaseSummaries();
        if (diseases.isEmpty()) {
            return "n/a";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("<ul>\n");
        for (var disease: visualizable.getDiseaseSummaries()) {
            String url = String.format("<a href=\"https://hpo.jax.org/app/browse/disease/%s\" target=\"__blank\">%s (%s)</a>",
                    disease.getDiseaseId(), disease.getDiseaseName(), disease.getDiseaseId());
            sb.append("<li>").append(url).append("</li>\n");
        }
        sb.append("</ul>\n");
        return sb.toString();
    }

    String getUcscLink(HtmlLocation hloc) {
        String chrom = hloc.getChrom().startsWith("chr") ? hloc.getChrom() : "chr" + hloc.getChrom();
        String url = String.format("https://genome.ucsc.edu/cgi-bin/hgTracks?db=hg38&virtMode=0&position=%s%%3A%d-%d",
                chrom, hloc.getBegin(), hloc.getEnd());
        return String.format("<a href=\"%s\" target=\"__blank\">%s:%d-%d</a>",
                url,chrom, hloc.getBegin(), hloc.getEnd());
    }

    @Override
    public String getHtml() {
        StringBuilder sb = new StringBuilder();
        List<HtmlLocation> locations = visualizable.getLocations();
        sb.append("<p>").append(visualizable.getType()).append("<br/>");
        if (locations.isEmpty()) {
            sb.append("ERROR - could not retrieve location(s) of structural variant</p>\n");
        } else if (locations.size() == 1) {
            sb.append(getUcscLink(locations.get(0))).append("</p>");
        } else {
            sb.append("<ul>\n");
            for (var loc : locations) {
                sb.append("<li>").append(getUcscLink(loc)).append("</li>\n");
            }
            sb.append("</ul></p>\n");
        }
        sb.append("<p>");
        for (var olap : this.visualizable.getOverlaps()) {
            sb.append(olap.toString()).append("<br/>\n");
        }
        sb.append("</p>\n");
        sb.append(HTML_TABLE_HEADER);
        sb.append("<tbody>\n");
//        Map<String, String> locationMap = visualizable.getLocationStrings();
//        for (var e : locationMap.entrySet()) {
//            sb.append(keyValueRow(e.getKey(), e.getValue()));
//        }
        sb.append(keyValueRow("Impact", visualizable.getImpact()));
        sb.append(keyValueRow("Associated diseases", getUnorderedListWithDiseases()));
        sb.append("</table>");

        return sb.toString();
    }
}
