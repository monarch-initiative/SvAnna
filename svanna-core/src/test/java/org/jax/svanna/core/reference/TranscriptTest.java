package org.jax.svanna.core.reference;

import org.junit.jupiter.api.Test;
import org.monarchinitiative.variant.api.*;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class TranscriptTest {

    private static final Contig CONTIG = Contig.of(1, "1", SequenceRole.ASSEMBLED_MOLECULE, 500, "", "", "");

    private static Transcript instance() {
        List<GenomicRegion> exons = List.of(
                GenomicRegion.of(CONTIG, Strand.POSITIVE, CoordinateSystem.ZERO_BASED, Position.of(100), Position.of(130)),
                GenomicRegion.of(CONTIG, Strand.POSITIVE, CoordinateSystem.ZERO_BASED, Position.of(150), Position.of(170)),
                GenomicRegion.of(CONTIG, Strand.POSITIVE, CoordinateSystem.ZERO_BASED, Position.of(180), Position.of(200)));
        return Transcript.of(CONTIG, 100, 200, Strand.POSITIVE, CoordinateSystem.ZERO_BASED,
                110, 190, "NM_123456.3", "GENE", true, exons);
    }

    @Test
    public void properties() {
        Transcript tx = instance();

        assertThat(tx.start(), equalTo(100));
        assertThat(tx.end(), equalTo(200));
        assertThat(tx.cdsStart().pos(), equalTo(110));
        assertThat(tx.cdsEnd().pos(), equalTo(190));

        assertThat(tx.accessionId(), equalTo("NM_123456.3"));
        assertThat(tx.hgvsSymbol(), equalTo("GENE"));
        assertThat(tx.isCoding(), equalTo(true));
        assertThat(tx.length(), equalTo(100));

        List<GenomicRegion> exons = tx.exons();
        assertThat(exons.get(0), equalTo(GenomicRegion.zeroBased(CONTIG, Strand.POSITIVE, Position.of(100), Position.of(130))));
        assertThat(exons.get(1), equalTo(GenomicRegion.zeroBased(CONTIG, Strand.POSITIVE, Position.of(150), Position.of(170))));
        assertThat(exons.get(2), equalTo(GenomicRegion.zeroBased(CONTIG, Strand.POSITIVE, Position.of(180), Position.of(200))));
    }

    @Test
    public void withStrand() {
        Transcript tx = instance().withStrand(Strand.NEGATIVE);

        assertThat(tx.start(), equalTo(300));
        assertThat(tx.end(), equalTo(400));
        assertThat(tx.cdsStart().pos(), equalTo(310));
        assertThat(tx.cdsEnd().pos(), equalTo(390));
        assertThat(tx.length(), equalTo(100));

        assertThat(tx.accessionId(), equalTo("NM_123456.3"));
        assertThat(tx.hgvsSymbol(), equalTo("GENE"));
        assertThat(tx.isCoding(), equalTo(true));

        List<GenomicRegion> exons = tx.exons();
        assertThat(exons.get(0), equalTo(GenomicRegion.zeroBased(CONTIG, Strand.NEGATIVE, Position.of(300), Position.of(320))));
        assertThat(exons.get(1), equalTo(GenomicRegion.zeroBased(CONTIG, Strand.NEGATIVE, Position.of(330), Position.of(350))));
        assertThat(exons.get(2), equalTo(GenomicRegion.zeroBased(CONTIG, Strand.NEGATIVE, Position.of(370), Position.of(400))));
    }

    @Test
    public void withCoordinateSystem() {
        Transcript tx = instance().withCoordinateSystem(CoordinateSystem.ONE_BASED);

        assertThat(tx.start(), equalTo(101));
        assertThat(tx.end(), equalTo(200));
        assertThat(tx.cdsStart().pos(), equalTo(111));
        assertThat(tx.cdsEnd().pos(), equalTo(190));

        assertThat(tx.accessionId(), equalTo("NM_123456.3"));
        assertThat(tx.hgvsSymbol(), equalTo("GENE"));
        assertThat(tx.isCoding(), equalTo(true));
        assertThat(tx.length(), equalTo(100));

        List<GenomicRegion> exons = tx.exons();
        assertThat(exons.get(0), equalTo(GenomicRegion.oneBased(CONTIG, Strand.POSITIVE, Position.of(101), Position.of(130))));
        assertThat(exons.get(1), equalTo(GenomicRegion.oneBased(CONTIG, Strand.POSITIVE, Position.of(151), Position.of(170))));
        assertThat(exons.get(2), equalTo(GenomicRegion.oneBased(CONTIG, Strand.POSITIVE, Position.of(181), Position.of(200))));
    }
}