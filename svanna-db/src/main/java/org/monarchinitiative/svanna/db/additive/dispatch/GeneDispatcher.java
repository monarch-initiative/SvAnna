package org.monarchinitiative.svanna.db.additive.dispatch;

import org.monarchinitiative.svanna.core.priority.additive.*;
import org.monarchinitiative.svanna.core.service.GeneService;
import org.monarchinitiative.svanna.core.service.QueryResult;
import org.monarchinitiative.svart.*;
import org.monarchinitiative.sgenes.model.Gene;

import java.util.List;

/**
 * Assemble routes for variant evaluation using only the overlapping genes.
 */
public class GeneDispatcher implements Dispatcher {

    /* for doing genome arithmetics */
    private static final CoordinateSystem CS = CoordinateSystem.zeroBased();
    private static final Strand STRAND = Strand.POSITIVE;

    private final GeneService geneService;

    public GeneDispatcher(GeneService geneService) {
        this.geneService = geneService;
    }

    @Override
    public Routes assembleRoutes(List<GenomicVariant> variants) throws DispatchException {
        VariantArrangement arrangement = RouteAssemblyUtils.assemble(variants);

        if (!arrangement.hasBreakend()) {
            return intrachromosomalArrangement(arrangement);
        } else {
            if (arrangement.size() != 1)
                throw new DispatchException("Dispatching from more than one breakend variant is not currently supported, got " + arrangement.size() + " variants");
            try {
                GenomicVariant v = arrangement.variants().get(arrangement.breakendIndex());
                GenomicBreakendVariant bv = (GenomicBreakendVariant) v;
                return interchromosomalArrangement(bv, arrangement);
            } catch (ClassCastException e) {
                throw new DispatchException("Expected BreakendVariant but found " + variants.getClass() + " instead at position " + arrangement.breakendIndex() + " of the variant arrangement");
            }
        }
    }

    private Routes intrachromosomalArrangement(VariantArrangement arrangement) {
        GenomicRegion variantRegion;
        if (arrangement instanceof IntrachromosomalVariantArrangement) {
            // This should always be the case, but let's be 100% sure.
            variantRegion = ((IntrachromosomalVariantArrangement) arrangement).variantRegion();
        } else {
            variantRegion = IntrachromosomalVariantArrangement.makeIntrachromosomalVariantRegion(arrangement.variants());
        }

        GenomicRegion reference = getGeneRegion(variantRegion);

        Route altRoute = RouteUtils.buildRoute(reference.start(), reference.end(), arrangement.variants());

        return Routes.of(List.of(reference), List.of(altRoute));
    }

    private Routes interchromosomalArrangement(GenomicBreakendVariant bv, VariantArrangement arrangement) throws DispatchException {
        GenomicRegion leftVariantRegion, rightVariantRegion;
        if (arrangement instanceof InterchromosomalVariantArrangement) {
            // This should always be the case, but let's be 100% sure.
            leftVariantRegion = ((InterchromosomalVariantArrangement) arrangement).leftVariantRegion();
            rightVariantRegion = ((InterchromosomalVariantArrangement) arrangement).rightVariantRegion();
        } else {
            leftVariantRegion = InterchromosomalVariantArrangement.prepareLeftRegion(arrangement.variants(), bv.left());
            rightVariantRegion = InterchromosomalVariantArrangement.prepareRightRegion(arrangement.variants(), bv.right());
        }

        /*
         In case of translocations, it is imperative to assemble the ALT segments on strands of the breakends, and NOT
         on arbitrary, e.g. POSITIVE strand.
         */

        /* ---------------------------------------------- ALT ------------------------------------------------------- */
        GenomicRegion leftGeneRegion = getGeneRegion(leftVariantRegion);
        GenomicRegion rightGeneRegion = getGeneRegion(rightVariantRegion);

        List<Route> alternates = RouteAssemblyUtils.makeAltRouteForBreakendVariant(bv, leftGeneRegion, rightGeneRegion);

        return Routes.of(List.of(leftGeneRegion, rightGeneRegion), alternates);
    }


    /**
     * Get region of gene(s) that overlap with variant region.
     * <p>
     * There are three possible outcomes:
     * <ul>
     *     <li>variant region overlaps with a gene - the region of the gene is returned</li>
     *     <li>variant region is intergenic - the region including upstream and downstream genes is returned</li>
     *     <li>variant region is on a contig with <em>no</em> genes - the variant region is returned</li>
     * </ul>
     *
     * @param variantRegion region representing the variant
     * @return region of relevant genes based on the variant overlap
     */
    private GenomicRegion getGeneRegion(GenomicRegion variantRegion) {
        QueryResult<Gene> result = geneService.overlappingGenes(variantRegion);

        // initialize with fallback values from the region spanned by variants
        int upstreamBound = variantRegion.startOnStrandWithCoordinateSystem(STRAND, CS);
        int downstreamBound = variantRegion.endOnStrandWithCoordinateSystem(STRAND, CS);

        if (result.hasOverlapping()) {
            // use gene boundaries to calculate upstream/downstream bounds
            for (Gene gene : result.overlapping()) {
                upstreamBound = Math.min(upstreamBound, gene.startOnStrandWithCoordinateSystem(STRAND, CS));
                downstreamBound = Math.max(downstreamBound, gene.endOnStrandWithCoordinateSystem(STRAND, CS));
            }
        } else {
            // include upstream/downstream genes to determine the bounds
            if (result.upstream().isPresent())
                upstreamBound = Math.min(upstreamBound, result.upstream().get().startOnStrandWithCoordinateSystem(STRAND, CS));

            if (result.downstream().isPresent())
                downstreamBound = Math.max(downstreamBound, result.downstream().get().endOnStrandWithCoordinateSystem(STRAND, CS));
        }

        return GenomicRegion.of(variantRegion.contig(), STRAND, CS, upstreamBound, downstreamBound);
    }
}
