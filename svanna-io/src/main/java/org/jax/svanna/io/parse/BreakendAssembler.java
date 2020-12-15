package org.jax.svanna.io.parse;

import htsjdk.variant.variantcontext.VariantContext;
import org.jax.svanna.core.reference.SvannaVariant;
import org.monarchinitiative.variant.api.*;
import org.monarchinitiative.variant.api.impl.PartialBreakend;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.jax.svanna.io.parse.Utils.makeVariantRepresentation;


class BreakendAssembler {

    private static final Logger LOGGER = LoggerFactory.getLogger(BreakendAssembler.class);

    private static final NumberFormat NF = NumberFormat.getInstance();

    private static final VariantCallAttributeParser ATTRIBUTE_PARSER = VariantCallAttributeParser.getInstance();

    /**
     * Any BND alt record must match this pattern.
     * E.g.: `G]1:123]`, `]1:123]G`, `G[1:123[`, `[1:123[G`
     */
    private static final Pattern BND_ALT_PATTERN = Pattern.compile(
            String.format("^(?<head>[%s]*)(?<left>[\\[\\]])(?<contig>[\\w._<>]+):(?<pos>\\d+)(?<right>[\\[\\]])(?<tail>[%s]*)$",
                    Utils.IUPAC_BASES, Utils.IUPAC_BASES));

    private final GenomicAssembly assembly;

    private final VariantCallAttributeParser attributeParser;

    BreakendAssembler(GenomicAssembly assembly, VariantCallAttributeParser attributeParser) {
        this.assembly = assembly;
        this.attributeParser = attributeParser;
    }

    public Optional<SvannaVariant> resolveBreakends(VariantContext vc) {
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

        Position pos = Position.of(vc.getStart(), ciPos);

        // this allele is on POSITIVE strand by VCF specification
        String refOnPositive = vc.getReference().getDisplayString().toUpperCase();
        if (refOnPositive.length() != 1) {
            LOGGER.warn("Ref allele with {}!=1 for breakend {}", refOnPositive.length(), vRepr);
            return Optional.empty();
        }
        String alt = vc.getAlternateAllele(0).getDisplayString().toUpperCase(Locale.ROOT);

        Matcher altMatcher = BND_ALT_PATTERN.matcher(alt);
        if (!altMatcher.matches()) {
            LOGGER.warn("Invalid breakend alt `{}` in `{}`", alt, vRepr);
            return Optional.empty();
        }

        // parse mate data
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
            matePos = Position.of(p, ciEnd);
        } catch (ParseException e) {
            LOGGER.warn("Invalid mate position `{}` for variant `{}`", matePosString, vRepr);
            return Optional.empty();
        }

        // figure out strands and the inserted sequence
        // left strand
        String head = altMatcher.group("head");
        String tail = altMatcher.group("tail");
        if (!head.isEmpty() && !tail.isEmpty()) {
            LOGGER.warn("Sequence present both at the beginning (`{}`) and the end (`{}`) of alt field for variant `{}`", head, tail, vRepr);
        }
        Strand strand;
        char refBase = refOnPositive.charAt(0);
        if (!head.isEmpty() && refBase == head.charAt(0)) {
            strand = Strand.POSITIVE;
        } else if (!tail.isEmpty() && refBase == tail.charAt(tail.length() - 1)) {
            strand = Strand.NEGATIVE;
        } else {
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

        Breakend left = strand.isPositive()
                ? PartialBreakend.zeroBased(contig, vc.getID(), Strand.POSITIVE, pos).withStrand(strand)
                : PartialBreakend.oneBased(contig, vc.getID(), Strand.POSITIVE, pos).withStrand(strand);
        Breakend right = mateStrand.isPositive()
                ? PartialBreakend.oneBased(mateContig, mateId, Strand.POSITIVE, matePos).withStrand(mateStrand)
                : PartialBreakend.zeroBased(mateContig, mateId, Strand.POSITIVE, matePos).withStrand(mateStrand);

        // alt allele
        String altSeq = strand.isPositive()
                ? head.substring(1)
                : tail.substring(0, tail.length() - 1);

        VariantCallAttributes variantCallAttributes = ATTRIBUTE_PARSER.parseAttributes(vc.getAttributes(), vc.getGenotype(0));

        return Optional.of(BreakendedSvannaVariant.of(eventId, left, right,
                strand.isPositive() ? refOnPositive : Utils.reverseComplement(refOnPositive),
                strand.isPositive() ? altSeq : Utils.reverseComplement(altSeq),
                variantCallAttributes));
    }

}
