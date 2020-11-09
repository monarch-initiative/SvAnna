package org.jax.svann.viz;


import org.jax.svann.except.SvAnnRuntimeException;
import org.jax.svann.hpo.HpoDiseaseSummary;
import org.jax.svann.reference.*;
import org.jax.svann.reference.genome.Contig;
import org.jax.svann.viz.svg.DeletionSvgGenerator;
import org.jax.svann.viz.svg.InsertionSvgGenerator;
import org.jax.svann.viz.svg.SvSvgGenerator;
import org.jax.svann.viz.svg.TranslocationSvgGenerator;

import java.util.*;


/**
 * This class creates the HTML code for each prioritized structural variant that is shown in the output.
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


    /**
     * Display a list of diseases that are associated with genes that are affected by the structural variant.
     * Currently we show an unordered list
     * @param visualizable object representing the SV
     * @return HTML code that displays associated diseases
     */
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
     * Creates a link to the UCSCS browser that shows the posiution of the SV using a color highlight
     * @param hloc Location of (part of) the SV
     * @return an HTML link to the UCSC Genome browser
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

    private int getInsertionLength(Visualizable visualizable) {
        SequenceRearrangement rearrangement = visualizable.getRearrangement();
        // we assume that a valid insertion has one adjacency
        // we also assume that each breakend of the adjacency contains one contig that
        // represents the insertion using a non-canonical contig
        Breakend leftmostBreakend = rearrangement.getLeftmostBreakend();
        List<Adjacency> adjacencies = rearrangement.getAdjacencies();
        if (adjacencies.size() != 2) {
            throw new SvAnnRuntimeException("We expected 2 adjacencies for an insertion but got " + adjacencies.size());
        }
        Adjacency leftAdjacency = adjacencies.get(0);
        Contig rightOfLeft = leftAdjacency.getEnd().getContig();
        if (rightOfLeft.getId()>25) {
            return rightOfLeft.getLength();
        }
       return -1; // should never happen
    }

    String getSvgString(Visualizable visualizable) {
        List<CoordinatePair> coordinatePairs = visualizable.getRearrangement().getRegions();
        SvType svtype = visualizable.getRearrangement().getType();
        try {
            SvSvgGenerator gen;
            switch (svtype) {
                case DELETION:
                    gen = new DeletionSvgGenerator(visualizable.getTranscripts(), visualizable.getEnhancers(), coordinatePairs);
                    return gen.getSvg();
                case INSERTION:
                    int insertionLength = getInsertionLength(visualizable);
                    gen = new InsertionSvgGenerator(visualizable.getTranscripts(), visualizable.getEnhancers(), coordinatePairs, insertionLength);
                    return gen.getSvg();
                case TRANSLOCATION:
                    gen = new TranslocationSvgGenerator(visualizable.getRearrangement(), visualizable.getTranscripts(), visualizable.getEnhancers(), coordinatePairs);
                    return gen.getSvg();
                default:
                    System.err.println("[ERROR] SVG not implemented for type=" + svtype);
                    return "";

            }
        } catch (SvAnnRuntimeException e) {
            System.err.println("[ERROR] " + e.getLocalizedMessage());

        }
        return "";
    }


    private String getVariantRepresentation(Visualizable visualizable, List<HtmlLocation> locations) {
        SvType svtype = visualizable.getRearrangement().getType();
        HtmlLocation loc;
        switch (svtype) {
            case INSERTION:
                int len = getInsertionLength(visualizable);
                if (locations.size() != 1) {
                    throw new SvAnnRuntimeException("Was expecting one location for insertion but got " + locations.size());
                }
                loc = locations.get(0);
                return String.format("%s:%dind%dbp", loc.getChrom(), loc.getBegin(), len);
            case DELETION:
                if (locations.size() != 1) {
                    throw new SvAnnRuntimeException("Wasa expecting one location for deletion but got " + locations.size());
                }
                loc = locations.get(0);
                return String.format("%s:%d-%ddel", loc.getChrom(), loc.getBegin(), loc.getEnd());
        }

        return "TODO";
    }



    @Override
    public String getHtml(Visualizable visualizable) {
        List<HtmlLocation> locations = visualizable.getLocations();
        String variantString = getVariantRepresentation(visualizable,  locations );
        StringBuilder sb = new StringBuilder();
        sb.append("<h1>").append(variantString).append("</h1>\n");
        sb.append("<div class=\"row\">\n");
        sb.append("<div class=\"column\" style=\"background-color:#F8F8F8;\">\n");
        sb.append("<h2>Sequence</h2>\n");
        sb.append(getSequencePrioritization(visualizable)).append("\n");
        sb.append("</div>\n");
        sb.append("<div class=\"column\" style=\"background-color:#F0F0F0;\">\n");
        sb.append("<h2>Phenotypic data</h2>\n");
        sb.append(getPhenotypePrioritization(visualizable)).append("\n");
        sb.append("</div>\n");
        sb.append("</div>\n");
        String svg = getSvgString(visualizable);
        sb.append(svg);

        return sb.toString();
    }


    String getSequencePrioritization(Visualizable visualizable) {
        StringBuilder sb = new StringBuilder();
        List<HtmlLocation> locations = visualizable.getLocations();
        String variantString = getVariantRepresentation(visualizable,  locations );
        Set<String> vcfIdSet = new HashSet<>();
        SequenceRearrangement rearrangement = visualizable.getRearrangement();
        List<Adjacency> adjacencies = visualizable.getRearrangement().getAdjacencies();
        for (var a : adjacencies) {
            vcfIdSet.add(a.getStart().getContigName());
            vcfIdSet.add(a.getEnd().getContigName());
        }
        String idString = String.join(";", vcfIdSet);
        if (vcfIdSet.size()>1) {
            idString = String.format("IDs: %s", idString);
        } else {
            idString = String.format("ID: %s", idString);
        }
        sb.append("<p>").append(idString).append("</p>\n");
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
        return sb.toString();
    }

    String getPhenotypePrioritization(Visualizable visualizable) {
        String s = getUnorderedListWithDiseases(visualizable);
        return s;
    }
}
