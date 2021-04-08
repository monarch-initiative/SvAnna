package org.jax.svanna.cli.cmd.benchmark;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import org.jax.svanna.core.exception.LogUtils;
import org.jax.svanna.core.reference.*;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

class CaseReportImporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(CaseReportImporter.class);

    private static final GenomicAssembly ASSEMBLY = GenomicAssemblies.GRCh38p13();
    private static final VcfConverter VCF_CONVERTER = new VcfConverter(ASSEMBLY, VariantTrimmer.rightShiftingTrimmer(VariantTrimmer.removingCommonBase()));

    private static final JsonFormat.Parser JSON_PARSER = JsonFormat.parser();
    // Matches a string like `PMID:30269814-Nguyen-2018-PIGS-Family_2-II-1`
    private static final Pattern PHENOPACKET_ID_PATTERN = Pattern.compile("PMID:(?<pmid>\\d+)-(?<author>[\\w_-]+)-(?<year>\\d{4})-(?<gene>[\\w\\d]+)-(?<proband>[\\w\\d‐\\-:,/._]+)", Pattern.UNICODE_CHARACTER_CLASS);

    private CaseReportImporter() {
    }


    public static List<CaseReport> readCaseReports(List<Path> caseReports, Path caseReportPath) {
        List<CaseReport> cases = new ArrayList<>();

        cases.addAll(readCasesProvidedAsPositionalArguments(caseReports));
        cases.addAll(readCasesProvidedViaCaseFolderOption(caseReportPath));

        cases.sort(Comparator.comparing(cr -> cr.caseSummary().caseSummary()));
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

        Optional<CaseSummary> csOpt = parsePhenopacketId(phenopacket.getId());
        if (csOpt.isEmpty()) {
            LogUtils.logWarn(LOGGER, "Invalid phenopacket id {}", phenopacket.getId());
            return Optional.empty();
        }

        CaseSummary caseSummary = csOpt.get();

        Set<TermId> terms = phenopacket.getPhenotypicFeaturesList().stream()
                .map(pf -> TermId.of(pf.getType().getId()))
                .collect(Collectors.toSet());

        Set<SvannaVariant> variants = new HashSet<>();
        int i = 0;
        for (org.phenopackets.schema.v1.core.Variant v : phenopacket.getVariantsList()) {
            if (v.getAlleleCase() == org.phenopackets.schema.v1.core.Variant.AlleleCase.VCF_ALLELE) {
                VcfAllele vcfAllele = v.getVcfAllele();
                if (!vcfAllele.getGenomeAssembly().equals("GRCh38")) {
                    LogUtils.logWarn(LOGGER, "Unexpected genomic assembly `{}` in case `{}`", vcfAllele.getGenomeAssembly(), caseSummary.caseSummary());
                    continue;
                }

                Contig contig = VCF_CONVERTER.parseContig(vcfAllele.getChr());
                if (contig.isUnknown()) {
                    LogUtils.logWarn(LOGGER, "Unable to find contig `{}` in GRCh38.p13", vcfAllele.getChr());
                    continue;
                }

                String variantId = String.format("%s[%d]", caseSummary.caseSummary(), i);
                String info = vcfAllele.getInfo();
                Map<String, String> infoFields;
                if (info.isEmpty()) {
                    infoFields = Map.of();
                } else {
                    infoFields = Arrays.stream(info.split(";"))
                            .map(p -> p.split("="))
                            .collect(Collectors.toMap(f -> f[0], f -> f.length == 2 ? f[1] : ""));
                }

                VariantCallAttributes.Builder attrBuilder = parseVariantCallAttributes(v);

                if (infoFields.containsKey("SVTYPE")) {
                    String[] cipos = infoFields.getOrDefault("CIPOS", "0,0").split(",");
                    ConfidenceInterval ciPos = ConfidenceInterval.of(Integer.parseInt(cipos[0]), Integer.parseInt(cipos[1]));
                    Position start = Position.of(vcfAllele.getPos(), ciPos);

                    String[] ciends = infoFields.getOrDefault("CIEND", "0,0").split(",");
                    ConfidenceInterval ciEnd = ConfidenceInterval.of(Integer.parseInt(ciends[0]), Integer.parseInt(ciends[1]));

                    String svtype = infoFields.get("SVTYPE");
                    if (svtype.equalsIgnoreCase("BND")) {
                        // BREAKEND
                        String mateId = infoFields.getOrDefault("MATEID", "");
                        String eventId = infoFields.getOrDefault("EVENTID", "");
                        BreakendedSvannaVariant.Builder builder = VCF_CONVERTER.convertBreakend(BreakendedSvannaVariant.builder(),
                                contig, variantId, start,
                                vcfAllele.getRef(), vcfAllele.getAlt(),
                                ciEnd, mateId, eventId);
                        builder.variantCallAttributes(attrBuilder.build());
                        variants.add(builder.build());
                        continue;
                    }
                    if (!infoFields.containsKey("END")) {
                        LogUtils.logWarn(LOGGER, "Unable to find variant END in `{}`", caseSummary.caseSummary());
                        continue;
                    }

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
                        // SYMBOLIC
                        DefaultSvannaVariant.Builder builder = VCF_CONVERTER.convertSymbolic(DefaultSvannaVariant.builder(),
                                contig, variantId, start, end,
                                vcfAllele.getRef(), '<' + vcfAllele.getAlt() + '>', changeLength);
                        builder.variantCallAttributes(attrBuilder.build());
                        variants.add(builder.build());
                    } catch (Exception e) {
                        LogUtils.logWarn(LOGGER, "Error: {}", e.getMessage());
                        throw e;
                    }
                } else {
                    // SEQUENCE
                    DefaultSvannaVariant.Builder builder = VCF_CONVERTER.convert(DefaultSvannaVariant.builder(),
                            contig, variantId, vcfAllele.getPos(),
                            vcfAllele.getRef(), vcfAllele.getAlt());
                    builder.variantCallAttributes(attrBuilder.build());
                    variants.add(builder.build());
                }
            }

            i++;
        }

        return Optional.of(CaseReport.of(caseSummary, terms, variants));
    }

    private static Optional<CaseSummary> parsePhenopacketId(String phenopacketId) {
        Matcher matcher = PHENOPACKET_ID_PATTERN.matcher(phenopacketId);
        if (!matcher.matches())
            return Optional.empty();

        return Optional.of(
                CaseSummary.of(matcher.group("author"), matcher.group("pmid"),
                        matcher.group("year"), matcher.group("gene"), matcher.group("proband")));
    }

    private static VariantCallAttributes.Builder parseVariantCallAttributes(org.phenopackets.schema.v1.core.Variant v) {
        VariantCallAttributes.Builder attrBuilder = VariantCallAttributes.builder();
        String zygosityId = v.getZygosity().getId();
        switch (zygosityId) {
            case "GENO:0000135":
                attrBuilder.zygosity(Zygosity.HETEROZYGOUS)
                        .dp(20)
                        .refReads(10)
                        .altReads(10);
                break;
            case "GENO:0000136":
                attrBuilder.zygosity(Zygosity.HOMOZYGOUS)
                        .dp(20)
                        .refReads(0)
                        .altReads(20);
                break;
            case "GENO:0000134":
                attrBuilder.zygosity(Zygosity.HEMIZYGOUS)
                        .dp(10)
                        .refReads(0)
                        .altReads(10);
                break;
            default:
                LogUtils.logWarn(LOGGER, "Unknown zygosity id={} label={}", zygosityId, v.getZygosity().getLabel());
                attrBuilder.zygosity(Zygosity.UNKNOWN);
                break;
        }
        return attrBuilder;
    }

}
