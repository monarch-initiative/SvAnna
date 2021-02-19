package org.jax.svanna.io.parse;

import htsjdk.variant.variantcontext.Allele;
import htsjdk.variant.variantcontext.GenotypesContext;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFCodec;
import htsjdk.variant.vcf.VCFFileReader;
import htsjdk.variant.vcf.VCFHeader;
import htsjdk.variant.vcf.VCFHeaderVersion;
import org.jax.svanna.core.reference.SvannaVariant;
import org.monarchinitiative.svart.*;
import org.monarchinitiative.svart.util.VariantTrimmer;
import org.monarchinitiative.svart.util.VcfConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.jax.svanna.io.parse.Utils.makeVariantRepresentation;

/**
 * Parse variants stored in a VCF file. The parser is <em>NOT</em> thread safe!
 */
public class VcfVariantParser implements VariantParser<SvannaVariant> {

    private static final Logger LOGGER = LoggerFactory.getLogger(VcfVariantParser.class);

    private final VariantCallAttributeParser attributeParser;

    private final VcfConverter vcfConverter;

    private final boolean requireVcfIndex;

    public VcfVariantParser(GenomicAssembly assembly) {
        this(assembly, false);
    }

    public VcfVariantParser(GenomicAssembly assembly, boolean requireVcfIndex) {
        this.attributeParser = VariantCallAttributeParser.getInstance();
        this.vcfConverter = new VcfConverter(assembly, VariantTrimmer.leftShiftingTrimmer(VariantTrimmer.removingCommonBase()));
        this.requireVcfIndex = requireVcfIndex;
    }

    /**
     * Function to map a VCF line to {@link VariantContext}.
     */
    private static Function<String, Optional<VariantContext>> toVariantContext(VCFCodec codec) {
        return line -> {
            try {
                // codec returns null for VCF header lines
                return Optional.ofNullable(codec.decode(line));
            } catch (Exception e) {
                LOGGER.warn("Invalid VCF record: `{}`: `{}`", e.getMessage(), line);
                return Optional.empty();
            }
        };
    }

    @Override
    public Stream<SvannaVariant> createVariantAlleles(Path filePath) throws IOException {
        /*
        Sniffles VCF contains corrupted VCF records like

        CM000663.2STRANDBIAS	2324	N	<INV>	.	PASS	IMPRECISE;SVMETHOD=Snifflesv1.0.12;CHR2=CM000663.2;END=143208425;ZMW=7;STD_quant_start=181.061947;STD_quant_stop=166.287187;Kurtosis_quant_start=2.721142;Kurtosis_quant_stop=-0.666956;SVTYPE=INV;SUPTYPE=SR;SVLEN=18033775;STRANDS=++;STRANDS2=7,0,0,7;RE=7;REF_strand=49,61;Strandbias_pval=0.00467625;AF=0.0598291	GT:DR:DV	0/0:110:7

        which prevent us to use the code below:

        try (VCFFileReader reader = new VCFFileReader(filePath, requireVcfIndex)) {
            return reader.iterator().stream()
                    .map(toVariants())
                    .filter(Optional::isPresent)
                    .map(Optional::get);
        }

        So, this is a workaround that drops the corrupted lines:
         */
        VCFHeader header;
        try (VCFFileReader reader = new VCFFileReader(filePath, requireVcfIndex)) {
            header = reader.getHeader();
        }

        VCFCodec codec = new VCFCodec();
        codec.setVCFHeader(header, header.getVCFHeaderVersion() == null ? VCFHeaderVersion.VCF4_1: header.getVCFHeaderVersion());

        return Files.newBufferedReader(filePath).lines()
                .map(toVariantContext(codec))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(toVariants())
                .filter(Optional::isPresent)
                .map(Optional::get);
    }

    /**
     * One variant context might represent multiple sequence variants or a single symbolic variant/breakend.
     * This function melts the variant context to a collection of variants.
     * <p>
     * Multi-allelic sequence variants are supported, while multi-allelic symbolic variants are not.
     *
     * @return function that maps variant context to collection of {@link Variant}s
     */
    Function<VariantContext, Optional<? extends SvannaVariant>> toVariants() {
        return vc -> {
            Contig contig = vcfConverter.parseContig(vc.getContig());
            if (contig.isUnknown()) {
                if (LOGGER.isWarnEnabled())
                    LOGGER.warn("Unknown contig `{}` for variant `{}`", vc.getContig(), makeVariantRepresentation(vc));
                return Optional.empty();
            }

            List<Allele> alts = vc.getAlternateAlleles();
            if (alts.size() != 1) {
                if (LOGGER.isWarnEnabled())
                    LOGGER.warn("Parsing variant with {} (!=2) alt alleles is not supported: {}", alts.size(), makeVariantRepresentation(vc));
                return Optional.empty();
            }

            Allele altAllele = alts.get(0);
            String alt = altAllele.getDisplayString();

            if (VariantType.isSymbolic(alt)) {
                return (VariantType.isBreakend(alt))
                        ? parseBreakendAllele(vc, contig)
                        : parseSymbolicVariantAllele(vc, contig);
            } else
                return parseSequenceVariantAllele(vc, contig);
        };
    }

    private Optional<? extends SvannaVariant> parseSequenceVariantAllele(VariantContext vc, Contig contig) {
        VariantCallAttributes attrs = attributeParser.parseAttributes(vc.getAttributes(), vc.getGenotype(0));

        DefaultSvannaVariant.Builder builder = vcfConverter.convert(DefaultSvannaVariant.builder(),
                contig, vc.getID(), vc.getStart(),
                vc.getReference().getDisplayString(), vc.getAlternateAllele(0).getDisplayString());

        return Optional.of(builder.variantCallAttributes(attrs).build());
    }

    private Optional<? extends SvannaVariant> parseSymbolicVariantAllele(VariantContext vc, Contig contig) {
        // parse start pos and CIPOS
        ConfidenceInterval cipos;
        List<Integer> cp = vc.getAttributeAsIntList("CIPOS", 0);
        if (cp.isEmpty()) {
            cipos = ConfidenceInterval.precise();
        } else if (cp.size() == 2) {
            cipos = ConfidenceInterval.of(cp.get(0), cp.get(1));
        } else {
            if (LOGGER.isWarnEnabled())
                LOGGER.warn("Invalid CIPOS field `{}` in variant `{}`", vc.getAttributeAsString("CIPOS", ""), makeVariantRepresentation(vc));
            return Optional.empty();
        }
        Position start = Position.of(vc.getStart(), cipos);

        // parse end pos and CIEND
        ConfidenceInterval ciend;
        int endPos = vc.getAttributeAsInt("END", 0); // 0 is not allowed in 1-based VCF coordinate system
        if (endPos < 1) {
            if (LOGGER.isWarnEnabled())
                LOGGER.warn("Missing END field for variant `{}`", makeVariantRepresentation(vc));
            return Optional.empty();
        }
        List<Integer> ce = vc.getAttributeAsIntList("CIEND", 0);
        if (ce.isEmpty()) {
            ciend = ConfidenceInterval.precise();
        } else if (ce.size() == 2) {
            ciend = ConfidenceInterval.of(ce.get(0), ce.get(1));
        } else {
            if (LOGGER.isWarnEnabled())
                LOGGER.warn("Invalid CIEND field `{}` in variant `{}`", vc.getAttributeAsString("CIEND", ""), makeVariantRepresentation(vc));
            return Optional.empty();
        }
        Position end = Position.of(endPos, ciend);

        // we only support calls with 1 genotype
        GenotypesContext gts = vc.getGenotypes();
        if (gts.isEmpty()) {
            if (LOGGER.isWarnEnabled())
                LOGGER.warn("Parsing symbolic variant with no genotype call is not supported: {}", makeVariantRepresentation(vc));
            return Optional.empty();
        } else if (gts.size() > 1) {
            if (LOGGER.isWarnEnabled())
                LOGGER.warn("Parsing symbolic variants with >1 genotype calls is not supported: {}", makeVariantRepresentation(vc));
            return Optional.empty();
        }


        String ref = vc.getReference().getDisplayString();
        String alt = vc.getAlternateAllele(0).getDisplayString();
        int svlen = vc.getAttributeAsInt("SVLEN", 0);
        if (alt.equals("<INV>") && svlen != 0) { // happens in Sniffles input
            if (LOGGER.isInfoEnabled())
                LOGGER.info("Correcting SVLEN `{}!=0` for an inversion `{}`", svlen, makeVariantRepresentation(vc));
            svlen = 0;
        }

        VariantCallAttributes variantCallAttributes = attributeParser.parseAttributes(vc.getAttributes(), vc.getGenotype(0));
        DefaultSvannaVariant.Builder builder = vcfConverter.convertSymbolic(DefaultSvannaVariant.builder(), contig, vc.getID(), start, end, ref, alt, svlen);

        return Optional.of(builder.variantCallAttributes(variantCallAttributes).build());
    }

    private Optional<? extends SvannaVariant> parseBreakendAllele(VariantContext vc, Contig contig) {
        // sanity checks
        if (vc.getAlternateAlleles().size() > 1) {
            if (LOGGER.isWarnEnabled())
                LOGGER.warn("Multiple alt breakends are not yet supported for variant `{}`", makeVariantRepresentation(vc));
            return Optional.empty();
        }

        // parse pos and confidence intervals, if present
        List<Integer> ci = vc.getAttributeAsIntList("CIPOS", 0);
        ConfidenceInterval ciPos, ciEnd;
        if (ci.isEmpty()) {
            ciPos = ConfidenceInterval.precise();
        } else if (ci.size() == 2) {
            ciPos = ConfidenceInterval.of(ci.get(0), ci.get(1));
        } else {
            if (LOGGER.isWarnEnabled())
                LOGGER.warn("Invalid CIPOS attribute `{}` for variant `{}`", vc.getAttributeAsString("CIPOS", ""), makeVariantRepresentation(vc));
            return Optional.empty();
        }
        ci = vc.getAttributeAsIntList("CIEND", 0);
        if (ci.isEmpty()) {
            ciEnd = ConfidenceInterval.precise();
        } else if (ci.size() == 2) {
            ciEnd = ConfidenceInterval.of(ci.get(0), ci.get(1));
        } else {
            if (LOGGER.isWarnEnabled())
                LOGGER.warn("Invalid CIEND attribute `{}` for variant `{}`", vc.getAttributeAsString("CIEND", ""), makeVariantRepresentation(vc));
            return Optional.empty();
        }

        Position pos = Position.of(vc.getStart(), ciPos);

        String mateId = vc.getAttributeAsString("MATEID", "");
        String eventId = vc.getAttributeAsString("EVENT", "");

        VariantCallAttributes attrs = attributeParser.parseAttributes(vc.getAttributes(), vc.getGenotype(0));

        BreakendedSvannaVariant.Builder builder = vcfConverter.convertBreakend(BreakendedSvannaVariant.builder(), contig, vc.getID(), pos,
                vc.getReference().getDisplayString(), vc.getAlternateAllele(0).getDisplayString(),
                ciEnd, mateId, eventId);


        return Optional.of(builder.variantCallAttributes(attrs).build());
    }

}
