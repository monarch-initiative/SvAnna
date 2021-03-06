package org.jax.svanna.cli.cmd.annotate_cases;

import org.jax.svanna.core.exception.LogUtils;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.svart.*;
import org.monarchinitiative.svart.util.VariantTrimmer;
import org.monarchinitiative.svart.util.VcfConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

class CaseReportImporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(CaseReportImporter.class);

    private static final Pattern HPO_PATTERN = Pattern.compile("\"id\":\\s*\"(?<term>HP:\\d{7})\"");
    private static final Pattern VARIANT_PATTERN = Pattern.compile("\"variant\":\\s* \\[(?<payload>.*)]");

    private static final GenomicAssembly ASSEMBLY = GenomicAssemblies.GRCh38p13();
    private static final VcfConverter VCF_CONVERTER = new VcfConverter(ASSEMBLY, VariantTrimmer.rightShiftingTrimmer(VariantTrimmer.removingCommonBase()));


    private CaseReportImporter() {
    }


    static Optional<CaseReport> importCase(Path casePath) {

        String payload;
        try (BufferedReader reader = Files.newBufferedReader(casePath)) {
            payload = reader.lines().collect(Collectors.joining());
        } catch (IOException e) {
            LogUtils.logWarn(LOGGER, "Error importing case report at `{}`: {}", casePath.toAbsolutePath(), e.getMessage());
            return Optional.empty();
        }

        String caseName = casePath.toFile().getName();
        caseName = caseName.endsWith(".json") ? caseName.substring(0, caseName.length() - 5) : caseName;


        Set<TermId> terms = parsePhenotypeTerms(payload);
        Set<Variant> variants = parseVariants(caseName, payload);

        return Optional.of(new CaseReport(caseName, terms, variants));
    }

    private static Set<Variant> parseVariants(String caseName, String payload) {
        Set<Variant> variants = new HashSet<>();
        Matcher variantMatcher = VARIANT_PATTERN.matcher(payload);
        while (variantMatcher.find()) {
            String variantPayload = variantMatcher.group("payload");
            try {
                Variant variant = parseVariantPayload(variantPayload);
                variants.add(variant);
            } catch (ParseException e) {
                LogUtils.logWarn(LOGGER, "Unable to parse variant in case `{}`", caseName);
            } catch (CoordinatesOutOfBoundsException e) {
                LogUtils.logWarn(LOGGER, "Invalid variant coordinates `{}`", caseName);
            }
        }
        return variants;
    }

    private static Set<TermId> parsePhenotypeTerms(String payload) {
        Set<TermId> terms = new HashSet<>();
        Matcher termMatcher = HPO_PATTERN.matcher(payload);
        while (termMatcher.find()) {
            String termId = termMatcher.group("term");
            terms.add(TermId.of(termId));
        }
        return terms;
    }

    private static Variant parseVariantPayload(String variantPayload) throws ParseException {
        Pattern uberPattern = Pattern.compile(".*\"contig\":\\s*\"(?<contig>.*)\".*\"pos\":\\s*(?<pos>\\d+).*\"refAllele\":\\s*\"(?<ref>\\w+)\".*\"altAllele\":\\s*\"(?<alt>\\w+)\".*");

        Matcher uberMatch = uberPattern.matcher(variantPayload);
        if (!uberMatch.matches())
            throw new ParseException("Unable to find the required data in variant " + variantPayload, 0);

        Contig contig = VCF_CONVERTER.parseContig(uberMatch.group("contig"));
        if (contig.isUnknown())
            throw new ParseException("Unable to parse contig " + uberMatch.group("contig") + " in GRCh38.p13", 0);

        int pos = Integer.parseInt(uberMatch.group("pos"));
        String ref = uberMatch.group("ref");
        String alt = uberMatch.group("alt");


        return VCF_CONVERTER.convert(contig, "", pos, ref, alt);
    }

}
