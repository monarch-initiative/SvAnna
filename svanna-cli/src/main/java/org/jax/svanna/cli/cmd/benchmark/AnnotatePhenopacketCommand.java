package org.jax.svanna.cli.cmd.benchmark;

import org.jax.svanna.cli.Main;
import org.jax.svanna.cli.cmd.PhenopacketImporter;
import org.jax.svanna.core.exception.LogUtils;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.phenopackets.schema.v1.Phenopacket;
import org.phenopackets.schema.v1.core.HtsFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;


@CommandLine.Command(name = "annotate-phenopacket",
        aliases = {"AAP"},
        header = "Prioritize the variants and phenotype of a case represented by phenopacket",
        mixinStandardHelpOptions = true,
        version = Main.VERSION,
        usageHelpWidth = Main.WIDTH,
        footer = Main.FOOTER)
public class AnnotatePhenopacketCommand extends BaseAdditiveCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(AnnotateAdditiveCommand.class);

    /*
     * ------------ ANALYSIS OPTIONS ------------
     */
    @CommandLine.Option(names = {"-p", "--phenopacket"}, description = "Path to input VCF file")
    public Path phenopacketPath = null;


    @Override
    public Integer call() {
        if (phenopacketPath == null) {
            LogUtils.logError(LOGGER, "Path to phenopacket must be provided");
            return 1;
        }

        int status = checkArguments();
        if (status != 0)
            return status;

        Phenopacket phenopacket;
        try {
            phenopacket = PhenopacketImporter.readPhenopacket(phenopacketPath);
        } catch (IOException e) {
            LogUtils.logError(LOGGER, "Error reading phenopacket at `{}`: {}", phenopacketPath, e.getMessage());
            return 1;
        }

        try {
            Set<TermId> phenotypeTermIds = phenopacket.getPhenotypicFeaturesList().stream()
                    .map(pf -> TermId.of(pf.getType().getId()))
                    .collect(Collectors.toSet());

            Optional<Path> vcfFilePathOptional = getVcfFilePath(phenopacket);
            if (vcfFilePathOptional.isEmpty())
                return 1;

            Path vcfFile = vcfFilePathOptional.get();

            runAnalysis(phenotypeTermIds, vcfFile);
        } catch (ExecutionException | InterruptedException | IOException e) {
            LogUtils.logError(LOGGER, "Error: {}", e.getMessage());
            return 1;
        }
        LogUtils.logInfo(LOGGER, "The analysis is complete. Bye");
        return 0;
    }

    private static Optional<Path> getVcfFilePath(Phenopacket phenopacket) {
        // There should be exactly one VCF file
        LinkedList<HtsFile> vcfFiles = phenopacket.getHtsFilesList().stream()
                .filter(htsFile -> htsFile.getHtsFormat().equals(HtsFile.HtsFormat.VCF))
                .distinct()
                .collect(Collectors.toCollection(LinkedList::new));
        if (vcfFiles.size() != 1) {
            LogUtils.logWarn(LOGGER, "Expected to find 1 VCF file, found {}", vcfFiles.size());
            return Optional.empty();
        }

        // The VCF file should have a proper URI
        HtsFile vcf = vcfFiles.getFirst();
        try {
            URI uri = new URI(vcf.getUri());
            return Optional.of(Path.of(uri));
        } catch (URISyntaxException e) {
            LogUtils.logWarn(LOGGER, "Invalid URI `{}`: {}", vcf.getUri(), e.getMessage());
            return Optional.empty();
        }
    }

}
