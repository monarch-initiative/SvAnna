package org.jax.svanna.db.additive.dispatch;

import org.jax.svanna.core.LogUtils;
import org.jax.svanna.core.priority.additive.*;
import org.jax.svanna.core.service.GeneService;
import org.jax.svanna.db.landscape.TadBoundaryDao;
import org.jax.svanna.model.landscape.tad.TadBoundary;
import org.monarchinitiative.svart.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.ielis.silent.genes.model.Gene;
import xyz.ielis.silent.genes.model.Located;

import java.util.*;

public class TadAwareDispatcher implements Dispatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(TadAwareDispatcher.class);
    private static final CoordinateSystem CS = CoordinateSystem.zeroBased();

    private final GeneService geneService;
    private final TadBoundaryDao tadBoundaryDao;
    private final DispatchOptions dispatchOptions;

    public TadAwareDispatcher(GeneService geneService, TadBoundaryDao tadBoundaryDao, DispatchOptions dispatchOptions) {
        this.geneService = geneService;
        this.tadBoundaryDao = tadBoundaryDao;
        this.dispatchOptions = dispatchOptions;
    }

    @Override
    public Routes assembleRoutes(List<Variant> variants) throws DispatchException {
        // variants are sorted and put to the same strand - either POSITIVE or NEGATIVE
        VariantArrangement arrangement = RouteAssemblyUtils.assemble(variants);


        if (arrangement.hasBreakend()) {
            if (arrangement.size() != 1)
                throw new DispatchException("Dispatching from more than one breakend variant is not currently supported, got " + arrangement.size());
            /*
             Assume reciprocal translocation:

             - get BreakendVariant
             - get left breakend
               - left TAD pair
             - get right breakend
               - right TAD pair
             - define reference routes using left and right TAD pairs
             - define alternate routes using left upstream and right downstream
             */
            Variant v = arrangement.variants().get(arrangement.breakendIndex());
            try {
                return interchromosomalArrangement((BreakendVariant) v);
            } catch (ClassCastException e) {
                LogUtils.logWarn(LOGGER, "Expected to get BreakendVariant, got `{}`. {}", v.getClass().getSimpleName(), e.getMessage());
                throw new DispatchException(e);
            }
        } else {
            return intrachromosomalArrangement(arrangement);
        }
    }

    private Routes interchromosomalArrangement(BreakendVariant bv) throws DispatchException {
        // reference regions
        Breakend left = bv.left();
        List<Gene> leftGenes = geneService.overlappingGenes(left).overlapping();
        Breakend right = bv.right();
        List<Gene> rightGenes = geneService.overlappingGenes(right).overlapping();
        Pair<GenomicRegion> pair = (!leftGenes.isEmpty() || !rightGenes.isEmpty())
                ? calculateGeneBoundsForBreakends(left, leftGenes, right, rightGenes) // dispatch for the genes only
                : calculateBoundsBasedOnBreakendsOnly(left, right);     // dispatch using variant data only for intergenic events
//                : calculateTadBounds(left, right);     // dispatch using TADs
        GenomicRegion leftReference = pair.left();
        GenomicRegion rightReference = pair.right();

        List<Route> alternates = RouteAssemblyUtils.makeAltRouteForBreakendVariant(bv, leftReference, rightReference);

        return Routes.of(List.of(leftReference, rightReference), alternates);
    }

    private Routes intrachromosomalArrangement(VariantArrangement arrangement) {
        LinkedList<? extends Variant> variants = arrangement.variants();
        Variant first = variants.getFirst();

        int upstreamBound = -1, downstreamBound = -1;
        if (!dispatchOptions.forceEvaluateTad() && variants.size() == 1) {
            List<Gene> genes = geneService.overlappingGenes(first).overlapping();

            // Let's make this simple if the variant overlaps with a single gene
            // or with a group of genes that overlap each other
            if (allGenesOverlapThemselves(genes)) {
                Pair<Integer> coordinates = findStartAndEnd(genes, first.strand(), CS);
                upstreamBound = Math.min(coordinates.left(), first.startWithCoordinateSystem(CS));
                downstreamBound = Math.max(coordinates.right(), first.endWithCoordinateSystem(CS));
            }
        }
        if (upstreamBound < 0 || downstreamBound < 0) {
            // use TADs
            Variant last = variants.getLast();
            if (first.strand() != last.strand())
                throw new DispatchException("First and last variants must be on the same strand");

            upstreamBound = upstreamBound(first) + first.coordinateSystem().startDelta(CS);
            downstreamBound = downstreamBound(last) + last.coordinateSystem().endDelta(CS);
        }

        GenomicRegion reference = GenomicRegion.of(first.contig(), first.strand(), CS, upstreamBound, downstreamBound);
        Route altRoute = RouteUtils.buildRoute(upstreamBound, downstreamBound, variants);

        return Routes.of(List.of(reference), List.of(altRoute));
    }

    private static Pair<GenomicRegion> calculateGeneBoundsForBreakends(Breakend left, List<Gene> leftGenes, Breakend right, List<Gene> rightGenes) {
        GenomicRegion leftRegion;
        if (leftGenes.isEmpty())
            leftRegion = left;
        else {
            Pair<Integer> pair = findStartAndEnd(leftGenes, left.strand(), CS);
            leftRegion = GenomicRegion.of(left.contig(), left.strand(), CS, pair.left(), pair.right());
        }
        GenomicRegion rightRegion;
        if (rightGenes.isEmpty())
            rightRegion = right;
        else {
            Pair<Integer> pair = findStartAndEnd(rightGenes, right.strand(), CS);
            rightRegion = GenomicRegion.of(right.contig(), right.strand(), CS, pair.left(), pair.right());
        }

        return Pair.of(leftRegion, rightRegion);
    }

    private Pair<GenomicRegion> calculateTadBounds(Breakend left, Breakend right) {
        int leftUpstream = upstreamBound(left);
        int leftDownstream = downstreamBound(left);
        GenomicRegion leftReference = GenomicRegion.of(left.contig(), left.strand(), CS, leftUpstream, leftDownstream);

        int rightUpstream = upstreamBound(right);
        int rightDownstream = downstreamBound(right);
        GenomicRegion rightReference = GenomicRegion.of(right.contig(), right.strand(), CS, rightUpstream, rightDownstream);

        return Pair.of(leftReference, rightReference);
    }

    private static Pair<GenomicRegion> calculateBoundsBasedOnBreakendsOnly(Breakend left, Breakend right) {
        return Pair.of(left, right);
    }

    private int upstreamBound(GenomicRegion query) {
        Optional<TadBoundary> upstreamTad = tadBoundaryDao.upstreamOf(query);
        if (upstreamTad.isPresent()) {
            TadBoundary tadBoundary = upstreamTad.get();
            return tadBoundary.midpoint().start() + tadBoundary.coordinateSystem().startDelta(query.coordinateSystem());
        } else
            // empty position of the contig start in query's coordinate system
            return CoordinateSystem.zeroBased().startDelta(query.coordinateSystem());
    }

    private int downstreamBound(GenomicRegion query) {
        Optional<TadBoundary> downstreamTad = tadBoundaryDao.downstreamOf(query);
        if (downstreamTad.isPresent()) {
            TadBoundary tadBoundary = downstreamTad.get();
            // Subtract one to not include the TAD as a relevant genomic element for evaluation. Otherwise, using the
            // upstream & downstream bounds to query overlapping TADs will fetch the TAD used to delimit the downstream
            // bound. This is because 0-based coordinate system includes the end position.
            return tadBoundary.midpoint().end() + tadBoundary.coordinateSystem().endDelta(query.coordinateSystem()) - 1;
        } else
            // empty coordinate of the contig end in query's coordinate system
            return query.contig().length() + CoordinateSystem.zeroBased().endDelta(query.coordinateSystem());
    }


    private static boolean allGenesOverlapThemselves(List<Gene> genes) {
        if (genes.isEmpty())
            return false;

        for (int i = 0, nGenes = genes.size(); i < nGenes; i++) {
            Gene current = genes.get(i);
            List<Gene> remaining = genes.subList(i + 1, nGenes);
            for (Gene other : remaining) {
                if (!current.location().overlapsWith(other.location()))
                    return false;
            }
        }
        return true;
    }

    private static Pair<Integer> findStartAndEnd(Collection<? extends Located> regions, Strand strand, CoordinateSystem coordinateSystem) {
        int startPos = -1, endPos = -1;
        for (Located region : regions) {
            int regionStart, regionEnd;
            if (region.strand().equals(strand)) {
                regionStart = region.startWithCoordinateSystem(coordinateSystem);
                regionEnd = region.endOnStrandWithCoordinateSystem(strand, coordinateSystem);
            } else {
                regionStart = Coordinates.invertPosition(coordinateSystem, region.contig(), region.end());
                regionEnd = Coordinates.invertPosition(coordinateSystem, region.contig(), region.start());
            }
            if (startPos == -1)
                startPos = regionStart;
            startPos = Math.min(startPos, regionStart);

            if (endPos == -1)
                endPos = regionEnd;
            endPos = Math.max(endPos, regionEnd);
        }
        return Pair.of(startPos, endPos);
    }
}
