package org.jax.svann.viz;

import de.charite.compbio.jannovar.reference.GenomeInterval;
import de.charite.compbio.jannovar.reference.Strand;
import org.jax.svann.except.SvAnnRuntimeException;
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




    @Override
    public String getHtml() {
        StringBuilder sb = new StringBuilder();
        sb.append(HTML_TABLE_HEADER);
        sb.append("<tbody>\n");
        Map<String, String> locationMap = visualizable.getLocationStrings();
        for (var e : locationMap.entrySet()) {
            sb.append(keyValueRow(e.getKey(), e.getValue()));
        }
        sb.append(keyValueRow("Type", visualizable.getType()));
        sb.append(keyValueRow("Impact", visualizable.getImpact()));
        String relevant = this.visualizable.hasPhenotypicRelevance() ? "YES" : "NO";
        sb.append(keyValueRow("Clinically relevant", relevant));
        sb.append("</table>");

        return sb.toString();
    }
}
