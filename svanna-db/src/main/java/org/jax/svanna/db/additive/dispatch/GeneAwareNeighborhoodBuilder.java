package org.jax.svanna.db.additive.dispatch;

import org.jax.svanna.core.exception.LogUtils;
import org.jax.svanna.core.priority.additive.DispatchException;
import org.jax.svanna.core.reference.Gene;
import org.jax.svanna.core.reference.GeneService;
import org.jax.svanna.db.landscape.TadBoundaryDao;
import org.monarchinitiative.svart.Breakend;
import org.monarchinitiative.svart.BreakendVariant;
import org.monarchinitiative.svart.GenomicRegion;
import org.monarchinitiative.svart.Variant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class GeneAwareNeighborhoodBuilder extends TadNeighborhoodBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(GeneAwareNeighborhoodBuilder.class);

    private static final int GENE_PADDING = 10;

    private final GeneService geneService;

    public GeneAwareNeighborhoodBuilder(TadBoundaryDao tadBoundaryDao, GeneService geneService) {
        super(tadBoundaryDao);
        this.geneService = geneService;
    }

    @Override
    public <V extends Variant> Neighborhood intrachromosomalNeighborhood(VariantArrangement<V> arrangement) {
        if (arrangement.size() == 1) {
            V variant = arrangement.variants().get(0);
            List<Gene> genes = geneService.overlappingGenes(variant);

            // Let's make this simple if the variant overlaps with a single gene
            // or with a group of genes that overlap each other
            if (allGenesOverlapThemselves(genes)) {
                int startPos = variant.start(), endPos = variant.end();
                for (Gene gene : genes) {
                    int geneStart = gene.startOnStrandWithCoordinateSystem(variant.strand(), CS);
                    startPos = Math.min(geneStart, startPos);

                    int geneEnd = gene.endOnStrandWithCoordinateSystem(variant.strand(), CS);
                    endPos = Math.max(geneEnd, endPos);
                }
                int start = Math.max(0, startPos - GENE_PADDING);
                GenomicRegion upstream = GenomicRegion.of(variant.contig(), variant.strand(), CS, start, startPos);
                int end = Math.min(variant.contig().length(), endPos + GENE_PADDING);
                GenomicRegion downstream = GenomicRegion.of(variant.contig(), variant.strand(), CS, endPos, end);

                return Neighborhood.of(upstream, downstream, downstream);
            }
        }
        return super.intrachromosomalNeighborhood(arrangement);
    }

    private static boolean allGenesOverlapThemselves(List<Gene> genes) {
        if (genes.isEmpty())
            return false;

        for (int i = 0, nGenes = genes.size(); i < nGenes; i++) {
            Gene current = genes.get(i);
            List<Gene> remaining = genes.subList(i + 1, nGenes);
            for (Gene other : remaining) {
                if (!current.overlapsWith(other))
                    return false;
            }
        }
        return true;
    }

    @Override
    public <V extends Variant> Neighborhood interchromosomalNeighborhood(VariantArrangement<V> arrangement) {
        if (arrangement.size() == 1) {
            V v = arrangement.variants().get(0);
            BreakendVariant bv;
            try {
                bv = (BreakendVariant) v;
            } catch (ClassCastException e) {
                LogUtils.logWarn(LOGGER, "Expected to get BreakendVariant, got `{}`. {}", v.getClass().getSimpleName(), e.getMessage());
                throw new DispatchException(e);
            }

            GenomicRegion upstream, downstreamRef, downstreamAlt;

            Breakend left = bv.left();
            List<Gene> genes = geneService.overlappingGenes(left);
            if (!genes.isEmpty()) {
                int startPos = -1, endPos = -1;
                for (Gene gene : genes) {
                    int geneStart = gene.startOnStrandWithCoordinateSystem(left.strand(), CS);
                    if (startPos == -1)
                        startPos = geneStart;
                    else
                        startPos = Math.min(startPos, geneStart);

                    int geneEnd = gene.endOnStrandWithCoordinateSystem(left.strand(), CS);
                    if (endPos == -1)
                        endPos = geneEnd;
                    else
                        endPos = Math.max(endPos, geneEnd);
                }

                int start = Math.max(0, startPos - GENE_PADDING);
                upstream = GenomicRegion.of(left.contig(), left.strand(), CS, start, startPos);
                int end = Math.min(left.contig().length(), endPos + GENE_PADDING);
                downstreamRef = GenomicRegion.of(left.contig(), left.strand(), CS, endPos, end);
            } else {
                upstream = left.withCoordinateSystem(CS).withPadding(1, 0);
                downstreamRef = left.withCoordinateSystem(CS);
            }
            downstreamAlt = bv.right();

            return Neighborhood.of(upstream, downstreamRef, downstreamAlt);
        }
        return super.interchromosomalNeighborhood(arrangement);
    }
}
