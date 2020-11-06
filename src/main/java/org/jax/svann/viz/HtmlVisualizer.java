package org.jax.svann.viz;

import de.charite.compbio.jannovar.reference.GenomeInterval;
import de.charite.compbio.jannovar.reference.Strand;
import org.jax.svann.except.SvAnnRuntimeException;
import org.jax.svann.hpo.HpoDiseaseSummary;
import org.jax.svann.priority.SvImpact;
import org.jax.svann.priority.SvPriority;
import org.jax.svann.reference.*;
import org.jax.svann.reference.genome.Contig;
import org.jax.svann.viz.svg.DeletionSvgGenerator;
import org.jax.svann.viz.svg.SvSvgGenerator;

import java.util.*;
import java.util.stream.Collectors;

/**
 * To get started, lets just make a simple HTML table with no CSS or styles.
 */
public class HtmlVisualizer implements Visualizer {

    private final static String colors[] = {"F08080", "CCE5FF", "ABEBC6", "FFA07A", "C39BD3", "FEA6FF","F7DC6F", "CFFF98", "A1D6E2",
            "EC96A4", "E6DF44", "F76FDA","FFCCE5", "E4EA8C", "F1F76F", "FDD2D6", "F76F7F", "DAF7A6","FFC300" ,"F76FF5" , "FFFF99",
            "FF99FF", "99FFFF","CCFF99","FFE5CC","FFD700","9ACD32","7FFFD4","FFB6C1","FFFACD",
            "FFE4E1","F0FFF0","F0FFFF"};

    private final static String HTML_TABLE_HEADER = "<table>\n" +
            "  <thead>\n" +
            "    <tr>\n" +
            "      <th>Item</th>\n" +
            "      <th>Value</th>\n" +
            "    </tr>\n" +
            "  </thead>\n";


    public HtmlVisualizer() {

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


    private String getUnorderedListWithDiseases(Visualizable visualizable) {
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

    /**
     * TODO -- mark position of SV in color similar to
     * @param hloc
     * @return
     */
    String getUcscLink(HtmlLocation hloc) {
        String chrom = hloc.getChrom().startsWith("chr") ? hloc.getChrom() : "chr" + hloc.getChrom();
        int sVbegin = hloc.getBegin();
        int sVend = hloc.getEnd();
        // We want to exand the view -- how much depends on the size of the SV
        int len = sVend = sVbegin;
        if (len < 0) {
            // should never happen
            throw new SvAnnRuntimeException("[ERROR] Malformed Htmllocation: " + hloc);
        }
        int OFFSET;
        if (len < 100) {
            OFFSET = 5000;
        } else if (len < 1000) {
            OFFSET = len*5;
        } else if (len < 1000) {
            OFFSET = len*3;
        } else if (len < 10000) {
            OFFSET = len*2;
        } else {
            OFFSET = (int)(len*1.5);
        }
        int viewBegin = sVbegin - OFFSET;
        int viewEnd = sVend + OFFSET;
        String highlight = getHighlightRegion(chrom, sVbegin, sVend);
        String url = String.format("https://genome.ucsc.edu/cgi-bin/hgTracks?db=hg38&virtMode=0&position=%s%%3A%d-%d&%s",
                chrom, viewBegin, viewEnd, highlight);
        return String.format("<a href=\"%s\" target=\"__blank\">%s:%d-%d</a>",
                url,chrom, hloc.getBegin(), hloc.getEnd());
    }

    /**
     * Creates a string to show highlights. Nonselected regions are highlighted in very light grey.
     * @return something like this {@code highlight=<DB>.<CHROM>:<START>-<END>#<COLOR>}.
     * . */
    private String getHighlightRegion(String chromosome, int start, int end) {
        String genome = "hg38"; // TODO make flexible
        Random r = new Random();
        String color = colors[r.nextInt(colors.length)];
        String highlight = String.format("%s.%s%%3A%d-%d%s",
                        genome,
                        chromosome,
                        start,
                        end,
                        color);
        return String.format("highlight=%s", highlight);
    }


    /** These are the things to hide and show to get a nice hg19 image. */
    private String getURLFragmentHg38() {
        return "gc5Base=dense&snp150Common=hide&gtexGene=hide&dgvPlus=hide&pubs=hide&knownGene=hide&ncbiRefSeqView=pack&OmimAvSnp=hide";
    }

    @Override
    public String getHtml(Visualizable visualizable) {
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
        for (var olap : visualizable.getOverlaps()) {
            sb.append(olap.toString()).append("<br/>\n");
        }
        sb.append("</p>\n");
        sb.append(HTML_TABLE_HEADER);
        sb.append("<tbody>\n");
        sb.append(keyValueRow("Impact", visualizable.getImpact()));
        //sb.append(keyValueRow("Associated diseases", getUnorderedListWithDiseases()));
        sb.append("</table>");
        SvType svtype = visualizable.getRearrangement().getType();
        List<CoordinatePair> coordinatePairs = visualizable.getRearrangement().getRegions();
        SvSvgGenerator gen = new DeletionSvgGenerator(visualizable.getTranscripts(), visualizable.getEnhancers(),coordinatePairs);
        sb.append("<br/>\n").append(gen.getSvg());
        return sb.toString();
    }
}
