package org.jax.svanna.cli.cmd.annotate_cases;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.jax.svanna.cli.Main;
import org.jax.svanna.cli.cmd.AnnotateTurboCommand;
import org.jax.svanna.cli.cmd.SvAnnaCommand;
import org.jax.svanna.core.exception.LogUtils;
import org.jax.svanna.core.hpo.PhenotypeDataService;
import org.jax.svanna.core.priority.SvPrioritizer;
import org.jax.svanna.core.priority.SvPrioritizerType;
import org.jax.svanna.core.priority.SvPriority;
import org.jax.svanna.core.priority.SvPriorityFactory;
import org.monarchinitiative.phenol.ontology.data.Term;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.svart.Variant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;
import picocli.CommandLine;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.util.*;
import java.util.stream.Collectors;

@CommandLine.Command(name = "annotate-cases",
        aliases = {"C"},
        header = "Annotate cases with additive prioritizer features",
        mixinStandardHelpOptions = true,
        version = Main.VERSION,
        usageHelpWidth = Main.WIDTH,
        footer = Main.FOOTER)
public class AnnotateCasesCommand extends SvAnnaCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(AnnotateTurboCommand.class);

    private static final NumberFormat NF = NumberFormat.getNumberInstance();

    static {
        NF.setMaximumFractionDigits(2);
    }

    @CommandLine.Option(names = {"-f", "--case-folder"}, description = "path to a folder with JSON case report files")
    public Path caseReportPath;

    /*
     * ------------   I/O OPTIONS    ------------
     */
    @CommandLine.Option(names = {"-x", "--prefix"}, description = "prefix for output files (default: ${DEFAULT-VALUE})")
    public String outPrefix = "SVANNA_PP";

    /*
     * ------------ ANALYSIS OPTIONS ------------
     */
    @CommandLine.Parameters(description = "path(s) to JSON files with case reports curated by Hpo Case Annotator")
    public List<Path> caseReports;


    @Override
    public Integer call() {
        List<CaseReport> cases = readCaseReports();

        Map<String, Map<Variant, SvPriority>> results = new HashMap<>();
        try (ConfigurableApplicationContext context = getContext()) {

            PhenotypeDataService phenotypeDataService = context.getBean(PhenotypeDataService.class);
            SvPriorityFactory svPriorityFactory = context.getBean(SvPriorityFactory.class);

            int processed = 1;
            for (CaseReport aCase : cases) {
                LogUtils.logInfo(LOGGER, "({}/{}) Processing case `{}`", processed, cases.size(), aCase.caseName());

                // get and validate patient terms
                Collection<TermId> patientTerms = aCase.patientTerms();
                Set<Term> validatedPatientTerms = phenotypeDataService.validateTerms(patientTerms);
                Set<TermId> validatedPatientTermIds = validatedPatientTerms.stream().map(Term::getId).collect(Collectors.toSet());

                // create the prioritizer seeded by the phenotype terms and prioritize the variants
                SvPrioritizer<Variant, SvPriority> prioritizer = svPriorityFactory.getPrioritizer(SvPrioritizerType.ADDITIVE, validatedPatientTermIds);

                Map<Variant, SvPriority> priorities = new HashMap<>();
                for (Variant variant : aCase.variants()) {
                    SvPriority priority = prioritizer.prioritize(variant);
                    priorities.put(variant, priority);
                }
                results.put(aCase.caseName(), priorities);
                processed++;
            }
        } catch (Exception e) {
            LogUtils.logError(LOGGER, "Error: {}", e);
            return 1;
        }

        try {
            writeOutTheResults(results);
        } catch (IOException e) {
            LogUtils.logError(LOGGER, "Error: {}", e);
            return 1;
        }

        return 0;
    }

    private List<CaseReport> readCaseReports() {
        List<CaseReport> cases = new ArrayList<>();

        cases.addAll(readCasesProvidedAsPositionalArguments());
        cases.addAll(readCasesProvidedViaCaseFolderOption());

        cases.sort(Comparator.comparing(CaseReport::caseName));
        return cases;
    }

    private List<CaseReport> readCasesProvidedAsPositionalArguments() {
        if (caseReports != null) {
            return caseReports.stream()
                    .map(CaseReportImporter::importCase)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toList());
        }
        return List.of();
    }

    private List<CaseReport> readCasesProvidedViaCaseFolderOption() {
        if (caseReportPath != null) {
            File caseReportFile = caseReportPath.toFile();
            if (caseReportFile.isDirectory()) {
                File[] jsons = caseReportFile.listFiles(f -> f.getName().endsWith(".json"));
                if (jsons != null) {
                    LogUtils.logDebug(LOGGER, "Found {} JSON files in `{}`", jsons.length, caseReportPath.toAbsolutePath());
                    return Arrays.stream(jsons)
                            .map(File::toPath)
                            .map(CaseReportImporter::importCase)
                            .filter(Optional::isPresent)
                            .map(Optional::get)
                            .collect(Collectors.toList());
                }
            } else {
                LogUtils.logWarn(LOGGER, "Skipping not-a-folder `{}`", caseReportPath);
            }
        }
        return List.of();
    }

    private void writeOutTheResults(Map<String, Map<Variant, SvPriority>> results) throws IOException {
        Path outputPath = Path.of(outPrefix + ".csv");
        LogUtils.logInfo(LOGGER, "Writing out the results to `{}`", outputPath.toAbsolutePath());
        try (BufferedWriter writer = Files.newBufferedWriter(outputPath)) {
            CSVPrinter printer = CSVFormat.DEFAULT
                    .withHeader("CASE_NAME", "VTYPE", "VARIANT", "PRIORITY")
                    .print(writer);

            for (Map.Entry<String, Map<Variant, SvPriority>> aCase : results.entrySet()) {
                String caseName = aCase.getKey();
                for (Map.Entry<Variant, SvPriority> vp : aCase.getValue().entrySet()) {
                    Variant variant = vp.getKey();
                    SvPriority priority = vp.getValue();

                    printer.print(caseName);
                    printer.print(variant.variantType().baseType());
                    printer.print(LogUtils.variantSummary(variant));
                    printer.print(priority.getPriority());

                    printer.println();
                }
            }
        }
    }

}
