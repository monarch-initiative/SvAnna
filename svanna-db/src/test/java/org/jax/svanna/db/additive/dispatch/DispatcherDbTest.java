package org.jax.svanna.db.additive.dispatch;

import org.jax.svanna.core.priority.additive.Routes;
import org.jax.svanna.core.service.GeneService;
import org.jax.svanna.db.TestContig;
import org.jax.svanna.db.landscape.TadBoundaryDao;
import org.jax.svanna.model.landscape.tad.TadBoundary;
import org.jax.svanna.model.landscape.tad.TadBoundaryDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.svart.*;
import xyz.ielis.silent.genes.model.Gene;
import xyz.ielis.silent.genes.model.GeneIdentifier;
import xyz.ielis.silent.genes.model.Transcript;
import xyz.ielis.silent.genes.model.TranscriptIdentifier;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Disabled // TODO - implement
public class DispatcherDbTest {

    private static final Contig ctg1 = TestContig.of(1, 1000);
    private static final Contig ctg2 = TestContig.of(2, 2000);

    private static final List<Gene> GENES = List.of(
            makeGene("NCBIGene:A", "ONE", ctg1, 200, 400),
            makeGene("NCBIGene:B", "TWO", ctg2, 600, 1000)
    );

    private static final List<TadBoundary> TADS = List.of(
            TadBoundaryDefault.of(GenomicRegion.of(ctg1, Strand.POSITIVE, CoordinateSystem.zeroBased(), 10, 10), "one", .95f),
            TadBoundaryDefault.of(GenomicRegion.of(ctg1, Strand.POSITIVE, CoordinateSystem.zeroBased(), 50, 50), "two", .96f),
            TadBoundaryDefault.of(GenomicRegion.of(ctg2, Strand.POSITIVE, CoordinateSystem.zeroBased(), 20, 20), "three", .97f),
            TadBoundaryDefault.of(GenomicRegion.of(ctg2, Strand.POSITIVE, CoordinateSystem.zeroBased(), 70, 70), "two", .98f)
    );
    private GeneService geneService;
    private TadBoundaryDao tadBoundaryDao;

    private static Gene makeGene(String id, String symbol, Contig contig, int start, int end) {
        GenomicRegion location = GenomicRegion.of(contig, Strand.POSITIVE, CoordinateSystem.zeroBased(), start, end);

        TranscriptIdentifier txId = TranscriptIdentifier.of(id + "_tx", symbol + "_tx", null);
        List<Coordinates> exons = List.of(Coordinates.of(CoordinateSystem.zeroBased(), start, end));
        Coordinates startCodon = Coordinates.of(CoordinateSystem.zeroBased(), start, start + 3);
        Coordinates stopCodon = Coordinates.of(CoordinateSystem.zeroBased(), end - 3, end);
        Transcript tx = Transcript.coding(txId, location, exons, startCodon, stopCodon);

        GeneIdentifier geneId = GeneIdentifier.of(id, symbol, null, null);
        return Gene.of(geneId, location, Set.of(tx));
    }

    @BeforeEach
    public void setUp() {
        geneService = mock(GeneService.class);
        tadBoundaryDao = mock(TadBoundaryDao.class);
    }

    @Test
    public void assembleRoutes() {
        Breakend left = Breakend.of(ctg1, "left", Strand.POSITIVE, Coordinates.of(CoordinateSystem.zeroBased(), 300, 300));
        Breakend right = Breakend.of(ctg2, "right", Strand.POSITIVE, Coordinates.of(CoordinateSystem.zeroBased(), 500, 500));
        BreakendVariant bv = BreakendVariant.of("BLA", left, right, "N", "ACGT");

        when(geneService.overlappingGenes(left))
                .thenReturn(List.of(GENES.get(0)));
        when(geneService.overlappingGenes(right))
                .thenReturn(List.of());

        when(tadBoundaryDao.upstreamOf(left))
                .thenReturn(Optional.of(TADS.get(0)));
        when(tadBoundaryDao.downstreamOf(left))
                .thenReturn(Optional.of(TADS.get(1)));
        when(tadBoundaryDao.upstreamOf(right))
                .thenReturn(Optional.of(TADS.get(2)));
        when(tadBoundaryDao.downstreamOf(right))
                .thenReturn(Optional.of(TADS.get(3)));

        DispatchOptions dispatchOptions = DispatchOptions.of(false);
        DispatcherDb polyDispatcherDb = new DispatcherDb(geneService, tadBoundaryDao, dispatchOptions);

        Routes routes = polyDispatcherDb.assembleRoutes(List.of(bv));

        routes.references().forEach(System.err::println);
        routes.alternates().forEach(System.err::println);
    }

}