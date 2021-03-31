package org.jax.svanna.cli.cmd.benchmark;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.jax.svanna.cli.Main;
import org.jax.svanna.cli.cmd.SvAnnaCommand;
import org.jax.svanna.core.exception.LogUtils;
import org.jax.svanna.core.hpo.PhenotypeDataService;
import org.jax.svanna.core.priority.SvPrioritizer;
import org.jax.svanna.core.priority.SvPrioritizerFactory;
import org.jax.svanna.core.priority.SvPrioritizerType;
import org.jax.svanna.core.priority.SvPriority;
import org.monarchinitiative.phenol.ontology.data.Term;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.svart.Variant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;
import picocli.CommandLine;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.util.*;
import java.util.stream.Collectors;

@CommandLine.Command(name = "annotate-cases",
        aliases = {"AC"},
        header = "Annotate cases curated using Hpo Case Annotator and store the priorities in tabular file",
        mixinStandardHelpOptions = true,
        version = Main.VERSION,
        usageHelpWidth = Main.WIDTH,
        footer = Main.FOOTER)
public class AnnotateCasesCommand extends SvAnnaCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(AnnotateCasesCommand.class);

    private static final NumberFormat NF = NumberFormat.getNumberInstance();

    static {
        NF.setMaximumFractionDigits(2);
    }

    @CommandLine.Option(names = {"-f", "--case-folder"}, description = "path to a folder with case report JSON files")
    public Path caseReportPath;

    /*
     * ------------   I/O OPTIONS    ------------
     */
    @CommandLine.Option(names = {"-x", "--prefix"}, description = "prefix for the output CSV file (default: ${DEFAULT-VALUE})")
    public String outPrefix = "SVANNA_PP";

    /*
     * ------------ ANALYSIS OPTIONS ------------
     */
    @CommandLine.Parameters(description = "path(s) to case report JSON files")
    public List<Path> caseReports;


    @Override
    public Integer call() {
        List<CaseReport> cases = CaseReportImporter.readCaseReports(caseReports, caseReportPath);

        Map<String, Map<Variant, SvPriority>> results = new HashMap<>();
        try (ConfigurableApplicationContext context = getContext()) {

            PhenotypeDataService phenotypeDataService = context.getBean(PhenotypeDataService.class);
            SvPrioritizerFactory svPrioritizerFactory = context.getBean(SvPrioritizerFactory.class);

            int processed = 1;
            for (CaseReport aCase : cases) {
                LogUtils.logInfo(LOGGER, "({}/{}) Processing case `{}`", processed, cases.size(), aCase.caseName());

                // get and validate patient terms
                Collection<TermId> patientTerms = aCase.patientTerms();
                Set<Term> validatedPatientTerms = phenotypeDataService.validateTerms(patientTerms);
                Set<TermId> validatedPatientTermIds = validatedPatientTerms.stream().map(Term::getId).collect(Collectors.toSet());

                // create the prioritizer seeded by the phenotype terms and prioritize the variants
                SvPrioritizer<SvPriority> prioritizer = svPrioritizerFactory.getPrioritizer(SvPrioritizerType.ADDITIVE, validatedPatientTermIds);

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
