package org.jax.svanna.db.metadata;

import org.jax.svanna.core.landscape.TadBoundary;
import org.jax.svanna.core.priority.additive.*;
import org.monarchinitiative.svart.*;

import javax.sql.DataSource;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class DispatcherDb<T extends Variant> implements Dispatcher<T> {

    private final PaddingTadBoundaryDao dao;

    public DispatcherDb(DataSource dataSource, GenomicAssembly genomicAssembly, double stabilityThreshold) {
        this.dao = new PaddingTadBoundaryDao(dataSource, genomicAssembly, stabilityThreshold);
    }

    @Override
    public Routes assembleRoutes(List<T> variants) throws DispatchException {
        Neighborhood neighborhood = exploreNeighborhood(variants);
        return buildRoutes(neighborhood, variants);
    }

    private Neighborhood exploreNeighborhood(List<T> variants) {
        if (variants.isEmpty())
            throw new DispatchException("Unable to dispatch with no variants");

        long nBreakends = variants.stream()
                .filter(v -> v instanceof BreakendVariant)
                .count();

        if (nBreakends == 0) {
            return intrachromosomalNeighborhood(variants);
        } else if (nBreakends == 1) {
            return interchromosomalNeighborhood(variants);
        } else {
            throw new DispatchException("Unable to dispatch for variants with " + nBreakends + ">1 breakends");
        }
    }

    private Neighborhood intrachromosomalNeighborhood(List<T> variants) {
        long nContigs = variants.stream()
                .map(Variant::contigId).distinct()
                .count();
        if (nContigs != 1)
            throw new DispatchException("Unable to dispatch with variants on " + nContigs + ">1 contigs");
        LinkedList<T> sortedVariants = variants.stream().sorted(comparingByPositionOnPositiveStrand()).collect(Collectors.toCollection(LinkedList::new));
        T first = sortedVariants.getFirst();
        T last = sortedVariants.getLast();

        TadBoundaryPair tadPair = dao.getBoundaryPair(
                first.contig(), first.startOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased()),
                last.contig(), last.endOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased())
        );
        GenomicRegion upstream = GenomicRegion.of(first.contig(), first.strand(), first.coordinateSystem(), tadPair.upstream().asPosition(), first.startPosition());
        GenomicRegion downstream = GenomicRegion.of(last.contig(), last.strand(), last.coordinateSystem(), last.endPosition(), tadPair.downstream().asPosition());
        return Neighborhood.of(upstream, downstream, downstream);
    }

    private Neighborhood interchromosomalNeighborhood(List<T> variants) {
        Optional<BreakendVariant> bndOpt = variants.stream()
                .filter(e -> e instanceof BreakendVariant)
                .map(v -> ((BreakendVariant) v))
                .findFirst();
        if (bndOpt.isEmpty())
            throw new DispatchException("Did not find breakend even though at least one was supposed to be present among the variants " + variants);

        BreakendVariant bv = bndOpt.get();

        Breakend left = bv.left();
        GenomicRegion upstreamBoundary = variants.stream()
                .filter(v -> !(v instanceof BreakendVariant)
                        && v.contig().equals(left.contig())
                        && v.endOnStrandWithCoordinateSystem(left.strand(), left.coordinateSystem()) < left.start())
                .min(comparingByPositionOnStrand(left.strand()))
                .map(r -> ((GenomicRegion) r))
                .orElse(left);

        Breakend right = bv.right();
        GenomicRegion downstreamBoundary = variants.stream()
                .filter(v -> !(v instanceof BreakendVariant)
                        && v.contig().equals(right.contig())
                        && v.startOnStrandWithCoordinateSystem(right.strand(), right.coordinateSystem()) > right.end())
                .max(comparingByPositionOnStrand(right.strand()))
                .map(r -> ((GenomicRegion) r))
                .orElse(right);

        Position upstreamBound = dao.upstreamOf(upstreamBoundary)
                .map(TadBoundary::asPosition)
                .orElse(Position.of(1));
        Position downstreamRefBound = dao.downstreamOf(left)
                .map(TadBoundary::asPosition)
                .orElse(Position.of(left.contig().length()));
        Position downstreamAltBound = dao.downstreamOf(downstreamBoundary)
                .map(TadBoundary::asPosition)
                .orElse(Position.of(right.contig().length()));

        GenomicRegion upstream = GenomicRegion.of(left.contig(), left.strand(), left.coordinateSystem(), upstreamBound, left.startPosition());
        GenomicRegion downstreamRef = GenomicRegion.of(left.contig(), left.strand(), left.coordinateSystem(), left.endPosition(), downstreamRefBound);
        GenomicRegion downstreamAlt = GenomicRegion.of(right.contig(), right.strand(), right.coordinateSystem(), right.startPosition(), downstreamAltBound);

        return Neighborhood.of(upstream, downstreamRef, downstreamAlt);
    }



    private static <T extends Variant> Comparator<? super T> comparingByPositionOnPositiveStrand() {
        return comparingByPositionOnStrand(Strand.POSITIVE);
    }

    private static <T extends Variant> Comparator<? super T> comparingByPositionOnStrand(Strand strand) {
        return Comparator.comparingInt((T t) -> t.startOnStrandWithCoordinateSystem(strand, CoordinateSystem.zeroBased()))
                .thenComparingInt(t -> t.endOnStrandWithCoordinateSystem(strand, CoordinateSystem.zeroBased()));
    }



    static <T extends Variant> Routes buildRoutes(Neighborhood neighborhood, List<T> variants) {
        GenomicRegion reference = buildReferencePath(neighborhood.upstream(), neighborhood.downstreamRef(), variants);
        Route alternate = buildAlternatePath(neighborhood.upstream(), neighborhood.downstreamAlt(), variants);
        return Routes.of(reference, alternate);
    }

    private static <T extends Variant> GenomicRegion buildReferencePath(GenomicRegion upstream, GenomicRegion downstream, List<T> variants) {
        validateReferenceInput(upstream, downstream, variants);

        return GenomicRegion.of(upstream.contig(), upstream.strand(), upstream.coordinateSystem(),
                upstream.start(),
                downstream.endOnStrandWithCoordinateSystem(downstream.strand(), downstream.coordinateSystem()));
    }

    private static <T extends Variant> void validateReferenceInput(GenomicRegion upstream, GenomicRegion downstream, List<T> variants) {
        if (variants.isEmpty())
            throw new IllegalArgumentException("Variants must not be empty");

        if (!upstream.contig().equals(downstream.contig()))
            throw new IllegalArgumentException("Upstream and downstream segments must be on the same contig for the reference path");

        if (variants.stream().anyMatch(v -> !v.contig().equals(upstream.contig())))
            throw new IllegalArgumentException("Variant contigs must be the same as the upstream/downstream segment for the reference path");


        T first = variants.get(0);
        if (upstream.endOnStrandWithCoordinateSystem(first.strand(), CoordinateSystem.zeroBased())
                != first.startWithCoordinateSystem(CoordinateSystem.zeroBased()))
            throw new IllegalArgumentException("Non-continuous coordinates for the upstream region and the first variant");

        T last = variants.get(variants.size() - 1);
        if (last.endWithCoordinateSystem(CoordinateSystem.zeroBased()) != downstream.startOnStrandWithCoordinateSystem(last.strand(), CoordinateSystem.zeroBased()))
            throw new IllegalArgumentException("Non-continuous coordinates for the last variant and the downstream region");
    }

    private static <T extends Variant> Route buildAlternatePath(GenomicRegion upstream, GenomicRegion downstream, List<T> variants) {
        if (variants.isEmpty())
            throw new IllegalArgumentException("Variants must not be empty");

        T first = variants.get(0);
        if (upstream.endOnStrandWithCoordinateSystem(first.strand(), CoordinateSystem.zeroBased())
                != first.startWithCoordinateSystem(CoordinateSystem.zeroBased()))
            throw new IllegalArgumentException("Non-continuous coordinates for the upstream region and the first variant");

        LinkedList<RouteLeg> legs = new LinkedList<>();
        legs.add(RouteLeg.of("upstream", upstream.contig(), upstream.strand(), upstream.coordinateSystem(), upstream.startPosition(), upstream.endPosition(), upstream.length()));
        legs.addAll(makeSegmentsForVariant(first));

        for (int i = 1; i < variants.size(); i++) {
            GenomicRegion previous = legs.getLast();
            T variant = variants.get(i);

            Position start = previous.endPositionWithCoordinateSystem(CoordinateSystem.zeroBased());
            Position end = variant.startPositionWithCoordinateSystem(CoordinateSystem.zeroBased());
            RouteLeg gap = RouteLeg.of("gap-" + i, previous.contig(), previous.strand(), CoordinateSystem.zeroBased(),
                    start, end, start.distanceTo(end));
            legs.add(gap);

            List<RouteLeg> variantSegments = makeSegmentsForVariant(variant);
            legs.addAll(variantSegments);
        }

        if (legs.get(legs.size() - 1).endOnStrandWithCoordinateSystem(downstream.strand(), CoordinateSystem.zeroBased())
                != downstream.startWithCoordinateSystem(CoordinateSystem.zeroBased()))
            throw new IllegalArgumentException("Non-continuous coordinates for the last variant and the downstream region");
        legs.add(RouteLeg.of("downstream", downstream.contig(), downstream.strand(), downstream.coordinateSystem(), downstream.startPosition(), downstream.endPosition(), downstream.length()));

        return Route.of(legs);
    }

    private static <T extends Variant> List<RouteLeg> makeSegmentsForVariant(T variant) {
        List<RouteLeg> variantSegments;
        switch (variant.variantType().baseType()) {
            case DEL:
                variantSegments = List.of(RouteLeg.of(variant.id(), variant.contig(), variant.strand(), CoordinateSystem.zeroBased(),
                        variant.startPositionWithCoordinateSystem(CoordinateSystem.zeroBased()),
                        variant.endPositionWithCoordinateSystem(CoordinateSystem.zeroBased()), 0));
                break;
            case DUP:
                RouteLeg dup = RouteLeg.of(variant.id(), variant.contig(), variant.strand(), CoordinateSystem.zeroBased(),
                        variant.startPositionWithCoordinateSystem(CoordinateSystem.zeroBased()),
                        variant.endPositionWithCoordinateSystem(CoordinateSystem.zeroBased()),
                        variant.length());
                variantSegments = List.of(dup, dup); // add twice since it is DUP
                break;
            case INV:
                variantSegments = List.of(RouteLeg.of(variant.id(), variant.contig(), variant.strand().opposite(), CoordinateSystem.zeroBased(),
                        variant.endPositionWithCoordinateSystem(CoordinateSystem.zeroBased()).invert(CoordinateSystem.zeroBased(), variant.contig()),
                        variant.startPositionWithCoordinateSystem(CoordinateSystem.zeroBased()).invert(CoordinateSystem.zeroBased(), variant.contig()),
                        variant.length()
                ));
                break;
            case INS:
                variantSegments = List.of(RouteLeg.of(variant.id(), variant.contig(), variant.strand(), CoordinateSystem.zeroBased(),
                        variant.startPositionWithCoordinateSystem(CoordinateSystem.zeroBased()),
                        variant.endPositionWithCoordinateSystem(CoordinateSystem.zeroBased()), variant.changeLength()));
                break;
            case SNV:
                variantSegments = List.of(); // TODO - this does not add the variant into the path. Evaluate
                break;
            case BND:
            case TRA:
                BreakendVariant bnd = (BreakendVariant) variant;
                Breakend left = bnd.left();
                Breakend right = bnd.right();
                variantSegments = List.of(
                        RouteLeg.of(left.id(), left.contig(), left.strand(), CoordinateSystem.zeroBased(),
                                left.startPositionWithCoordinateSystem(CoordinateSystem.zeroBased()),
                                left.endPositionWithCoordinateSystem(CoordinateSystem.zeroBased()), left.length()),

                        RouteLeg.of(right.id(), right.contig(), right.strand(), CoordinateSystem.zeroBased(),
                                right.startPositionWithCoordinateSystem(CoordinateSystem.zeroBased()),
                                right.endPositionWithCoordinateSystem(CoordinateSystem.zeroBased()),
                                right.length()));
                break;
            default:
                throw new RuntimeException("Unsupported variant type " + variant.variantType());
        }
        return variantSegments;
    }

}
