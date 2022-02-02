package org.jax.svanna.db.additive.dispatch;

import org.jax.svanna.core.LogUtils;
import org.jax.svanna.core.priority.additive.Event;
import org.jax.svanna.core.priority.additive.IntrachromosomalBreakendException;
import org.jax.svanna.core.priority.additive.Route;
import org.jax.svanna.core.priority.additive.Segment;
import org.monarchinitiative.svart.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

class RouteAssemblyUtils {

    private static final CoordinateSystem CS = CoordinateSystem.zeroBased();

    /**
     * Sort variants located in a single contig and adjust variant strands to the same strand. If there is a breakend
     * among the variants, variant strands of the other variants are adjusted to align with the breakend.
     * <p>
     * The method supports max 1 breakend to be present.
     *
     * @throws RouteAssemblyException if not possible to arrange the variants in a meaningful way
     */
    static VariantArrangement assemble(List<GenomicVariant> variants) throws RouteAssemblyException {
        if (variants.isEmpty()) throw new RouteAssemblyException("Variant list must not be empty");

        LinkedList<GenomicVariant> breakends = variants.stream()
                .filter(GenomicVariant::isBreakend)
                .collect(Collectors.toCollection(LinkedList::new));

        if (breakends.isEmpty())
            return assembleIntrachromosomal(variants);
        else if (breakends.size() == 1)
            return assembleInterchromosomal(variants, breakends.getFirst());
        else
            throw new RouteAssemblyException("Unable to assemble a list of " + breakends.size() + "(>1) breakend variants");
    }

    private static VariantArrangement assembleIntrachromosomal(List<GenomicVariant> variants) {
        long contigCount = variants.stream().map(GenomicVariant::contig).distinct().count();
        if (contigCount > 1)
            throw new RouteAssemblyException("Unable to assemble variants on " + contigCount + "(>1) contigs without knowing the breakend");
        if (variants.size() == 1)
            return VariantArrangement.intrachromosomal(variants);

        List<GenomicVariant> sortedByStart = variants.stream()
                .map(v -> v.withStrand(Strand.POSITIVE)) // this can fail if V does not override `withStrand`
                .sorted(Comparator.comparingInt(v -> v.startOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased())))
                .collect(Collectors.toUnmodifiableList());

        GenomicVariant previous = sortedByStart.get(0);
        for (GenomicVariant current : sortedByStart) {
            if (previous == current) continue;
            if (previous.overlapsWith(current))
                throw new RouteAssemblyException("Unable to assemble overlapping variants: "
                        + LogUtils.variantSummary(previous) + " " + LogUtils.variantSummary(current));
            previous = current;
        }

        return VariantArrangement.intrachromosomal(sortedByStart);
    }

    private static VariantArrangement assembleInterchromosomal(List<? extends GenomicVariant> variants, GenomicVariant breakendVariant) {
        GenomicBreakendVariant breakend = (GenomicBreakendVariant) breakendVariant;

        GenomicBreakend left = breakend.left();
        GenomicBreakend right = breakend.right();
        if (left.contig().equals(right.contig()))
            // TODO - evaluate
            throw new IntrachromosomalBreakendException("Intrachromosomal breakends are not currently supported: " + LogUtils.variantSummary(breakend));

        List<GenomicVariant> leftSorted = variants.stream()
                .filter(v -> v.contig().equals(left.contig()) && !v.equals(breakend))
                .sorted(Comparator.comparingInt(left::distanceTo))
                .map(v -> (GenomicVariant) v.withStrand(left.strand())) // this might fail if V does not override `withStrand`
                .collect(Collectors.toUnmodifiableList());
        for (GenomicVariant variant : leftSorted) {
            if (left.distanceTo(variant) > 0)
                throw new RouteAssemblyException("Variant " + LogUtils.variantSummary(variant) + " is not upstream of the breakend " + LogUtils.breakendSummary(left));
        }


        List<GenomicVariant> rightSorted = variants.stream()
                .filter(v -> v.contig().equals(right.contig()) && !v.equals(breakend))
                .sorted(Comparator.comparing(v -> right.distanceTo((Region<?>) v)).reversed())
                .map(v -> (GenomicVariant) v.withStrand(right.strand()))
                .collect(Collectors.toUnmodifiableList());
        for (GenomicVariant variant : rightSorted) {
            if (right.distanceTo(variant) < 0)
                throw new RouteAssemblyException("Variant " + LogUtils.variantSummary(variant) + " is not downstream of the breakend " + LogUtils.breakendSummary(right));
        }


        List<GenomicVariant> sortedVariants = new ArrayList<>();
        sortedVariants.addAll(leftSorted);
        sortedVariants.add(breakendVariant);
        sortedVariants.addAll(rightSorted);

        return VariantArrangement.interchromosomal(sortedVariants, leftSorted.size());
    }


    static List<Route> makeAltRouteForBreakendVariant(GenomicBreakendVariant bv, GenomicRegion leftGeneRegion, GenomicRegion rightGeneRegion) {
        GenomicBreakend left = bv.left();
        GenomicBreakend right = bv.right();

        // ------------------------------------ The LEFT segments ------------------------------------------------------
        List<Segment> leftSegments = new LinkedList<>();
        // upstream
        leftSegments.add(Segment.of(left.contig(), left.strand(), CS,
                leftGeneRegion.startOnStrandWithCoordinateSystem(left.strand(), CS), left.startWithCoordinateSystem(CS),
                "left-upstream", Event.GAP, 1));
        // left breakend
        Segment leftBndSegment = Segment.of(left.contig(), left.strand(), CS,
                left.startWithCoordinateSystem(CS), left.endWithCoordinateSystem(CS),
                left.id(), Event.BREAKEND, 1);
        leftSegments.add(leftBndSegment);

        if (bv.changeLength() != 0) { // there is an inserted sequence within breakend
            leftSegments.add(Segment.insertion(left.contig(), left.strand(), CS,
                    left.endWithCoordinateSystem(CS), left.endWithCoordinateSystem(CS), "ins" + bv.eventId(), bv.changeLength()));
        }

        // right breakend
        Segment rightBndSegment = Segment.of(right.contig(), right.strand(), CS,
                right.startWithCoordinateSystem(CS), right.endWithCoordinateSystem(CS), right.id(), Event.BREAKEND, 1);
        leftSegments.add(rightBndSegment);

        // downstream
        leftSegments.add(Segment.of(right.contig(), right.strand(), CS,
                right.endWithCoordinateSystem(CS), rightGeneRegion.endOnStrandWithCoordinateSystem(right.strand(), CS), "right-downstream", Event.GAP, 1));


        // ------------------------------------ The RIGHT segments -----------------------------------------------------
        List<Segment> rightSegments = new LinkedList<>();
        // upstream
        rightSegments.add(Segment.of(right.contig(), right.strand(), CS,
                rightGeneRegion.startOnStrandWithCoordinateSystem(right.strand(), CS), right.startWithCoordinateSystem(CS),
                "right-upstream", Event.GAP, 1));
        // right breakend
        rightSegments.add(rightBndSegment);
        // left breakend
        rightSegments.add(leftBndSegment);
        // downstream
        rightSegments.add(Segment.of(left.contig(), left.strand(), CS,
                left.endWithCoordinateSystem(CS), leftGeneRegion.endOnStrandWithCoordinateSystem(left.strand(), CS), "left-downstream", Event.GAP, 1));

        return List.of(Route.of(leftSegments), Route.of(rightSegments));
    }
}
