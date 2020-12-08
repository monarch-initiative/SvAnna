package org.jax.svanna.io.parse;

import htsjdk.variant.variantcontext.Allele;
import htsjdk.variant.variantcontext.GenotypesContext;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFFileReader;
import org.jax.svanna.core.reference.SvannaVariant;
import org.jax.svanna.core.reference.Zygosity;
import org.monarchinitiative.variant.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.jax.svanna.io.parse.Utils.makeVariantRepresentation;

/**
 * Parse variants stored in a VCF file.
 */
public class VcfVariantParser implements VariantParser<SvannaVariant> {

    private static final Logger LOGGER = LoggerFactory.getLogger(VcfVariantParser.class);

    private final GenomicAssembly assembly;

    private final BreakendAssembler breakendAssembler;

    private final boolean requireVcfIndex;

    public VcfVariantParser(GenomicAssembly assembly) {
        this(assembly, false);
    }

    public VcfVariantParser(GenomicAssembly assembly, boolean requireVcfIndex) {
        this.assembly = assembly;
        this.breakendAssembler = new BreakendAssembler(assembly);
        this.requireVcfIndex = requireVcfIndex;
    }

    @Override
    public Stream<SvannaVariant> createVariantAlleles(Path filePath) throws IOException {
        try (VCFFileReader reader = new VCFFileReader(filePath, requireVcfIndex)) {
            return reader.iterator().stream()
                    .map(toVariants())
                    .flatMap(Collection::stream);
        }
    }

    /**
     * One variant context might represent multiple sequence variants or a single symbolic variant/breakend.
     * This function melts the variant context to a collection of variants.
     * <p>
     * Multi-allelic sequence variants are supported, while multi-allelic symbolic variants are not.
     *
     * @return function that maps variant context to collection of {@link Variant}s
     */
    Function<VariantContext, Collection<SvannaVariant>> toVariants() {
        return vc -> {
            String vRepr = makeVariantRepresentation(vc);

            List<Allele> alts = vc.getAlternateAlleles();
            List<SvannaVariant> variants = new ArrayList<>(alts.size());
            for (int altAlleleIdx = 0; altAlleleIdx < alts.size(); altAlleleIdx++) {
                Allele altAllele = alts.get(altAlleleIdx);
                String alt = altAllele.getDisplayString();
                Optional<? extends SvannaVariant> variantOptional;
                if (VariantType.isBreakend(alt)) {
                    // breakend
                    if (alts.size() > 1) {
                        LOGGER.warn("Parsing breakend variant with {} (>1) alt alleles is not supported: {}", alts.size(), vRepr);
                        return List.of();
                    }
                    variantOptional = breakendAssembler.resolveBreakends(vc);
                } else if (VariantType.isLargeSymbolic(alt)) {
                    // symbolic
                    if (alts.size() > 1) {
                        LOGGER.warn("Parsing symbolic variant with >1 ({}) alt alleles is not supported: {}", alts.size(), vRepr);
                        return List.of();
                    }
                    variantOptional = parseIntrachromosomalVariantAllele(vc);
                } else {
                    // sequence variant
                    variantOptional = parseSequenceVariantAllele(vc, altAlleleIdx);
                }
                variantOptional.ifPresent(variants::add);
            }
            return variants;
        };
    }

    private Optional<? extends SvannaVariant> parseSequenceVariantAllele(VariantContext vc, int altAlleleIdx) {
        String contigName = vc.getContig();
        Contig contig = assembly.contigByName(contigName);
        if (contig == null) {
            LOGGER.warn("Unknown contig `{}` in variant `{}-{}:({})`", contigName, vc.getContig(), vc.getStart(), vc.getID());
            return Optional.empty();
        }

        Allele alt = vc.getAlternateAllele(altAlleleIdx);
        GenotypesContext gts = vc.getGenotypes();
        Zygosity zygosity = Utils.parseZygosity(altAlleleIdx, gts);
        int depthOfCoverage = Utils.parseDepthFromGenotype(altAlleleIdx, gts);

        return Optional.of(SvannaSequenceVariant.oneBased(contig, vc.getID(), vc.getStart(),
                vc.getReference().getDisplayString(), alt.getDisplayString(),
                zygosity, depthOfCoverage));
    }

    private Optional<? extends SvannaVariant> parseIntrachromosomalVariantAllele(VariantContext vc) {
        String vr = makeVariantRepresentation(vc);

        // parse contig
        String contigName = vc.getContig();
        Contig contig = assembly.contigByName(contigName);
        if (contig == null) {
            LOGGER.warn("Unknown contig `{}` in variant `{}`", contigName, vr);
            return Optional.empty();
        }

        // parse start pos and CIPOS
        ConfidenceInterval cipos;
        List<Integer> cp = vc.getAttributeAsIntList("CIPOS", 0);
        if (cp.isEmpty()) {
            cipos = ConfidenceInterval.precise();
        } else if (cp.size() == 2) {
            cipos = ConfidenceInterval.of(cp.get(0), cp.get(1));
        } else {
            LOGGER.warn("Invalid CIPOS field `{}` in variant `{}`", vc.getAttributeAsString("CIPOS", ""), vr);
            return Optional.empty();
        }
        Position start = Position.of(vc.getStart(), cipos);

        // parse end pos and CIEND
        ConfidenceInterval ciend;
        int endPos = vc.getAttributeAsInt("END", 0); // 0 is not allowed in 1-based VCF coordinate system
        if (endPos < 1) {
            LOGGER.warn("Missing END field for variant `{}`", vr);
            return Optional.empty();
        }
        List<Integer> ce = vc.getAttributeAsIntList("CIEND", 0);
        if (ce.isEmpty()) {
            ciend = ConfidenceInterval.precise();
        } else if (ce.size() == 2) {
            ciend = ConfidenceInterval.of(ce.get(0), ce.get(1));
        } else {
            LOGGER.warn("Invalid CIEND field `{}` in variant `{}`", vc.getAttributeAsString("CIEND", ""), vr);
            return Optional.empty();
        }
        Position end = Position.of(endPos, ciend);

        // assemble the results
        String ref = vc.getReference().getDisplayString();
        String alt = vc.getAlternateAllele(0).getDisplayString();
        int svlen = vc.getAttributeAsInt("SVLEN", 0);

        // parse depth & zygosity
        GenotypesContext gts = vc.getGenotypes();
        if (gts.size() > 1) {
            LOGGER.warn("Parsing symbolic variants with >1 alleles is not supported: {}", vr);
            return Optional.empty();
        }
        Zygosity zygosity = Utils.parseZygosity(0, gts);
        int depthOfCoverage = Utils.parseDepthFromGenotype(0, gts);



        return Optional.of(SvannaSymbolicVariant.of(contig, vc.getID(), Strand.POSITIVE, CoordinateSystem.ONE_BASED,
                start, end, ref, alt,
                svlen,
                zygosity, depthOfCoverage));
    }

}
