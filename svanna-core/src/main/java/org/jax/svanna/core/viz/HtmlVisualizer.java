package org.jax.svanna.core.viz;


import org.jax.svanna.core.exception.SvAnnRuntimeException;
import org.jax.svanna.core.hpo.HpoDiseaseSummary;
import org.jax.svanna.core.reference.Enhancer;
import org.jax.svanna.core.reference.SvannaVariant;
import org.jax.svanna.core.reference.Transcript;
import org.jax.svanna.core.reference.Zygosity;
import org.jax.svanna.core.viz.svg.*;
import org.monarchinitiative.variant.api.Breakended;
import org.monarchinitiative.variant.api.Contig;
import org.monarchinitiative.variant.api.VariantType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;


/**
 * This class creates the HTML code for each prioritized structural variant that is shown in the output.
 */
public class HtmlVisualizer implements Visualizer {

    private static final Logger LOGGER = LoggerFactory.getLogger(HtmlVisualizer.class);

    /** Many SV callers pick up very large deletions/duplications that are almost certainly artifacts. We do not want
     * to print detailed information for these. We user an arbitrary threshold -- if we have more than this many
     * genes, we do not show details.
     */
    private final static int THRESHOLD_GENE_COUNT_TO_SUPPRESS_DETAILS = 100;

    private final static String[] colors = {"F08080", "CCE5FF", "ABEBC6", "FFA07A", "C39BD3", "FEA6FF", "F7DC6F", "CFFF98", "A1D6E2",
            "EC96A4", "E6DF44", "F76FDA", "FFCCE5", "E4EA8C", "F1F76F", "FDD2D6", "F76F7F", "DAF7A6", "FFC300", "F76FF5", "FFFF99",
            "FF99FF", "99FFFF", "CCFF99", "FFE5CC", "FFD700", "9ACD32", "7FFFD4", "FFB6C1", "FFFACD",
            "FFE4E1", "F0FFF0", "F0FFFF"};

    private final static String EMPTY_STRING = "";

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
     * Creates a link to the UCSCS browser that shows the posiution of the SV using a color highlight
     *
     * @param hloc Location of (part of) the SV
     * @return an HTML link to the UCSC Genome browser
     */
    String getUcscLink(HtmlLocation hloc) {
        String chrom = hloc.getChrom().startsWith("chr") ? hloc.getChrom() : "chr" + hloc.getChrom();
        int sVbegin = hloc.getBegin();
        int sVend = hloc.getEnd();
        // We want to exand the view -- how much depends on the size of the SV
        int len = sVend - sVbegin;
        if (len < 0) {
            // should never happen
            throw new SvAnnRuntimeException("[ERROR] Malformed Htmllocation: " + hloc);
        }
        int OFFSET;
        if (len<2) {
            // insertion
            OFFSET = 15;
        } else if (len < 10000) {
            OFFSET = len * 2;
        } else {
            OFFSET = (int) (len * 1.5);
        }
        int viewBegin = sVbegin - OFFSET;
        int viewEnd = sVend + OFFSET;
        String highlight = getHighlightRegion(chrom, sVbegin, sVend);
        String url = String.format("https://genome.ucsc.edu/cgi-bin/hgTracks?db=hg38&virtMode=0&position=%s%%3A%d-%d&%s",
                chrom, viewBegin, viewEnd, highlight);
        return String.format("<a href=\"%s\" target=\"__blank\">%s:%d-%d</a>",
                url, chrom, hloc.getBegin(), hloc.getEnd());
    }

    /**
     * Creates a string to show highlights. Nonselected regions are highlighted in very light grey.
     *
     * @return something like this {@code highlight=<DB>.<CHROM>:<START>-<END>#<COLOR>}.
     * .
     */
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


    /**
     * These are the things to hide and show to get a nice hg19 image.
     */
    private String getURLFragmentHg38() {
        return "gc5Base=dense&snp150Common=hide&gtexGene=hide&dgvPlus=hide&pubs=hide&knownGene=hide&ncbiRefSeqView=pack&OmimAvSnp=hide";
    }


    String getSvgString(Visualizable visualizable) {
        SvannaVariant variant = visualizable.variant();
        if (visualizable.getGeneCount() > 10) {
            return EMPTY_STRING;
        }
        try {
            SvSvgGenerator gen;
            switch (variant.variantType().baseType()) {
                case DEL:
                    gen = new DeletionSvgGenerator(variant, visualizable.transcripts(), visualizable.enhancers());
                    break;
                case INS:
                    int insertionLength = variant.changeLength();
                    gen = new InsertionSvgGenerator(variant, visualizable.transcripts(), visualizable.enhancers());
                    break;
                case INV:
                    gen = new InversionSvgGenerator(variant, visualizable.transcripts(), visualizable.enhancers());
                    break;
                case TRA:
                case BND:
                    if (variant instanceof Breakended) {
                        gen = new TranslocationSvgGenerator(variant, (Breakended) variant, visualizable.transcripts(), visualizable.enhancers());
                        break;
                    }
                    // fall through to default
                case DUP:
                    gen = new DuplicationSvgGenerator(variant, visualizable.transcripts(), visualizable.enhancers());
                    break;
                default:
                    LOGGER.warn("SVG not implemented for type {}", variant.variantType());
                    return "";
            }
            return gen.getSvg();
        } catch (SvAnnRuntimeException e) {
            LOGGER.warn("Error: {}", e.getMessage());
            return "";
        }
    }


    private String getLengthDisplayString(int begin, int end) {
        int len = end - begin;
        if (len < 1)
            return "n/a";
        else if (len < 1000) {
            return String.format("%d bp", len);
        } else if (len < 1_000_000) {
            double kb = (double) len / 1000.0;
            return String.format("%.2f kb", kb);
        } else {
            double mb = (double) len / 1000000.0;
            return String.format("%.2f Mb", mb);
        }
    }

    private String getVariantRepresentation(Visualizable visualizable, List<HtmlLocation> locations) {
        SvannaVariant variant = visualizable.variant();
        VariantType svtype = variant.variantType().baseType();
        HtmlLocation loc;
        switch (svtype) {
            case INS:
                int len = variant.changeLength();
                if (locations.size() != 1) {
                    throw new SvAnnRuntimeException("Was expecting one location for insertion but got " + locations.size());
                }
                loc = locations.get(0);
                return String.format("%s:%dins%dbp", loc.getChrom(), loc.getBegin(), len);
            case DEL:
                if (locations.size() != 1) {
                    throw new SvAnnRuntimeException("Was expecting one location for deletion but got " + locations.size());
                }
                loc = locations.get(0);
                String lend = getLengthDisplayString(loc.getBegin(), loc.getEnd());
                return String.format("%s:%d-%ddel (%s)", loc.getChrom(), loc.getBegin(), loc.getEnd(), lend);
            case TRA:
                if (locations.size() != 2) {
                    throw new SvAnnRuntimeException("Was expecting two locations for translocation but got " + locations.size());
                }
                HtmlLocation locA = locations.get(0);
                HtmlLocation locB = locations.get(1);
                String translocationA = String.format("%s:%d", locA.getChrom(), locA.getBegin());
                String translocationB = String.format("%s:%d", locB.getChrom(), locB.getBegin());
                return String.format("t(%s, %s)", translocationA, translocationB);
            case DUP:
                if (locations.size() != 1) {
                    throw new SvAnnRuntimeException("Was expecting one location for duplication but got " + locations.size());
                }
                HtmlLocation dupLoc = locations.get(0);
                int dupBegin = Math.min(dupLoc.getBegin(), dupLoc.getEnd());
                int dupEnd = Math.max(dupLoc.getBegin(), dupLoc.getEnd());
                String lengthDup = getLengthDisplayString(dupBegin, dupEnd);
                return String.format("%s:%d-%d duplication (%s)", dupLoc.getChrom(), dupBegin, dupEnd, lengthDup);
            case INV:
                if (locations.size() != 1) {
                    throw new SvAnnRuntimeException("Was expecting one location for inversion but got " + locations.size());
                }
                HtmlLocation invLoc = locations.get(0);
                int invBegin = Math.min(invLoc.getBegin(), invLoc.getEnd());
                int invEnd = Math.max(invLoc.getBegin(), invLoc.getEnd());
                String lengthInv = getLengthDisplayString(invBegin, invEnd);
                return String.format("inv(%s)(%d; %d) (%s)", invLoc.getChrom(), invBegin, invEnd, lengthInv);
        }

        return "TODO";
    }


    @Override
    public String getHtml(Visualizable visualizable) {
        List<HtmlLocation> locations = visualizable.locations();
        String variantString = getVariantRepresentation(visualizable, locations);
        String predImpact = String.format("Predicted impact: %s", visualizable.getImpact());
        Zygosity zygosity = visualizable.variant().zygosity();
        String zygo = zygosity.equals(Zygosity.UNKNOWN) ?
                "[unknown genotype]" :
                String.format("[%s]", zygosity.name().toLowerCase());
        StringBuilder sb = new StringBuilder();
        sb.append("<h1>").append(variantString).append(" &emsp; ").append(predImpact)
                .append(" &emsp; ").append(zygo).append("</h1>\n");
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

    String getEnhancerSummary(Enhancer e) {
       Contig contig = e.contig();
       String chrom = contig.ucscName();
       String tissueLabel = String.format("%s; tau %.2f", e.tissueLabel(), e.tau());
       return String.format("%s:%d-%d [%s]", chrom, e.start(), e.end(), tissueLabel);
    }


    String getSequencePrioritization(Visualizable visualizable) {
        SvannaVariant variant = visualizable.variant();
        if (visualizable.getGeneCount() > 2 &&
                (variant.variantType().baseType() == VariantType.DEL ||
                        variant.variantType().baseType() == VariantType.DUP)) {
            return getMultigeneSequencePrioritization(visualizable);
        }
        StringBuilder sb = new StringBuilder();
        int minSequenceDepth = variant.minDepthOfCoverage();
        List<HtmlLocation> locations = visualizable.locations();
        String idString = variant.id();

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
        sb.append("<p>Minimum sequence depth: ").append(minSequenceDepth).append(".</p>\n");
        for (var olap : visualizable.overlaps()) {
            sb.append(olap.toString()).append("<br/>\n");
        }
        sb.append("</p>\n");
        List<Enhancer> enhancers = visualizable.enhancers();
        if (enhancers.isEmpty()) {
            sb.append("<p>No enhancers found within genomic window.</p>\n");
        } else {
            sb.append("<p>Enhancers within genomic window:</p>\n<ul>");
            for (var e : visualizable.enhancers()) {
                sb.append("<li>").append(getEnhancerSummary(e)).append("</li>\n");
            }
            sb.append("</ul>\n");
        }
        return sb.toString();
    }

    /**
     * Use this to show the visualization of structural variants that affect more than two different genes.
     * In this case, it is very probably a large deletion. There is no use in displaying all of the
     * transcripts that are affected as we do for single-gene or two-gene deletions. Instead, we just assume that
     * there is a null mutation for all of the genes affected by the SV and we list them.
     *
     * @param visualizable
     * @return
     */
    private String getMultigeneSequencePrioritization(Visualizable visualizable) {
        StringBuilder sb = new StringBuilder();
        List<HtmlLocation> locations = visualizable.locations();
        //String variantString = getVariantRepresentation(visualizable,  locations );
        String idString = visualizable.variant().id();

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

        // get list of genes
        List<String> genes = visualizable.transcripts()
                .stream()
                .map(Transcript::hgvsSymbol)
                .distinct()
                .collect(Collectors.toList());
        // show up to ten affected genes -- if there are more just show the count
        if (genes.size() > 10) {
            sb.append("<p>").append(genes.size()).append(" affected genes</p>\n");
        } else {
            sb.append("<p>Affected genes: <br/><ol>");
            for (var g : genes) {
                sb.append("<li>").append(g).append("</li>\n");
            }
            sb.append("</ol></p>\n");
        }
        return sb.toString();
    }


    private String getEnhancerPrioritizationHtml(Visualizable visualizable) {
        List<Enhancer> enhancerList = visualizable.enhancers();
        if (enhancerList.isEmpty()) {
            return "";
        } else  if (visualizable.getGeneCount() > THRESHOLD_GENE_COUNT_TO_SUPPRESS_DETAILS) {
            return String.format("<p>Total of %d relevant enhancers associated this variant.</p>\n",
                    visualizable.enhancers().size());
        } else if (enhancerList.size() > 10) {
            return String.format("<p>Total of %d enhancers associated this variant.</p>\n",
                    enhancerList.size());
        }
        StringBuilder sb = new StringBuilder();
        sb.append("<ul>\n");
        for (Enhancer e : enhancerList) {
            sb.append("<li>").append(getEnhancerSummary(e)).append("</li>\n");
        }
        sb.append("</ul>\n");
        return sb.toString();
    }

    /**
     * Display a list of diseases that are associated with genes that are affected by the structural variant.
     * Currently we show an unordered list
     *
     * @param visualizable object representing the SV
     * @return HTML code that displays associated diseases
     */
    private String getDiseaseGenePrioritizationHtml(Visualizable visualizable) {
        List<HpoDiseaseSummary> diseases = visualizable.diseaseSummaries();
        if (diseases.isEmpty()) {
            return "n/a";
        } else if (visualizable.getGeneCount() > THRESHOLD_GENE_COUNT_TO_SUPPRESS_DETAILS) {
            return String.format("<p>Total of %d relevant diseases associated with genes in this variant.</p>\n",
                    visualizable.diseaseSummaries().size());
        }
        StringBuilder sb = new StringBuilder();
        sb.append("<ul>\n");
        for (var disease : visualizable.diseaseSummaries()) {
            String url = String.format("<a href=\"https://hpo.jax.org/app/browse/disease/%s\" target=\"__blank\">%s (%s)</a>",
                    disease.getDiseaseId(), disease.getDiseaseName(), disease.getDiseaseId());
            sb.append("<li>").append(url).append("</li>\n");
        }
        sb.append("</ul>\n");
        return sb.toString();
    }

    String getPhenotypePrioritization(Visualizable visualizable) {
        String diseases = getDiseaseGenePrioritizationHtml(visualizable);
        String enhancers = getEnhancerPrioritizationHtml(visualizable);
        return String.format("%s<br />%s", diseases, enhancers);
    }
}
