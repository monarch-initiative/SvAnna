package org.monarchinitiative.svanna.cli.cmd;

import com.google.protobuf.Message;
import org.monarchinitiative.phenol.base.PhenolRuntimeException;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.phenopackets.phenopackettools.core.PhenopacketElement;
import org.phenopackets.phenopackettools.core.PhenopacketSchemaVersion;
import org.phenopackets.phenopackettools.io.PhenopacketParser;
import org.phenopackets.phenopackettools.io.PhenopacketParserFactory;
import org.phenopackets.phenopackettools.util.format.SniffException;
import org.phenopackets.schema.v1.Phenopacket;
import org.phenopackets.schema.v1.core.HtsFile;
import org.phenopackets.schema.v2.core.File;
import org.phenopackets.schema.v2.core.PhenotypicFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Utility methods for reading {@link AnalysisData} from v1 or v2 phenopacket.
 */
class PhenopacketAnalysisDataUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(PhenopacketAnalysisDataUtil.class);

    private PhenopacketAnalysisDataUtil() {
    }

    static AnalysisData parseV2Phenopacket(Path phenopacketPath,
                                           Path cliVcfPath,
                                           PhenopacketParserFactory parserFactory) throws AnalysisInputException {
        Message message = parseMessage(phenopacketPath, parserFactory, PhenopacketSchemaVersion.V2);

        if (message instanceof org.phenopackets.schema.v2.Phenopacket) {
            org.phenopackets.schema.v2.Phenopacket pp = (org.phenopackets.schema.v2.Phenopacket) message;

            // (1) Phenotype features
            List<TermId> phenotypeTermIds = new ArrayList<>();
            boolean reportExcludedFeature = true;
            List<PhenotypicFeature> phenotypicFeaturesList = pp.getPhenotypicFeaturesList();
            for (int i = 0; i < phenotypicFeaturesList.size(); i++) {
                PhenotypicFeature pf = phenotypicFeaturesList.get(i);
                // SvAnna does not support excluded features.
                // As a matter of courtesy, let's warn the user about skipping the excluded features.
                if (pf.getExcluded()) {
                    if (reportExcludedFeature) {
                        reportExcludedFeature = false;
                        String excludedFeatureIndices = extractIndicesOfNegatedFeatures(phenotypicFeaturesList, PhenotypicFeature::getExcluded);
                        LOGGER.warn("Skipping unsupported excluded phenotype features {}", excludedFeatureIndices);
                    }
                } else {
                    try {
                        TermId termId = TermId.of(pf.getType().getId());
                        phenotypeTermIds.add(termId);
                    } catch (PhenolRuntimeException pre) {
                        LOGGER.warn("Skipping phenotype feature #{} due to invalid identifier {}", i, pf.getType().getId());
                    }
                }
            }

            // (2) VCF path
            // We take the 1st VCF file
            List<File> vcfFiles = pp.getFilesList().stream()
                    .filter(f -> "vcf".equalsIgnoreCase(f.getFileAttributesOrDefault("fileFormat", null)))
                    .collect(Collectors.toList());
            Path vcf = getVcfPath(cliVcfPath, vcfFiles, File::getUri);
            return new AnalysisData(phenotypeTermIds, vcf);
        } else {
            // Shouldn't really happen but let's make sure we can report a meaningful error.
            throw new AnalysisInputException(String.format("Unexpected instance %s!=%s", message.getClass().getName(), org.phenopackets.schema.v2.Phenopacket.class.getName()));
        }

    }

    static AnalysisData parseV1Phenopacket(Path phenopacketPath,
                                           Path cliVcfPath,
                                           PhenopacketParserFactory parserFactory) throws AnalysisInputException {
        Message message = parseMessage(phenopacketPath, parserFactory, PhenopacketSchemaVersion.V1);
        if (message instanceof Phenopacket) {
            Phenopacket pp = (Phenopacket) message;

            // (1) Phenotype features
            List<TermId> phenotypeTermIds = new ArrayList<>();
            boolean reportExcludedFeature = true;
            List<org.phenopackets.schema.v1.core.PhenotypicFeature> phenotypicFeaturesList = pp.getPhenotypicFeaturesList();
            for (int i = 0; i < phenotypicFeaturesList.size(); i++) {
                org.phenopackets.schema.v1.core.PhenotypicFeature pf = phenotypicFeaturesList.get(i);
                // SvAnna does not support excluded features.
                // As a matter of courtesy, let's warn the user about skipping the excluded features.
                if (pf.getNegated()) {
                    if (reportExcludedFeature) {
                        reportExcludedFeature = false;
                        String excludedFeatureIndices = extractIndicesOfNegatedFeatures(phenotypicFeaturesList, org.phenopackets.schema.v1.core.PhenotypicFeature::getNegated);
                        LOGGER.warn("Skipping unsupported excluded phenotype features {}", excludedFeatureIndices);
                    }
                } else {
                    try {
                        TermId termId = TermId.of(pf.getType().getId());
                        phenotypeTermIds.add(termId);
                    } catch (PhenolRuntimeException pre) {
                        LOGGER.warn("Skipping phenotype feature #{} due to invalid identifier {}", i, pf.getType().getId());
                    }
                }
            }

            // (2) VCF path
            // We take the 1st VCF file
            List<HtsFile> vcfFiles = pp.getHtsFilesList().stream()
                    .filter(f -> f.getHtsFormat().equals(HtsFile.HtsFormat.VCF))
                    .collect(Collectors.toList());
            Path vcf = getVcfPath(cliVcfPath, vcfFiles, HtsFile::getUri);
            return new AnalysisData(phenotypeTermIds, vcf);
        } else {
            // Again, shouldn't really happen but let's make sure we can report a meaningful error.
            throw new AnalysisInputException(String.format("Unexpected instance %s!=%s", message.getClass().getName(), org.phenopackets.schema.v2.Phenopacket.class.getName()));
        }
    }

    private static Message parseMessage(Path phenopacketPath,
                                PhenopacketParserFactory parserFactory,
                                PhenopacketSchemaVersion schemaVersion) throws AnalysisInputException {
        PhenopacketParser parser = parserFactory.forFormat(schemaVersion);

        Message message;
        try {
            message = parser.parse(PhenopacketElement.PHENOPACKET, phenopacketPath);
        } catch (IOException | SniffException e) {
            throw new AnalysisInputException(e);
        }
        return message;
    }

    private static <T> Path getVcfPath(Path cliVcfPath,
                               List<T> files,
                               Function<T, String> uriExtractor) throws AnalysisInputException {
        if (files.isEmpty()) {
            if (cliVcfPath == null)
                throw new AnalysisInputException("VCF file was found neither in CLI arguments nor in the Phenopacket. Aborting.");
            else
                return cliVcfPath;
        } else if (files.size() > 1) {
            String fileUris = files.stream()
                    .map(uriExtractor)
                    .collect(Collectors.joining(", ", "[", "]"));
            throw new AnalysisInputException(String.format("There must be exactly 1 VCF file in the phenopacket but got %s: %s", files.size(), fileUris));
        } else {
            String uriStr = uriExtractor.apply(files.get(0));
            try {
                URI uri = new URI(uriStr);
                return Path.of(uri);
            } catch (URISyntaxException e) {
                LOGGER.warn("Invalid URI `{}`: {}", uriStr, e.getMessage());
                LOGGER.debug("Invalid URI `{}`: {}", uriStr, e.getMessage(), e);
                throw new AnalysisInputException(e);
            }
        }
    }

    private static <T> String extractIndicesOfNegatedFeatures(List<T> phenotypicFeaturesList, Predicate<T> isNegated) {
        return IntStream.range(0, phenotypicFeaturesList.size())
                .filter(idx -> isNegated.test(phenotypicFeaturesList.get(idx)))
                .boxed()
                .map(Objects::toString)
                .collect(Collectors.joining(", ", "[", "]"));
    }
}
