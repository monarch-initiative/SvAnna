package org.jax.svanna.db.additive.dispatch;

import org.jax.svanna.core.landscape.TadBoundary;
import org.jax.svanna.core.priority.additive.DispatchException;
import org.jax.svanna.db.landscape.TadBoundaryDao;
import org.monarchinitiative.svart.*;

import java.util.LinkedList;
import java.util.Optional;

/**
 * Define variant neighborhood using the closest TAD boundaries found upstream/downstream of the variant position.
 * <p>
 * If there is no TAD upstream/downstream from the position, use contig end instead.
 */
public class TadNeighborhoodBuilder implements NeighborhoodBuilder {

    protected static final CoordinateSystem CS = CoordinateSystem.zeroBased();
    private final TadBoundaryDao tadBoundaryDao;

    public TadNeighborhoodBuilder(TadBoundaryDao tadBoundaryDao) {
        this.tadBoundaryDao = tadBoundaryDao;
    }

    @Override
    public <V extends Variant> Neighborhood intrachromosomalNeighborhood(VariantArrangement<V> arrangement) {
        LinkedList<V> variants = arrangement.variants();
        V first = variants.getFirst();
        V last = variants.getLast();
        if (first.strand() != last.strand())
            throw new DispatchException("First and last variants must be on the same strand");

        Optional<TadBoundary> upstreamTad = tadBoundaryDao.upstreamOf(first);
        // empty when there is no TAD upstream, just the end of the chromosome
        GenomicRegion upstream = upstreamTad.isPresent()
                ? upstreamTad.get()
                : GenomicRegion.of(first.contig(), first.strand(), CS, Position.of(0), Position.of(0)); // !!

        Optional<TadBoundary> downstreamTad = tadBoundaryDao.downstreamOf(last);
        // empty when there is no TAD downstream, just the end of the chromosome
        GenomicRegion downstream = downstreamTad.isPresent()
                ? downstreamTad.get()
                : GenomicRegion.of(last.contig(), last.strand(), CS, last.contig().length(), last.contig().length());

        return Neighborhood.of(upstream, downstream, downstream);
    }

    @Override
    public <V extends Variant> Neighborhood interchromosomalNeighborhood(VariantArrangement<V> arrangement) {
        LinkedList<V> variants = arrangement.variants();

        BreakendVariant bv = (BreakendVariant) variants.get(arrangement.breakendIndex());
        Breakend left = bv.left();
        Breakend right = bv.right();

        V first = variants.getFirst();
        GenomicRegion leftmost = (first instanceof BreakendVariant)
                ? ((BreakendVariant) first).left()
                : first;

        Position upstreamBound = tadBoundaryDao.upstreamOf(leftmost)
                .map(TadBoundary::asPosition)
                .orElse(Position.of(0));
        GenomicRegion upstream = GenomicRegion.of(left.contig(), left.strand(), CS, upstreamBound, upstreamBound);

        Position downstreamRefBound = tadBoundaryDao.downstreamOf(left)
                .map(TadBoundary::asPosition)
                .orElse(Position.of(left.contig().length()));
        GenomicRegion downstreamRef = GenomicRegion.of(left.contig(), left.strand(), CS, downstreamRefBound, downstreamRefBound);

        V last = variants.getLast();
        GenomicRegion rightmost = (last instanceof BreakendVariant)
                ? ((BreakendVariant) last).right()
                : last;
        Position downstreamAltBound = tadBoundaryDao.downstreamOf(rightmost)
                .map(TadBoundary::asPosition)
                .orElse(Position.of(right.contig().length()));
        GenomicRegion downstreamAlt = GenomicRegion.of(right.contig(), right.strand(), CS, downstreamAltBound, downstreamAltBound);

        return Neighborhood.of(upstream, downstreamRef, downstreamAlt);
    }
}
