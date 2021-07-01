package org.jax.svanna.cli.writer.html;

import org.jax.svanna.cli.writer.AnalysisResults;
import org.jax.svanna.cli.writer.OutputFormat;
import org.jax.svanna.cli.writer.ResultWriter;
import org.jax.svanna.cli.writer.html.template.FilterAndCount;
import org.jax.svanna.cli.writer.html.template.HtmlTemplate;
import org.jax.svanna.core.exception.LogUtils;
import org.jax.svanna.core.hpo.PhenotypeDataService;
import org.jax.svanna.core.landscape.AnnotationDataService;
import org.jax.svanna.core.overlap.GeneOverlapper;
import org.jax.svanna.core.reference.SvannaVariant;
import org.monarchinitiative.svart.BreakendVariant;
import org.monarchinitiative.svart.Variant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// not thread-safe
public class HtmlResultWriter implements ResultWriter {

    private static final Logger LOGGER = LoggerFactory.getLogger(HtmlResultWriter.class);

    private final VisualizableGenerator visualizableGenerator;

    private final Visualizer visualizer;

    private AnalysisParameters analysisParameters;

    public boolean doNotReportBreakends = false;

    public HtmlResultWriter(GeneOverlapper overlapper,
                            AnnotationDataService annotationDataService,
                            PhenotypeDataService phenotypeDataService) {
        visualizableGenerator = new VisualizableGeneratorSimple(overlapper, annotationDataService, phenotypeDataService);
        visualizer = new HtmlVisualizer();
    }

    public void setAnalysisParameters(AnalysisParameters parameters) {
        this.analysisParameters = parameters;
    }

    public void setDoNotReportBreakends(boolean doNotReportBreakends) {
        this.doNotReportBreakends = doNotReportBreakends;
    }

    @Override
    public void write(AnalysisResults results, String prefix) {
        String outString = prefix + OutputFormat.HTML.fileSuffix();
        Path outPath = Path.of(outString);
        LogUtils.logInfo(LOGGER, "Writing HTML results to {}", outPath.toAbsolutePath());

        LogUtils.logDebug(LOGGER, "Reporting {} variants sorted by priority", analysisParameters.topNVariantsReported());
        // Add data required to create the header summary table in the HTML report (genes, enhancers, etc.)
        List<VariantLandscape> variantLandscapes = results.variants().stream()
                .sorted(prioritizedVariantComparator())
                .map(visualizableGenerator::prepareLandscape)
                .collect(Collectors.toList());

        // Generate the summary table
        Map<String, String> variantCountSummary = summarizeVariantCounts(variantLandscapes, analysisParameters.minAltReadSupport());
        variantCountSummary.put("vcf_file", results.variantSource());

        // Limit performing of the expensive DB lookups to several dozens of variants that will be reported
        // in the report, and not to the entire variant corpus
        List<String> visualizations = variantLandscapes.stream()
                .filter(s -> s.variant().numberOfAltReads() >= analysisParameters.minAltReadSupport()
                        && s.variant().passedFilters()
                        && !Double.isNaN(s.variant().svPriority().getPriority()))
                .filter(v -> !(v.variant() instanceof BreakendVariant) || !doNotReportBreakends)
                .limit(analysisParameters.topNVariantsReported())
                .map(visualizableGenerator::makeVisualizable)
                .map(visualizer::getHtml)
                .collect(Collectors.toList());

        HtmlTemplate template = new HtmlTemplate(visualizations, variantCountSummary, results.probandPhenotypeTerms(), this.analysisParameters);
        template.outputFile(outPath);
    }

    private Map<String, String> summarizeVariantCounts(List<VariantLandscape> variantLandscapes, int minAltAlleleSupport) {
        FilterAndCount fac = new FilterAndCount(variantLandscapes, minAltAlleleSupport);

        Map<String, String> infoMap = new HashMap<>();
        infoMap.put("unparsable", String.valueOf(fac.getUnparsableCount()));
        infoMap.put("n_affectedGenes", String.valueOf(fac.getnAffectedGenes()));
        infoMap.put("n_affectedEnhancers", String.valueOf(fac.getnAffectedEnhancers()));
        infoMap.put("counts_table", fac.toHtmlTable());
        return infoMap;
    }

    private static Comparator<? super SvannaVariant> prioritizedVariantComparator() {
        return (l, r) -> {
            int priority = r.svPriority().compareTo(l.svPriority()); // the order is intentional
            if (priority != 0)
                return priority;
            return Variant.compare(l, r);
        };
    }


}
