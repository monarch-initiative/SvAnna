package org.jax.svanna.core.parse;

import htsjdk.variant.variantcontext.VariantContext;
import org.monarchinitiative.variant.api.*;
import org.monarchinitiative.variant.api.impl.BreakendVariant;
import org.monarchinitiative.variant.api.impl.PartialBreakend;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.jax.svanna.core.parse.VcfVariantParser.makeVariantRepresentation;


class BreakendAssembler {

    // TODO: 24. 11. 2020 should we adjust the REF and ALT alleles if left breakend is on NEG strand?

    private static final Logger LOGGER = LoggerFactory.getLogger(BreakendAssembler.class);

    private static final NumberFormat NF = NumberFormat.getInstance();

    /**
     * Any BND alt record must match this pattern.
     * E.g.: `G]1:123]`, `]1:123]G`, `G[1:123[`, `[1:123[G`
     */
    private static final Pattern BND_ALT_PATTERN = Pattern.compile(
            String.format("^(?<head>[%s]*)(?<left>[\\[\\]])(?<contig>[\\w._<>]+):(?<pos>\\d+)(?<right>[\\[\\]])(?<tail>[%s]*)$",
                    Utils.IUPAC_BASES, Utils.IUPAC_BASES));

    private final GenomicAssembly assembly;

    BreakendAssembler(GenomicAssembly assembly) {
        this.assembly = assembly;
    }

    Optional<BreakendVariant> resolveBreakends(VariantContext vc) {
        String vRepr = makeVariantRepresentation(vc); // for logging

        // sanity checks
        String contigName = vc.getContig();
        Contig contig = assembly.contigByName(contigName);
        if (contig == null) {
            LOGGER.warn("Unknown contig `{}` in variant `{}`", contigName, vRepr);
            return Optional.empty();
        }
        if (vc.getAlternateAlleles().size() > 1) {
            LOGGER.warn("Multiple alt breakends are not yet supported for variant `{}`", vRepr);
            return Optional.empty();
        }

        String eventId = vc.getAttributeAsString("EVENT", "");
        String mateId = vc.getAttributeAsString("MATEID", null);
        if (mateId == null) {
            LOGGER.warn("Missing required `MATEID` field for breakend variant `{}`", vRepr);
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
            LOGGER.warn("Invalid CIPOS attribute `{}` for variant `{}`", vc.getAttributeAsString("CIPOS", ""), vRepr);
            return Optional.empty();
        }
        ci = vc.getAttributeAsIntList("CIEND", 0);
        if (ci.isEmpty()) {
            ciEnd = ConfidenceInterval.precise();
        } else if (ci.size() == 2) {
            ciEnd = ConfidenceInterval.of(ci.get(0), ci.get(1));
        } else {
            LOGGER.warn("Invalid CIEND attribute `{}` for variant `{}`", vc.getAttributeAsString("CIEND", ""), vRepr);
            return Optional.empty();
        }

        Position pos = Position.of(CoordinateSystem.ONE_BASED, vc.getStart(), ciPos);

        // this allele is on POSITIVE strand by VCF specification
        String refOnPositive = vc.getReference().getDisplayString();
        String alt = vc.getAlternateAllele(0).getDisplayString();

        Matcher altMatcher = BND_ALT_PATTERN.matcher(alt);
        if (!altMatcher.matches()) {
            LOGGER.warn("Invalid breakend alt `{}` in `{}`", alt, vRepr);
            return Optional.empty();
        }

        // pares mate data
        String mateContigName = altMatcher.group("contig");
        Contig mateContig = assembly.contigByName(mateContigName);
        if (mateContig == null) {
            LOGGER.warn("Unknown mate contig `{}` for variant `{}`", mateContigName, vRepr);
            return Optional.empty();
        }
        String matePosString = altMatcher.group("pos");
        Position matePos;
        try {
            int p = NF.parse(matePosString).intValue();
            matePos = Position.of(CoordinateSystem.ONE_BASED, p, ciEnd);
        } catch (ParseException e) {
            LOGGER.warn("Invalid mate position `{}` for variant `{}`", matePosString, vRepr);
            return Optional.empty();
        }

        // figure out strands
        // left strand
        String head = altMatcher.group("head");
        String tail = altMatcher.group("tail");
        Strand strand = refOnPositive.equals(head)
                ? Strand.POSITIVE
                : refOnPositive.equals(tail)
                ? Strand.NEGATIVE
                : null;
        if (strand == null) {
            LOGGER.warn("Invalid breakend alt `{}`, no match for ref allele `{}` neither at the beginning, nor at the end for variant `{}`", alt, refOnPositive, vRepr);
            return Optional.empty();
        }

        // right strand
        String leftBracket = altMatcher.group("left");
        String rightBracket = altMatcher.group("right");
        if (!leftBracket.equals(rightBracket)) {
            LOGGER.warn("Invalid bracket orientation in `{}` for variant `{}`", alt, vRepr);
            return Optional.empty();
        }
        Strand mateStrand = (leftBracket.equals("["))
                ? Strand.POSITIVE
                : Strand.NEGATIVE;

        Breakend left = PartialBreakend.of(contig, pos, Strand.POSITIVE, vc.getID()).withStrand(strand);
        Breakend right = PartialBreakend.of(mateContig, matePos, Strand.POSITIVE, mateId).withStrand(mateStrand);

        return Optional.of(new BreakendVariant(eventId, left, right, strand.isPositive() ? refOnPositive : Utils.reverseComplement(refOnPositive), alt));
    }

}