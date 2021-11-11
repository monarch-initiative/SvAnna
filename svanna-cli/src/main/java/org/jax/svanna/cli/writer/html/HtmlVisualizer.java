package org.jax.svanna.cli.writer.html;


import org.jax.svanna.cli.writer.html.svg.*;
import org.jax.svanna.core.SvAnnaRuntimeException;
import org.jax.svanna.core.overlap.GeneOverlap;
import org.jax.svanna.core.overlap.TranscriptOverlap;
import org.jax.svanna.core.reference.SvannaVariant;
import org.jax.svanna.core.reference.Zygosity;
import org.jax.svanna.model.HpoDiseaseSummary;
import org.jax.svanna.model.landscape.enhancer.Enhancer;
import org.jax.svanna.model.landscape.enhancer.EnhancerTissueSpecificity;
import org.monarchinitiative.phenol.ontology.data.Term;
import org.monarchinitiative.svart.BreakendVariant;
import org.monarchinitiative.svart.VariantType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.ielis.silent.genes.model.Gene;

import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;


/**
 * This class creates the HTML code for each prioritized structural variant that is shown in the output. The class
 * works in concert with the freemarker template to create the various HTML display elements.
 * @author Peter N Robinson
 */
public class HtmlVisualizer implements Visualizer {
    private static final Logger LOGGER = LoggerFactory.getLogger(HtmlVisualizer.class);

    /** Many SV callers pick up very large deletions/duplications that are almost certainly artifacts. We do not want
     * to print detailed information for these. We user an arbitrary threshold -- if we have more than this many
     * genes, we do not show details.
     */
    private final static int THRESHOLD_GENE_COUNT_TO_SUPPRESS_DETAILS = 100;
    /**
     * For SVs that affect multiple genes, the amount of information shown can grow considerably. Therefore, for
     * SVs that have this number of genes or more we show an abbreviated view (e.g., we do not list all transcripts
     * in a table, we just list all gene symbols).
     */
    private final static int THRESHOLD_COUNT_TO_SHOW_MULTIGENE_DISPLAY = 3;

    /** Pattern to format genomic positions with commas (e.g., transform 113236378 to 113,236,378). */
    private final DecimalFormat decimalFormat = new DecimalFormat("###,###.###");
    /** Colors that we use to create areas of colored highlight in the UCSC Browser. */
    private final static String[] colors = {"F08080", "CCE5FF", "ABEBC6", "FFA07A", "C39BD3", "FEA6FF", "F7DC6F", "CFFF98", "A1D6E2",
            "EC96A4", "E6DF44", "F76FDA", "FFCCE5", "E4EA8C", "F1F76F", "FDD2D6", "F76F7F", "DAF7A6", "FFC300", "F76FF5", "FFFF99",
            "FF99FF", "99FFFF", "CCFF99", "FFE5CC", "FFD700", "9ACD32", "7FFFD4", "FFB6C1", "FFFACD",
            "FFE4E1", "F0FFF0", "F0FFFF"};

    private final static String EMPTY_STRING = "";

    public HtmlVisualizer() {
    }


    @Override
    public String getHtml(Visualizable visualizable) {
        int totalAffectedGeneCount = visualizable.getGeneCount();
        if (totalAffectedGeneCount > THRESHOLD_GENE_COUNT_TO_SUPPRESS_DETAILS) {
            return getMultigeneSequencePrioritization(visualizable);
        }
        StringBuilder sb = new StringBuilder();

        List<HtmlLocation> locations = visualizable.locations();
        String variantString = getVariantRepresentation(visualizable, locations);
        sb.append("<h1>").append(variantString);

        String predImpact = String.format("Priority: %.2f", visualizable.variant().svPriority().getPriority());
        sb.append(" &emsp; ").append(predImpact);

        Zygosity zygosity = visualizable.variant().zygosity();
        String zygo = zygosity.equals(Zygosity.UNKNOWN)
                ? "[unknown genotype]"
                : String.format("[%s]", zygosity.name().toLowerCase());
        sb.append(" &emsp; ").append(zygo).append("</h1>\n");
        sb.append("<div class=\"row\">\n");
        sb.append("<div class=\"column\" style=\"background-color:#F8F8F8;\">\n");
        sb.append(getSequencePrioritization(visualizable)).append("\n");
        sb.append("</div>\n");
        sb.append("<div class=\"column\" style=\"background-color:#F0F0F0;\">\n");
        sb.append(getOverlapSummary(visualizable)).append("\n");
        sb.append("</div>\n");
        sb.append("</div>\n");
        String svg = getSvgString(visualizable);
        sb.append(svg);

        return sb.toString();
    }

    /**
     * Creates a link to the UCSCS browser that shows the position of the SV using a color highlight
     *
     * @param hloc Location of (part of) the SV
     * @return an HTML link to the UCSC Genome browser
     */
    private String getUcscLink(HtmlLocation hloc) {
        final String hg38ucsc = "gc5Base=dense&snp150Common=hide&gtexGene=hide&dgvPlus=hide&pubs=hide&knownGene=hide&ncbiRefSeqView=pack&OmimAvSnp=hide";
        String chrom = hloc.getChrom().startsWith("chr") ? hloc.getChrom() : "chr" + hloc.getChrom();
        int sVbegin = hloc.getBegin();
        int sVend = hloc.getEnd();
        // We want to exand the view -- how much depends on the size of the SV
        int len = sVend - sVbegin;
        if (len < 0) {
            // should never happen
            throw new SvAnnaRuntimeException("[ERROR] Malformed Htmllocation: " + hloc);
        }
        int OFFSET;
        if (len<2) {
            // insertion
            OFFSET = 15;
        } else if (len < 100) {
            OFFSET = len + (int)(0.3*len);
        } else if (len < 500) {
            OFFSET = len + (int)(0.2*len);
        } else {
            OFFSET = len + (int)(0.1*len);
        }
        int viewBegin = sVbegin - OFFSET;
        int viewEnd = sVend + OFFSET;
        String highlight = getHighlightRegion(chrom, sVbegin, sVend);
        String url = String.format("https://genome.ucsc.edu/cgi-bin/hgTracks?db=hg38&virtMode=0&position=%s%%3A%d-%d&%s&%s",
                chrom, viewBegin, viewEnd, highlight, hg38ucsc);
       String startString = decimalFormat.format(hloc.getBegin());
       String endString = decimalFormat.format(hloc.getEnd());
        chrom = chrom.startsWith("chr") ? chrom : "chr" + chrom;
        return String.format("<a href=\"%s\" target=\"_blank\">%s:%s-%s</a>",
                url, chrom, startString, endString);
    }


    /**
     * Creates a string to show highlights. Nonselected regions are highlighted in very light grey.
     * @return something like this {@code highlight=<DB>.<CHROM>:<START>-<END>#<COLOR>}.
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
     * Get a linkl to the nice GeneCards database to show information about a gene
     * @param symbol A gene symbol
     * @return HTML link to corresponding GeneCards page
     */
    private String getGeneCardsLink(String symbol) {
        String base = "https://www.genecards.org/cgi-bin/carddisp.pl?gene=";
        String url = base + symbol;
        return String.format("<a href=\"%s\" target=\"_blank\">%s</a>",url, symbol);
    }




    private String getSvgString(Visualizable visualizable) {
        SvannaVariant variant = visualizable.variant();
        if (visualizable.getGeneCount() > 10) {
            return EMPTY_STRING;
        }
        try {
            SvSvgGenerator gen;
            //visualizable.repetitiveRegions()
            switch (variant.variantType().baseType()) {
                case DEL:
                    gen = new DeletionSvgGenerator(variant, visualizable.genes(), visualizable.enhancers(), visualizable.repetitiveRegions());
                    break;
                case INS:
                    gen = new InsertionSvgGenerator(variant, visualizable.genes(), visualizable.enhancers(), visualizable.repetitiveRegions());
                    break;
                case INV:
                    gen = new InversionSvgGenerator(variant, visualizable.genes(), visualizable.enhancers(), visualizable.repetitiveRegions());
                    break;
                case DUP:
                    gen = new DuplicationSvgGenerator(variant, visualizable.genes(), visualizable.enhancers(), visualizable.repetitiveRegions());
                    break;
                case TRA:
                case BND:
                    if (variant instanceof BreakendVariant) {
                        gen = new TranslocationSvgGenerator(variant, (BreakendVariant) variant, visualizable.genes(), visualizable.enhancers(), visualizable.repetitiveRegions());
                        break;
                    }
                    // fall through to default
                default:
                    LOGGER.warn("SVG not implemented for type {}", variant.variantType());
                    return String.format("SVG generation for variant type %s not implemented.", variant.variantType().toString());
            }
            return gen.getSvg();
        } catch (SvAnnaRuntimeException e) {
            LOGGER.warn("Error: {}", e.getMessage());
            return "<p>" + e.getMessage() +"</p>\n";
        }
    }


    private String getLengthDisplayString(int len) {
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
                    throw new SvAnnaRuntimeException("Was expecting one location for insertion but got " + locations.size());
                }
                loc = locations.get(0);

                return String.format("%s:%sins%dbp", loc.getChrom(), decimalFormat.format(loc.getBegin()), len);
            case DEL:
                if (locations.size() != 1) {
                    throw new SvAnnaRuntimeException("Was expecting one location for deletion but got " + locations.size());
                }
                loc = locations.get(0);
                String lend = getLengthDisplayString(visualizable.variant().length());
                return String.format("%s:%s-%sdel (%s)", loc.getChrom(), decimalFormat.format(loc.getBegin()), decimalFormat.format(loc.getEnd()), lend);
            case TRA:
            case BND:
                if (locations.size() != 2) {
                    throw new SvAnnaRuntimeException("Was expecting two locations for translocation but got " + locations.size());
                }
                HtmlLocation locA = locations.get(0);
                HtmlLocation locB = locations.get(1);
                String translocationA = String.format("%s:%s", locA.getChrom(), decimalFormat.format(locA.getBegin()));
                String translocationB = String.format("%s:%s", locB.getChrom(), decimalFormat.format(locB.getBegin()));
                return String.format("t(%s, %s)", translocationA, translocationB);
            case DUP:
                if (locations.size() != 1) {
                    throw new SvAnnaRuntimeException("Was expecting one location for duplication but got " + locations.size());
                }
                HtmlLocation dupLoc = locations.get(0);
                int dupBegin = Math.min(dupLoc.getBegin(), dupLoc.getEnd());
                int dupEnd = Math.max(dupLoc.getBegin(), dupLoc.getEnd());
                String lengthDup = getLengthDisplayString(visualizable.variant().length());
                return String.format("%s:%d-%d duplication (%s)", dupLoc.getChrom(), dupBegin, dupEnd, lengthDup);
            case INV:
                if (locations.size() != 1) {
                    throw new SvAnnaRuntimeException("Was expecting one location for inversion but got " + locations.size());
                }
                HtmlLocation invLoc = locations.get(0);
                int invBegin = Math.min(invLoc.getBegin(), invLoc.getEnd());
                int invEnd = Math.max(invLoc.getBegin(), invLoc.getEnd());
                String lengthInv = getLengthDisplayString(visualizable.variant().length());
                return String.format("inv(%s)(%d; %d) (%s)", invLoc.getChrom(), invBegin, invEnd, lengthInv);
        }

        return "Unimplemented variant type: " + svtype;
    }


    private String getEnhancerSummary(Enhancer e) {
        String tissues = e.tissueSpecificity().stream().map(EnhancerTissueSpecificity::tissueTerm).map(Term::getName).collect(Collectors.joining(", "));
        String tissueLabel = String.format("%s; tau %.2f", tissues, e.tau());
       return String.format("%s:%d-%d [%s]", e.contig().ucscName(), e.start(), e.end(), tissueLabel);
    }


    private String itemValueRow(String item, String row) {
        return String.format("<tr><td><b>%s</b></td><td>%s</td></tr>\n", item, row);
    }

    private String affectedSymbols(Visualizable visualizable) {
        List<String> genes = visualizable.genes().stream()
                .map(Gene::symbol)
                .distinct()
                .collect(toList());
        if (genes.isEmpty()) { return "n/a"; }
        Collections.sort(genes);
        List<String> anchors = genes.stream().map(this::getGeneCardsLink).collect(toList());
        StringBuilder sb = new StringBuilder();
        sb.append("<ul>");
        for (String a : anchors) {
            sb.append("<li>").append(a).append("</li>\n");
        }
        sb.append("</ul>");
        return sb.toString();
    }

    private String numerousAffectedSymbols(Visualizable visualizable) {
        List<String> genes = visualizable.genes().stream().map(Gene::symbol).distinct().collect(toList());
        if (genes.isEmpty()) { return "n/a"; }
        Collections.sort(genes);
        List<String> anchors = genes.stream().map(this::getGeneCardsLink).collect(toList());
        StringBuilder sb = new StringBuilder();
        int n_genes = genes.size();

        sb.append("<table class=\"numvartab\">\n");
        sb.append("<caption>Affected genes</caption>\n");
        //sb.append("<thead><tr><td>Tissue</td><td>position</td><td>tau</td></tr></thead>\n");
        sb.append("<tbody>\n");
        final List<List<String>> groups = range(0, anchors.size())
                .boxed()
                .collect(groupingBy(index -> index / 4))
                .values()
                .stream()
                .map(indices -> indices
                        .stream()
                        .map(anchors::get)
                        .collect(toList()))
                .collect(toList());
        for (List<String> group : groups) {
            if (group.size() == 4) {
                sb.append(fourItemRow(group.get(0), group.get(1), group.get(2), group.get(3)));
            } else if (group.size() == 3) {
                sb.append(fourItemRow(group.get(0), group.get(1), group.get(2), ""));
            } else if (group.size() == 2) {
                sb.append(fourItemRow(group.get(0), group.get(1), "", ""));
            } else if (group.size() == 1) {
                sb.append(fourItemRow(group.get(0), "",  "", ""));
            }
        }
        sb.append("</tbody>\n</table>\n");
        return sb.toString();
    }

    private String numerousEnhancers(Visualizable visualizable) {
        List<Enhancer> enhancerList = visualizable.enhancers();
        Map<String, Integer> enhancerMap = new HashMap<>();
        for (Enhancer e : enhancerList) {
            String tissue = e.tissueSpecificity().stream().map(EnhancerTissueSpecificity::tissueTerm).map(Term::getName).collect(Collectors.joining(", "));
            enhancerMap.putIfAbsent(tissue, 0);
            enhancerMap.merge(tissue, 1, Integer::sum);
        }
        // sort the list (in ascending order)
        List<Map.Entry<String, Integer>> linkedList =new LinkedList<>(enhancerMap.entrySet());
        linkedList.sort(Map.Entry.comparingByValue());
        StringBuilder sb = new StringBuilder();
        sb.append("<table class=\"vartab\">\n");
        sb.append("<caption>Enhancers</caption>\n");
        sb.append("<thead><tr><td>Tissue</td><td>Count</td></tr></thead>\n");
        // iterate in reverse (descending) order
        ListIterator<Map.Entry<String, Integer>> listIterator = linkedList.listIterator(linkedList.size());
        while (listIterator.hasPrevious()) {
            Map.Entry<String, Integer> entry = listIterator.previous();
            sb.append(twoItemRow(entry.getKey(), String.valueOf(entry.getValue())));
        }
        sb.append("</tbody>\n</table>\n");
        return sb.toString();
    }


    private String getSequencePrioritization(Visualizable visualizable) {
        SvannaVariant variant = visualizable.variant();
        StringBuilder sb = new StringBuilder();
        int minSequenceDepth = variant.minDepthOfCoverage();
        int nRefReads = variant.numberOfRefReads();
        int nAltReads  = variant.numberOfAltReads();
        int copyNumber = variant.copyNumber();

        List<HtmlLocation> locations = visualizable.locations();
        String idString = variant.id();
        sb.append("<table class=\"vartab\">\n");
        sb.append("<caption>Variant information and disease association</caption>\n");
        sb.append(itemValueRow("ID", idString));
        sb.append(itemValueRow("type", visualizable.getType()));
        StringBuilder ucscBuilder = new StringBuilder();
        if (locations.isEmpty()) {
            ucscBuilder.append("ERROR - could not retrieve location(s) of structural variant</p>\n");
        } else if (locations.size() == 1) {
            ucscBuilder.append(getUcscLink(locations.get(0))).append("</p>");
        } else {
            ucscBuilder.append("<ul>\n");
            for (var loc : locations) {
                ucscBuilder.append("<li>").append(getUcscLink(loc)).append("</li>\n");
            }
            ucscBuilder.append("</ul>\n");
        }
        sb.append(itemValueRow("UCSC", ucscBuilder.toString()));
        int totalReads = nAltReads + nRefReads;
        if (totalReads != minSequenceDepth) {
            LOGGER.warn("Sum of alt ({})/ref({}) reads not equal to minDepth ({}) for variant {}", nAltReads, nRefReads, minSequenceDepth, visualizable.variant().id());
        }
        if (totalReads == 0) {
            LOGGER.warn("Total reads zero (should never happen), setting to 1 for variant {}.", visualizable.variant().id());
            totalReads = 1;
        }
        String refReads = String.format("%d/%d reads (%.1f%%)", nRefReads, totalReads, 100.0 * (float)nRefReads/totalReads);
        String altReads = String.format("%d/%d reads (%.1f%%)", nAltReads, totalReads, 100.0 * (float)nAltReads/totalReads);
        sb.append(itemValueRow("Ref", refReads));
        sb.append(itemValueRow("Alt", altReads));
        sb.append(itemValueRow("Disease associations", getDiseaseGenePrioritizationHtml(visualizable)));
        sb.append(itemValueRow("Affected genes", affectedSymbols(visualizable)));
        sb.append("</table>\n");
        return sb.toString();
    }

    /**
     * Use this to show the visualization of structural variants that affect more than two different genes.
     * In this case, it is very probably a large deletion. There is no use in displaying all of the
     * transcripts that are affected as we do for single-gene or two-gene deletions. Instead, we just assume that
     * there is a null mutation for all of the genes affected by the SV and we list them.
     *
     * @param visualizable object representing the visualizable data for a variant
     * @return HTML representation
     */
    private String getMultigeneSequencePrioritization(Visualizable visualizable) {
        StringBuilder sb = new StringBuilder();
        List<HtmlLocation> locations = visualizable.locations();
        String variantString = getVariantRepresentation(visualizable,  locations );
        Zygosity zygosity = visualizable.variant().zygosity();
        String zygo = zygosity.equals(Zygosity.UNKNOWN) ?
                "[unknown genotype]" :
                String.format("[%s]", zygosity.name().toLowerCase());
        String idString = visualizable.variant().id();
        String predImpact = String.format("Priority: %.2f", visualizable.variant().svPriority().getPriority());
        sb.append("<h1>").append(variantString).append(" &emsp; ").append(predImpact)
                .append(" &emsp; ").append(zygo).append("</h1>\n");
        sb.append("<div class=\"row\">\n");
        sb.append("<div class=\"column\" style=\"background-color:#F8F8F8;\">\n");
        sb.append("<table class=\"vartab\">\n");
        sb.append("<caption>Variant information and disease association</caption>\n");
        sb.append(itemValueRow("ID", idString));
        sb.append(itemValueRow("type", visualizable.getType()));
        StringBuilder ucscBuilder = new StringBuilder();
        if (locations.isEmpty()) {
            ucscBuilder.append("ERROR - could not retrieve location(s) of structural variant</p>\n");
        } else if (locations.size() == 1) {
            ucscBuilder.append(getUcscLink(locations.get(0))).append("</p>");
        } else {
            ucscBuilder.append("<ul>\n");
            for (var loc : locations) {
                ucscBuilder.append("<li>").append(getUcscLink(loc)).append("</li>\n");
            }
            ucscBuilder.append("</ul></p>\n");
        }
        sb.append(itemValueRow("UCSC", ucscBuilder.toString()));
        int nAltReads = visualizable.variant().numberOfAltReads();
        int nRefReads = visualizable.variant().numberOfRefReads();
        int totalReads = nAltReads + nRefReads;
        if (totalReads == 0) {
            LOGGER.warn("Total reads zero (should never happen), setting to 1 for variant {}.", visualizable.variant().id());
            totalReads = 1;
        }
        String refReads = String.format("%d/%d reads (%.1f%%)", nRefReads, totalReads, 100.0 * (float)nRefReads/totalReads);
        String altReads = String.format("%d/%d reads (%.1f%%)", nAltReads, totalReads, 100.0 * (float)nAltReads/totalReads);
        sb.append(itemValueRow("Ref", refReads));
        sb.append(itemValueRow("Alt", altReads));
        sb.append(itemValueRow("Disease associations", getDiseaseGenePrioritizationHtml(visualizable)));
        sb.append("</table>\n");
        sb.append(numerousAffectedSymbols(visualizable));

        sb.append("</div>\n");
        sb.append("<div class=\"column\" style=\"background-color:#F0F0F0;\">\n");
        sb.append(numerousEnhancers(visualizable)).append("\n");
        sb.append("</div>\n");
        sb.append("</div>\n");




        return sb.toString();
    }


    private String getEnhancerTable(Visualizable visualizable) {
        List<Enhancer> enhancerList = visualizable.enhancers();
        if (enhancerList.isEmpty()) {
            return "";
        } else  if (visualizable.getGeneCount() > THRESHOLD_GENE_COUNT_TO_SUPPRESS_DETAILS) {
            return String.format("<p>Total of %d relevant enhancers associated this variant.</p>\n",
                    visualizable.enhancers().size());
        }
        StringBuilder sb = new StringBuilder();
        sb.append("<ul>\n");
        sb.append("<table class=\"vartab\">\n");
        sb.append("<caption>Enhancers</caption>\n");
        sb.append("<thead><tr><td>Tissue</td><td>position</td><td>tau</td></tr></thead>\n");
        sb.append("<tbody>\n");
        for (Enhancer e : enhancerList) {
            String tissue = e.tissueSpecificity().stream().map(EnhancerTissueSpecificity::tissueTerm).map(Term::getName).collect(Collectors.joining(", "));
            String contig = e.contigName();
            int start = e.start();
            int end = e.end();
            String pos = (contig.startsWith("chr") ? contig : "chr"+ contig) + ":" + start +"-"+end;
            String tau = String.format("%.2f",e.tau());
            sb.append(threeItemRow(tissue, pos, tau));
        }
        sb.append("</tbody>\n</table>\n");
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
        Set<HpoDiseaseSummary> diseases = visualizable.diseaseSummaries();
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
                    disease.getDiseaseId(), prettifyDiseaseName(disease.getDiseaseName()), disease.getDiseaseId());
            sb.append("<li>").append(url).append("</li>\n");
        }
        sb.append("</ul>\n");
        return sb.toString();
    }

    private String twoItemRow(String item1, String item2) {
        return String.format("<tr><td><b>%s</b></td><td>%s</td></tr>\n", item1, item2);
    }

    private String threeItemRow(String item1, String item2, String item3) {
        return String.format("<tr><td><b>%s</b></td><td>%s</td><td>%s</td></tr>\n", item1, item2, item3);
    }

    private String fourItemRow(String item1, String item2, String item3, String item4) {
        return String.format("<tr><td><b>%s</b></td><td>%s</td><td>%s</td><td>%s</td></tr>\n", item1, item2, item3, item4);
    }

    String getOverlapSummary(Visualizable visualizable) {
        StringBuilder sb = new StringBuilder();
        sb.append("<table class=\"overlap\">\n");
        sb.append("<caption>Overlapping transcripts</caption>\n");
        //sb.append("<thead><tr><td>Type</td><td>description</td></tr></thead>\n");
        sb.append("<tbody>\n");
        for (GeneOverlap olap : visualizable.overlaps()) {
            Gene gene = olap.gene();
            for (TranscriptOverlap txOverlap : olap.transcriptOverlaps()) {
                String cat = txOverlap.getOverlapType().getName();
                String description = txOverlap.getDescription();
                sb.append(threeItemRow(gene.symbol(), cat, description));
            }
        }

        sb.append("</tbody>\n</table>\n");
        List<Enhancer> enhancers = visualizable.enhancers();
        if (enhancers.isEmpty()) {
            sb.append("<p>No enhancers found within genomic window.</p>\n");
        } else {
        sb.append("<table class=\"overlap\">\n");
        sb.append("<caption>Overlapping enhancers</caption>\n");
            sb.append("<thead><tr><td>Tissue</td><td>tau</td><td>Position</td></tr></thead>\n");
            sb.append("<tbody>\n");
            for (var e : visualizable.enhancers()) {
                String tau = String.format("%.1f", e.tau());
                String chrom = e.contigName().startsWith("chr") ? e.contigName() : "chr" + e.contigName();
                String position = String.format("%s:%s-%s", chrom, decimalFormat.format(e.start()), decimalFormat.format(e.end()));
                String tissues = e.tissueSpecificity().stream().map(EnhancerTissueSpecificity::tissueTerm).map(Term::getName).collect(Collectors.joining(", "));
                sb.append(threeItemRow(tissues, tau, position));
            }
            sb.append("</tbody>\n</table>\n");
        }
        return sb.toString();
    }
    /** Some of our name strings contain multiple synonyms. This function removes all but the first and
     * change all capitalized names to Sentence case.*/
    String prettifyDiseaseName(String name) {
        String prettified;
        int i = name.indexOf(';');
        if (i>0) {
            prettified = name.substring(0, i);
        } else {
            prettified = name;
        }
        // check if name starts with an OMIM id, e.g., #654321
        String pattern = "^#\\d{6,6}";
        if (prettified.length()>7 && prettified.substring(0,7).matches(pattern)) {
            prettified = prettified.substring(7);
        }
        prettified = prettified.trim();
        if (prettified.toUpperCase().equals(prettified)) {
            // String is all caps.
            prettified = Character.toUpperCase(prettified.charAt(0)) + prettified.substring(1).toLowerCase();
        }
        return prettified;
    }
}
