package org.jax.svanna.core.reference;

import org.junit.jupiter.api.Test;
import org.monarchinitiative.svart.*;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.sameInstance;

public class TranscriptTest {

    private static final Contig CONTIG = Contig.of(1, "1", SequenceRole.ASSEMBLED_MOLECULE, "1", AssignedMoleculeType.CHROMOSOME, 500, "", "", "");

    @Test
    public void properties() {
        List<GenomicRegion> txExons = List.of(
                GenomicRegion.of(CONTIG, Strand.POSITIVE, CoordinateSystem.zeroBased(), Position.of(100), Position.of(130)),
                GenomicRegion.of(CONTIG, Strand.POSITIVE, CoordinateSystem.zeroBased(), Position.of(150), Position.of(170)),
                GenomicRegion.of(CONTIG, Strand.POSITIVE, CoordinateSystem.zeroBased(), Position.of(180), Position.of(200)));
        TranscriptDefault tx = TranscriptDefault.coding(CONTIG, Strand.POSITIVE, CoordinateSystem.zeroBased(), 100, 200, 110, 190,
                "NM_123456.3", "GENE", txExons);

        assertThat(tx.start(), equalTo(100));
        assertThat(tx.end(), equalTo(200));
        assertThat(tx.length(), equalTo(100));

        assertThat(tx.isCoding(), equalTo(true));
        GenomicRegion cds = tx.cdsRegion().get();
        assertThat(cds.start(), equalTo(110));
        assertThat(cds.end(), equalTo(190));

        assertThat(tx.accessionId(), equalTo("NM_123456.3"));
        assertThat(tx.hgvsSymbol(), equalTo("GENE"));

        List<GenomicRegion> exons = tx.exons();
        assertThat(exons.get(0), equalTo(GenomicRegion.of(CONTIG, Strand.POSITIVE, CoordinateSystem.zeroBased(), Position.of(100), Position.of(130))));
        assertThat(exons.get(1), equalTo(GenomicRegion.of(CONTIG, Strand.POSITIVE, CoordinateSystem.zeroBased(), Position.of(150), Position.of(170))));
        assertThat(exons.get(2), equalTo(GenomicRegion.of(CONTIG, Strand.POSITIVE, CoordinateSystem.zeroBased(), Position.of(180), Position.of(200))));
    }

    @Test
    public void withStrand() {
        List<GenomicRegion> txExons = List.of(
                GenomicRegion.of(CONTIG, Strand.POSITIVE, CoordinateSystem.zeroBased(), Position.of(100), Position.of(130)),
                GenomicRegion.of(CONTIG, Strand.POSITIVE, CoordinateSystem.zeroBased(), Position.of(150), Position.of(170)),
                GenomicRegion.of(CONTIG, Strand.POSITIVE, CoordinateSystem.zeroBased(), Position.of(180), Position.of(200)));
        TranscriptDefault instance = TranscriptDefault.coding(CONTIG, Strand.POSITIVE, CoordinateSystem.zeroBased(), 100, 200, 110, 190,
                "NM_123456.3", "GENE", txExons);
        assertThat(instance.withStrand(Strand.POSITIVE), sameInstance(instance));

        Transcript tx = instance.withStrand(Strand.NEGATIVE);

        assertThat(tx.start(), equalTo(300));
        assertThat(tx.end(), equalTo(400));
        assertThat(tx.length(), equalTo(100));

        assertThat(tx.accessionId(), equalTo("NM_123456.3"));
        assertThat(tx.hgvsSymbol(), equalTo("GENE"));
        assertThat(tx.isCoding(), equalTo(true));
        GenomicRegion cds = tx.cdsRegion().get();
        assertThat(cds.start(), equalTo(310));
        assertThat(cds.end(), equalTo(390));

        List<GenomicRegion> exons = tx.exons();
//        assertThat(exons.get(0), equalTo(GenomicRegion.of(CONTIG, Strand.NEGATIVE, CoordinateSystem.zeroBased(), Position.of(300), Position.of(320))));
//        assertThat(exons.get(1), equalTo(GenomicRegion.of(CONTIG, Strand.NEGATIVE, CoordinateSystem.zeroBased(), Position.of(330), Position.of(350))));
//        assertThat(exons.get(2), equalTo(GenomicRegion.of(CONTIG, Strand.NEGATIVE, CoordinateSystem.zeroBased(), Position.of(370), Position.of(400))));
        assertThat(exons.get(0), equalTo(GenomicRegion.of(CONTIG, Strand.NEGATIVE, CoordinateSystem.zeroBased(), Position.of(370), Position.of(400))));
        assertThat(exons.get(1), equalTo(GenomicRegion.of(CONTIG, Strand.NEGATIVE, CoordinateSystem.zeroBased(), Position.of(330), Position.of(350))));
        assertThat(exons.get(2), equalTo(GenomicRegion.of(CONTIG, Strand.NEGATIVE, CoordinateSystem.zeroBased(), Position.of(300), Position.of(320))));
    }

    @Test
    public void withStrand_noncodingTx() {
        Transcript instance = TranscriptDefault.nonCoding(CONTIG, Strand.POSITIVE, CoordinateSystem.zeroBased(), 100, 200,
                "NM_123456.3", "GENE",
                List.of(
                        GenomicRegion.of(CONTIG, Strand.POSITIVE, CoordinateSystem.zeroBased(), Position.of(100), Position.of(130)),
                        GenomicRegion.of(CONTIG, Strand.POSITIVE, CoordinateSystem.zeroBased(), Position.of(150), Position.of(170)),
                        GenomicRegion.of(CONTIG, Strand.POSITIVE, CoordinateSystem.zeroBased(), Position.of(180), Position.of(200))));

        assertThat(instance.withStrand(Strand.POSITIVE), sameInstance(instance));

        Transcript tx = instance.withStrand(Strand.NEGATIVE);

        assertThat(tx.start(), equalTo(300));
        assertThat(tx.end(), equalTo(400));
        assertThat(tx.length(), equalTo(100));

        assertThat(tx.accessionId(), equalTo("NM_123456.3"));
        assertThat(tx.hgvsSymbol(), equalTo("GENE"));
        assertThat(tx.isCoding(), equalTo(false));

        List<GenomicRegion> exons = tx.exons();
//        assertThat(exons.get(0), equalTo(GenomicRegion.of(CONTIG, Strand.NEGATIVE, CoordinateSystem.zeroBased(), Position.of(300), Position.of(320))));
//        assertThat(exons.get(1), equalTo(GenomicRegion.of(CONTIG, Strand.NEGATIVE, CoordinateSystem.zeroBased(), Position.of(330), Position.of(350))));
//        assertThat(exons.get(2), equalTo(GenomicRegion.of(CONTIG, Strand.NEGATIVE, CoordinateSystem.zeroBased(), Position.of(370), Position.of(400))));
        assertThat(exons.get(0), equalTo(GenomicRegion.of(CONTIG, Strand.NEGATIVE, CoordinateSystem.zeroBased(), Position.of(370), Position.of(400))));
        assertThat(exons.get(1), equalTo(GenomicRegion.of(CONTIG, Strand.NEGATIVE, CoordinateSystem.zeroBased(), Position.of(330), Position.of(350))));
        assertThat(exons.get(2), equalTo(GenomicRegion.of(CONTIG, Strand.NEGATIVE, CoordinateSystem.zeroBased(), Position.of(300), Position.of(320))));
    }

    @Test
    public void withCoordinateSystem() {
        List<GenomicRegion> txExons = List.of(
                GenomicRegion.of(CONTIG, Strand.POSITIVE, CoordinateSystem.zeroBased(), Position.of(100), Position.of(130)),
                GenomicRegion.of(CONTIG, Strand.POSITIVE, CoordinateSystem.zeroBased(), Position.of(150), Position.of(170)),
                GenomicRegion.of(CONTIG, Strand.POSITIVE, CoordinateSystem.zeroBased(), Position.of(180), Position.of(200)));
        Transcript instance = TranscriptDefault.coding(CONTIG, Strand.POSITIVE, CoordinateSystem.zeroBased(), 100, 200, 110, 190,
                "NM_123456.3", "GENE", txExons);
        assertThat(instance.withCoordinateSystem(CoordinateSystem.zeroBased()), sameInstance(instance));

        Transcript tx = instance.withCoordinateSystem(CoordinateSystem.oneBased());

        assertThat(tx.start(), equalTo(101));
        assertThat(tx.end(), equalTo(200));
        assertThat(tx.isCoding(), equalTo(true));
        GenomicRegion cds = tx.cdsRegion().get();
        assertThat(cds.start(), equalTo(111));
        assertThat(cds.end(), equalTo(190));

        assertThat(tx.accessionId(), equalTo("NM_123456.3"));
        assertThat(tx.hgvsSymbol(), equalTo("GENE"));
        assertThat(tx.isCoding(), equalTo(true));
        assertThat(tx.length(), equalTo(100));

        List<GenomicRegion> exons = tx.exons();
        assertThat(exons.get(0), equalTo(GenomicRegion.of(CONTIG, Strand.POSITIVE, CoordinateSystem.oneBased(), Position.of(101), Position.of(130))));
        assertThat(exons.get(1), equalTo(GenomicRegion.of(CONTIG, Strand.POSITIVE, CoordinateSystem.oneBased(), Position.of(151), Position.of(170))));
        assertThat(exons.get(2), equalTo(GenomicRegion.of(CONTIG, Strand.POSITIVE, CoordinateSystem.oneBased(), Position.of(181), Position.of(200))));
    }

    @Test
    public void withCoordinateSystem_noncodingTx() {
        Transcript instance = TranscriptDefault.nonCoding(CONTIG, Strand.POSITIVE, CoordinateSystem.zeroBased(), 100, 200,
                "NM_123456.3", "GENE", List.of(
                        GenomicRegion.of(CONTIG, Strand.POSITIVE, CoordinateSystem.zeroBased(), Position.of(100), Position.of(130)),
                        GenomicRegion.of(CONTIG, Strand.POSITIVE, CoordinateSystem.zeroBased(), Position.of(150), Position.of(170)),
                        GenomicRegion.of(CONTIG, Strand.POSITIVE, CoordinateSystem.zeroBased(), Position.of(180), Position.of(200))));

        assertThat(instance.withCoordinateSystem(CoordinateSystem.zeroBased()), sameInstance(instance));

        Transcript tx = instance.withCoordinateSystem(CoordinateSystem.oneBased());

        assertThat(tx.start(), equalTo(101));
        assertThat(tx.end(), equalTo(200));
        assertThat(tx.length(), equalTo(100));

        assertThat(tx.accessionId(), equalTo("NM_123456.3"));
        assertThat(tx.hgvsSymbol(), equalTo("GENE"));
        assertThat(tx.isCoding(), equalTo(false));

        List<GenomicRegion> exons = tx.exons();
        assertThat(exons.get(0), equalTo(GenomicRegion.of(CONTIG, Strand.POSITIVE, CoordinateSystem.oneBased(), Position.of(101), Position.of(130))));
        assertThat(exons.get(1), equalTo(GenomicRegion.of(CONTIG, Strand.POSITIVE, CoordinateSystem.oneBased(), Position.of(151), Position.of(170))));
        assertThat(exons.get(2), equalTo(GenomicRegion.of(CONTIG, Strand.POSITIVE, CoordinateSystem.oneBased(), Position.of(181), Position.of(200))));
    }
}