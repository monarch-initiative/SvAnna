package org.jax.svanna.db.additive.dispatch;

import org.jax.svanna.core.exception.LogUtils;
import org.monarchinitiative.svart.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

class RouteAssembly {

    static <V extends Variant> VariantArrangement<V> assemble(List<V> variants) throws RouteAssemblyException {
        if (variants.isEmpty()) throw new RouteAssemblyException("Variant list must not be empty");

        List<V> breakends = variants.stream()
                .filter(v -> v instanceof BreakendVariant)
                .collect(Collectors.toList());
        if (breakends.isEmpty())
            return assembleIntrachromosomal(variants);
        else if (breakends.size() == 1)
            return assembleInterchromosomal(variants, breakends.get(0));
        else
            throw new RouteAssemblyException("Unable to assemble a list of " + breakends.size() + "(>1) breakend variants");
    }

    private static <V extends Variant> VariantArrangement<V> assembleIntrachromosomal(List<V> variants) {
        long contigCount = variants.stream().map(Variant::contig).distinct().count();
        if (contigCount > 1)
            throw new RouteAssemblyException("Unable to assemble variants on " + contigCount + "(>1) contigs without knowing the breakend");
        if (variants.size() == 1)
            return VariantArrangement.intrachromosomal(variants);

        List<V> startSorted = variants.stream()
                .map(v -> (V) v.withStrand(Strand.POSITIVE))
                .sorted(Comparator.comparingInt(v -> v.startOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased())))
                .collect(Collectors.toList());

        V previous = startSorted.get(0);
        for (V current : startSorted) {
            if (previous == current) continue;
            if (previous.overlapsWith(current))
                throw new RouteAssemblyException("Unable to assemble overlapping variants: "
                        + LogUtils.variantSummary(previous) + " " + LogUtils.variantSummary(current));
            previous = current;
        }

        return VariantArrangement.intrachromosomal(startSorted);
    }

    private static <V extends Variant> VariantArrangement<V> assembleInterchromosomal(List<V> variants, V breakendVariant) {
        BreakendVariant breakend = (BreakendVariant) breakendVariant;

        Breakend left = breakend.left();
        Breakend right = breakend.right();
        if (left.contig().equals(right.contig()))
            throw new RouteAssemblyException("Intrachromosomal breakends are not currently supported: " + LogUtils.variantSummary(breakend));

        List<V> leftSorted = variants.stream()
                .filter(v -> v.contig().equals(left.contig()) && !v.equals(breakend))
                .sorted(Comparator.comparingInt(left::distanceTo))
                .map(v -> (V) v.withStrand(left.strand()))
                .collect(Collectors.toList());
        for (V variant : leftSorted) {
            if (left.distanceTo(variant) > 0)
                throw new RouteAssemblyException("Variant " + LogUtils.variantSummary(variant) + " is not upstream of the breakend " + LogUtils.breakendSummary(left));
        }


        List<V> rightSorted = variants.stream()
                .filter(v -> v.contig().equals(right.contig()) && !v.equals(breakend))
                .sorted(Comparator.comparing(v -> right.distanceTo((Region<?>) v)).reversed())
                .map(v -> (V) v.withStrand(right.strand()))
                .collect(Collectors.toList());
        for (V variant : rightSorted) {
            if (right.distanceTo(variant) < 0)
                throw new RouteAssemblyException("Variant " + LogUtils.variantSummary(variant) + " is not downstream of the breakend " + LogUtils.breakendSummary(right));
        }


        List<V> sortedVariants = new ArrayList<>();
        sortedVariants.addAll(leftSorted);
        sortedVariants.add(breakendVariant);
        sortedVariants.addAll(rightSorted);

        return VariantArrangement.interchromosomal(sortedVariants, leftSorted.size());
    }

}
