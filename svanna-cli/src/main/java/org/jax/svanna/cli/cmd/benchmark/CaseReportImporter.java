package org.jax.svanna.cli.cmd.benchmark;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import org.jax.svanna.core.exception.LogUtils;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.svart.*;
import org.monarchinitiative.svart.util.VariantTrimmer;
import org.monarchinitiative.svart.util.VcfConverter;
import org.phenopackets.schema.v1.Phenopacket;
import org.phenopackets.schema.v1.core.VcfAllele;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

class CaseReportImporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(CaseReportImporter.class);

    private static final GenomicAssembly ASSEMBLY = GenomicAssemblies.GRCh38p13();
    private static final VcfConverter VCF_CONVERTER = new VcfConverter(ASSEMBLY, VariantTrimmer.rightShiftingTrimmer(VariantTrimmer.removingCommonBase()));

    private static final JsonFormat.Parser JSON_PARSER = JsonFormat.parser();

    private CaseReportImporter() {
    }


    public static List<CaseReport> readCaseReports(List<Path> caseReports, Path caseReportPath) {
        List<CaseReport> cases = new ArrayList<>();

        cases.addAll(readCasesProvidedAsPositionalArguments(caseReports));
        cases.addAll(readCasesProvidedViaCaseFolderOption(caseReportPath));

        cases.sort(Comparator.comparing(CaseReport::caseName));
        return cases;
    }

    public static List<CaseReport> readCasesProvidedAsPositionalArguments(List<Path> caseReports) {
        if (caseReports != null) {
            return caseReports.stream()
                    .map(CaseReportImporter::importPhenopacket)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toList());
        }
        return List.of();
    }

    public static List<CaseReport> readCasesProvidedViaCaseFolderOption(Path caseReportPath) {
        if (caseReportPath != null) {
            File caseReportFile = caseReportPath.toFile();
            if (caseReportFile.isDirectory()) {
                File[] jsons = caseReportFile.listFiles(f -> f.getName().endsWith(".json"));
                if (jsons != null) {
                    LogUtils.logDebug(LOGGER, "Found {} JSON files in `{}`", jsons.length, caseReportPath.toAbsolutePath());
                    return Arrays.stream(jsons)
                            .map(File::toPath)
                            .map(CaseReportImporter::importPhenopacket)
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


    private static Optional<CaseReport> importPhenopacket(Path casePath) {
        LogUtils.logTrace(LOGGER, "Parsing `{}`", casePath);
        String payload;
        try (BufferedReader reader = Files.newBufferedReader(casePath)) {
            payload = reader.lines().collect(Collectors.joining());
        } catch (IOException e) {
            LogUtils.logWarn(LOGGER, "Error importing case report at `{}`: {}", casePath.toAbsolutePath(), e.getMessage());
            return Optional.empty();
        }

        Phenopacket phenopacket;
        try {
            Phenopacket.Builder builder = Phenopacket.newBuilder();
            JSON_PARSER.merge(payload, builder);
            phenopacket = builder.build();
        } catch (InvalidProtocolBufferException e) {
            LogUtils.logWarn(LOGGER, "Unable to decode content of the phenopacket at `{}`: `{}`", casePath, e.getMessage());
            return Optional.empty();
        }

        String caseName = phenopacket.getId();

        Set<TermId> terms = phenopacket.getPhenotypicFeaturesList().stream()
                .map(pf -> TermId.of(pf.getType().getId()))
                .collect(Collectors.toSet());

        Set<Variant> variants = new HashSet<>();
        int i = 0;
        for (org.phenopackets.schema.v1.core.Variant v : phenopacket.getVariantsList()) {
            if (v.getAlleleCase() == org.phenopackets.schema.v1.core.Variant.AlleleCase.VCF_ALLELE) {
                VcfAllele vcfAllele = v.getVcfAllele();
                if (!vcfAllele.getGenomeAssembly().equals("GRCh38")) {
                    LogUtils.logWarn(LOGGER, "Unexpected genomic assembly `{}` in case `{}`", vcfAllele.getGenomeAssembly(), caseName);
                    continue;
                }

                Contig contig = VCF_CONVERTER.parseContig(vcfAllele.getChr());
                if (contig.isUnknown()) {
                    LogUtils.logWarn(LOGGER, "Unable to find contig `{}` in GRCh38.p13", vcfAllele.getChr());
                    continue;
                }

                String variantId = String.format("%s[%d]", caseName, i);
                String info = vcfAllele.getInfo();
                Map<String, String> infoFields;
                if (info.isEmpty()) {
                    infoFields = Map.of();
                } else {
                    infoFields = Arrays.stream(info.split(";"))
                            .map(p -> p.split("="))
                            .collect(Collectors.toMap(f -> f[0], f -> f.length == 2 ? f[1] : ""));
                }

                if (infoFields.containsKey("SVTYPE")) {
                    String svtype = infoFields.get("SVTYPE");
                    if (svtype.equalsIgnoreCase("BND")) {
                        // TODO - figure out how to parse breakends
                        LogUtils.logWarn(LOGGER, "Skipping breakend variant in `{}`", caseName);
                        continue;
                    }
                    if (!infoFields.containsKey("END")) {
                        LogUtils.logWarn(LOGGER, "Unable to find variant END in `{}`", caseName);
                        continue;
                    }

                    String[] cipos = infoFields.getOrDefault("CIPOS", "0,0").split(",");
                    ConfidenceInterval ciPos = ConfidenceInterval.of(Integer.parseInt(cipos[0]), Integer.parseInt(cipos[1]));
                    Position start = Position.of(vcfAllele.getPos(), ciPos);

                    String[] ciends = infoFields.getOrDefault("CIEND", "0,0").split(",");
                    ConfidenceInterval ciEnd = ConfidenceInterval.of(Integer.parseInt(ciends[0]), Integer.parseInt(ciends[1]));
                    Position end = Position.of(Integer.parseInt(infoFields.get("END")), ciEnd);

                    int changeLength = end.pos() - start.pos() + 1;
                    switch (vcfAllele.getAlt().toLowerCase()) {
                        case "del":
                            changeLength = -changeLength;
                            break;
                        case "inv":
                            changeLength = 0;
                            break;
                    }

                    try {
                        Variant variant = VCF_CONVERTER.convertSymbolic(contig, variantId, start, end, vcfAllele.getRef(), '<' + vcfAllele.getAlt() + '>', changeLength);
                        variants.add(variant);
                    } catch (Exception e) {
                        LogUtils.logWarn(LOGGER, "Error: {}", e.getMessage());
                        throw e;
                    }
                } else {
                    Variant variant = VCF_CONVERTER.convert(contig, variantId, vcfAllele.getPos(), vcfAllele.getRef(), vcfAllele.getAlt());
                    variants.add(variant);
                }
            }

            i++;
        }

        return Optional.of(new CaseReport(caseName, terms, variants));
    }

}
