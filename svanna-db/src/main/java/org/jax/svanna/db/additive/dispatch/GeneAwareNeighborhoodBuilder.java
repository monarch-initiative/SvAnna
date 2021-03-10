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
            if (genes.size() == 1) {
                Gene gene = genes.get(0);
                if (gene.contains(variant)) {
                    int startPos = gene.startOnStrandWithCoordinateSystem(variant.strand(), CS) - GENE_PADDING;
                    GenomicRegion upstream = GenomicRegion.of(variant.contig(), variant.strand(), CS, startPos, startPos);

                    int endPos = gene.endOnStrandWithCoordinateSystem(variant.strand(), CS) + GENE_PADDING;
                    GenomicRegion downstream = GenomicRegion.of(variant.contig(), variant.strand(), CS, endPos, endPos);

                    return Neighborhood.of(upstream, downstream, downstream);
                }
            }
        }
        return super.intrachromosomalNeighborhood(arrangement);
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

                upstream = GenomicRegion.of(left.contig(), left.strand(), CS, startPos - GENE_PADDING, startPos);
                downstreamRef = GenomicRegion.of(left.contig(), left.strand(), CS, endPos, endPos + GENE_PADDING);
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
