package org.jax.svann.parse;

import org.jax.svann.reference.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;

/**
 * Assemble {@link org.jax.svann.parse.BreakendRecord} into {@link SequenceRearrangement}s.
 * <p>
 * Assembler works under these assumptions:
 * <ul>
 *     <li>breakend record might have <code>EVENT</code> attribute</li>
 *     <li>breakend record must have <code>MATEID</code> attribute</li>
 * </ul>
 * <p>
 */
public class BreakendAssemblerImpl implements BreakendAssembler<StructuralVariant> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BreakendAssemblerImpl.class);

    private static final Charset ASCII = StandardCharsets.US_ASCII;

    /**
     * Any BND alt record must match this pattern.
     * E.g.: `G]1:123]`, `]1:123]G`, `G[1:123[`, `[1:123[G`
     */
    private static final Pattern BND_ALT_PATTERN = Pattern.compile(
            String.format("^(?<head>[%s]*)[\\[\\]][\\w:._<>]+[\\[\\]](?<tail>[%s]*)$", Utils.IUPAC_BASES, Utils.IUPAC_BASES));

    /**
     * Figure out strand of the mate breakend from the ALT allele record.
     *
     * @param alt allele, e.g. `A]chr1:123456]`
     * @return strand of the mate breakend
     */
    private static Strand figureOutStrandOfTheMate(String alt) {
        /*
        Which strand is the mate record located on?
        - FWD - brackets are in `]` direction in the ALT field (e.g. `A]chr1:123456]`, `]chr1:123456]A`)
        - REV - brackets are in `[` direction in the ALT field (e.g. `C[chr1:123456[`, `[chr1:123456[C`)
         */
        char[] brackets = new char[2];
        int bracketsFound = 0;
        for (char c : alt.toCharArray()) {
            if (c == '[' || c == ']') {
                if (bracketsFound < 2) {
                    brackets[bracketsFound] = c;
                } else {
                    // more than 2 bracket symbols
                    return null;
                }
                bracketsFound++;
            }
        }
        if (brackets[0] != brackets[1]) {
            // brackets of different orientation
            return null;
        }
        if (brackets[0] == ']') {
            // the mate record is extending to the left of this record, hence this record must be on FWD strand
            return Strand.FWD;
        } else {
            // the mate record is extending to the right of this record, hence this record must be on REV strand
            return Strand.REV;
        }
    }

    static Optional<? extends StructuralVariant> assembleBreakendRecords(BreakendRecord left, BreakendRecord right) {
        // 0 - sanity check, any BND alt must match this pattern
        String leftAlt = left.getAlt();
        Matcher leftAltMatcher = BND_ALT_PATTERN.matcher(leftAlt);
        String rightAlt = right.getAlt();
        Matcher rightAltMatcher = BND_ALT_PATTERN.matcher(rightAlt);
        if (!leftAltMatcher.matches() || !rightAltMatcher.matches()) {
            LOGGER.warn("Invalid ALT breakend in mates: `{}`, `{}`", left, right);
            return Optional.empty();
        }

        // 1 - figure out mate strands
        Strand leftStrand = figureOutStrandOfTheMate(right.getAlt());
        if (leftStrand == null) {
            LOGGER.warn("Invalid ALT breakend `{}` for {}", leftAlt, left);
            return Optional.empty();
        }

        Strand rightStrand = figureOutStrandOfTheMate(left.getAlt());
        if (rightStrand == null) {
            LOGGER.warn("Invalid ALT breakend `{}` for {}", rightAlt, right);
            return Optional.empty();
        }
        rightStrand = rightStrand.getOpposite(); // this needs to be done but don't ask me why

        // 2 - make positions
        // Note, the position coordinates are always specified on FWD strand.
        ChromosomalPosition leftPos = left.getPosition().withStrand(leftStrand);
        String leftRef = leftStrand.isForward()
                ? left.getRef()
                : Utils.reverseComplement(left.getRef());
        Breakend leftBreakend = BreakendDefault.impreciseWithRef(leftPos.getContig(),
                leftPos.getPosition(),
                leftPos.getCi(),
                leftStrand,
                CoordinateSystem.ONE_BASED,
                left.getId(),
                leftRef);

        ChromosomalPosition rightPos = right.getPosition().withStrand(rightStrand);
        String rightRef = rightStrand.isForward()
                ? right.getRef()
                : Utils.reverseComplement(right.getRef());
        Breakend rightBreakend = BreakendDefault.impreciseWithRef(rightPos.getContig(),
                rightPos.getPosition(),
                rightPos.getCi(),
                rightStrand,
                CoordinateSystem.ONE_BASED,
                right.getId(),
                rightRef);

        // 3 - figure out SvType and the inserted sequence (if any)
        /*
         There are 3 SvTypes that can be described using a single adjacency:
         - TRANSLOCATION
         - DELETION
         - TANDEM DUPLICATION
         */
        SvType type;
        if (leftPos.getContig().getId() != rightPos.getContig().getId()) {
            // positions on different chromosomes -> TRANSLOCATION
            type = SvType.TRANSLOCATION;
        } else {
            if (!leftStrand.equals(rightStrand)) {
                // this should not happen. Intrachromosomal events with an adjacency joining different strands
                // are possible, but multiple adjacencies should be present for such events.
                LOGGER.warn("Unexpected breakend coordinates on different strands: {}, {}", left, right);
                return Optional.empty();
            }
            if (leftPos.getPosition() < rightPos.getPosition()) {
                // left is upstream from right -> DELETION
                type = SvType.DELETION;
            } else if (leftPos.getPosition() > rightPos.getPosition()) {
                // left is downstream from right ->  DUPLICATION
                type = SvType.DUPLICATION;
            } else {
                // this should not happen
                LOGGER.warn("Unexpected equality of coordinates: {}, {}", left, right);
                return Optional.empty();
            }
        }

        // 4 - figure out if there is an inserted sequence
        // we can get the inserted sequence from any allele, let's use left
        byte[] inserted = getInsertedSequence(leftAltMatcher, leftStrand);
        int depthOfCoverage = Math.min(left.depthOfCoverage(), right.depthOfCoverage());

        AdjacencyDefault adjacency = AdjacencyDefault.withInsertedSequenceAndDepth(leftBreakend, rightBreakend, inserted, depthOfCoverage);
        return Optional.of(StructuralVariantDefault.of(type, adjacency));
    }

    private static byte[] getInsertedSequence(Matcher altMatcher, Strand strand) {
        String head = altMatcher.group("head");
        String tail = altMatcher.group("tail");
        byte[] seq = head.isEmpty()
                ? tail.getBytes(ASCII)
                : head.getBytes(ASCII);

        return strand.isForward()
                ? seq
                : Utils.reverseComplement(seq);
    }

    /**
     * Assemble a list of breakends into a complex sequence rearrangement using `EVENT` info field.
     *
     * @return optional with the rearrangement or empty optional if
     */
    private static Function<List<BreakendRecord>, Optional<StructuralVariant>> assembleEvents() {
        return records -> {
            // TODO: 28. 10. 2020 implement
            LOGGER.warn("Event-based assembly is not yet supported");
            return Optional.empty();
        };
    }

    @Override
    public List<StructuralVariant> assemble(Collection<BreakendRecord> breakends) {
        List<StructuralVariant> rearrangements = new ArrayList<>();

        /*
         * First, group events by event ID
         */
        Map<String, List<BreakendRecord>> bndByEvent = breakends.stream()
                .filter(record -> record.getEventId() != null)
                .collect(Collectors.groupingBy(BreakendRecord::getEventId));
        // IDs of breakends that will be assembled using event ID. We won't assemble these breakends using mate ID.
        Set<String> eventBndIds = bndByEvent.values().stream()
                .flatMap(Collection::stream)
                .map(BreakendRecord::getId)
                .collect(Collectors.toSet());

        // assemble records using the event ID
        bndByEvent.values().stream()
                .map(assembleEvents())
                .filter(Optional::isPresent)
                .map(Optional::get)
                .forEach(rearrangements::add);

        /*
         * Then, group events by IDs.
         */
        // group the remaining breakends by ID
        Map<String, BreakendRecord> bndById = breakends.stream()
                .filter(record -> record.getId() != null && !eventBndIds.contains(record.getId()))
                .collect(toMap(BreakendRecord::getId, Function.identity()));
        //
        Set<String> processedRecordIds = new HashSet<>();

        for (BreakendRecord record : bndById.values()) {
            String id = record.getId();
            String mateId = record.getMateId();
            if (id == null || mateId == null) {
                // do not process a breakend where any ID is missing
                continue;
            }

            if (processedRecordIds.contains(id)) {
                // we've already processed this record as mate of another record
                continue;
            }

            assembleBreakendRecords(record, bndById.get(mateId)).ifPresent(rearrangements::add);
            processedRecordIds.add(id);
            processedRecordIds.add(mateId);
        }

        return rearrangements;
    }

}
