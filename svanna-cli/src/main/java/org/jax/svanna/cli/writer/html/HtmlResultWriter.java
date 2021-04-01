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

    public HtmlResultWriter(GeneOverlapper overlapper, AnnotationDataService annotationDataService, PhenotypeDataService phenotypeDataService) {
        visualizableGenerator = new VisualizableGeneratorSimple(overlapper, annotationDataService, phenotypeDataService);
        visualizer = new HtmlVisualizer();
    }

    public void setAnalysisParameters(AnalysisParameters parameters) {
        this.analysisParameters = parameters;
    }

    @Override
    public void write(AnalysisResults results, String prefix) {
        String outString = prefix + OutputFormat.HTML.fileSuffix();
        Path outPath = Path.of(outString);
        LogUtils.logInfo(LOGGER, "Writing HTML results to `{}`", outPath.toAbsolutePath());

        LogUtils.logDebug(LOGGER, "Reporting {} variants sorted by priority", analysisParameters.topNVariantsReported());
        List<Visualizable> visualizables = results.variants().stream()
                .map(visualizableGenerator::makeVisualizable)
                .collect(Collectors.toList());

        Map<String, String> variantCountSummary = summarizeVariantCounts(visualizables, analysisParameters.minAltReadSupport());
        variantCountSummary.put("vcf_file", results.variantSource());

        List<String> visualizations = visualizables.stream()
                .filter(vp -> vp.variant().numberOfAltReads() >= analysisParameters.minAltReadSupport()
                        && vp.variant().passedFilters()
                        && !Double.isNaN(vp.variant().svPriority().getPriority()))
                .sorted(prioritizedVariantComparator())
                .map(visualizer::getHtml)
                .limit(analysisParameters.topNVariantsReported())
                .collect(Collectors.toList());


        HtmlTemplate template = new HtmlTemplate(visualizations, variantCountSummary, results.probandPhenotypeTerms(), this.analysisParameters);
        template.outputFile(outPath);
    }

    private Map<String, String> summarizeVariantCounts(List<Visualizable> visualizables, int minAltAlleleSupport) {
        FilterAndCount fac = new FilterAndCount(visualizables, minAltAlleleSupport);

        Map<String, String> infoMap = new HashMap<>();
        infoMap.put("unparsable", String.valueOf(fac.getUnparsableCount()));
        infoMap.put("n_affectedGenes", String.valueOf(fac.getnAffectedGenes()));
        infoMap.put("n_affectedEnhancers", String.valueOf(fac.getnAffectedEnhancers()));
        infoMap.put("counts_table", fac.toHtmlTable());
        return infoMap;
    }

    private static Comparator<? super Visualizable> prioritizedVariantComparator() {
        return (l, r) -> {
            SvannaVariant rv = r.variant();
            SvannaVariant lv = l.variant();

            int priority = rv.svPriority().compareTo(lv.svPriority()); // the order is intentional
            if (priority != 0)
                return priority;
            return Variant.compare(lv, rv);
        };
    }


}
