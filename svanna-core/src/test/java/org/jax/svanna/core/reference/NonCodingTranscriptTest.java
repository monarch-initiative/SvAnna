package org.jax.svanna.core.reference;

import org.jax.svanna.core.TestContig;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.svart.Contig;
import org.monarchinitiative.svart.CoordinateSystem;
import org.monarchinitiative.svart.Position;
import org.monarchinitiative.svart.Strand;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.sameInstance;

public class NonCodingTranscriptTest {

    private static final Contig CONTIG = TestContig.of(1, 500);

    @Test
    public void properties() {
        List<Exon> txExons = List.of(
                Exon.of(CoordinateSystem.zeroBased(), Position.of(100), Position.of(130)),
                Exon.of(CoordinateSystem.zeroBased(), Position.of(150), Position.of(170)),
                Exon.of(CoordinateSystem.zeroBased(), Position.of(180), Position.of(200)));
        Transcript tx = Transcript.noncoding(CONTIG, Strand.POSITIVE, CoordinateSystem.zeroBased(), 100, 200,
                "NM_123456.3",  txExons);

        assertThat(tx.start(), equalTo(100));
        assertThat(tx.end(), equalTo(200));
        assertThat(tx.length(), equalTo(100));

        assertThat(tx.isCoding(), equalTo(false));

        assertThat(tx.accessionId(), equalTo("NM_123456.3"));

        List<Exon> exons = tx.exons();
        assertThat(exons.get(0), equalTo(Exon.of(CoordinateSystem.zeroBased(), Position.of(100), Position.of(130))));
        assertThat(exons.get(1), equalTo(Exon.of(CoordinateSystem.zeroBased(), Position.of(150), Position.of(170))));
        assertThat(exons.get(2), equalTo(Exon.of(CoordinateSystem.zeroBased(), Position.of(180), Position.of(200))));
    }

    @Test
    public void withStrand_noncodingTx() {
        Transcript instance = NonCodingTranscript.of(CONTIG, Strand.POSITIVE, CoordinateSystem.zeroBased(), 100, 200,
                "NM_123456.3", List.of(
                        Exon.of(CoordinateSystem.zeroBased(), Position.of(100), Position.of(130)),
                        Exon.of(CoordinateSystem.zeroBased(), Position.of(150), Position.of(170)),
                        Exon.of(CoordinateSystem.zeroBased(), Position.of(180), Position.of(200))));

        assertThat(instance.withStrand(Strand.POSITIVE), sameInstance(instance));

        Transcript tx = instance.withStrand(Strand.NEGATIVE);

        assertThat(tx.start(), equalTo(300));
        assertThat(tx.end(), equalTo(400));
        assertThat(tx.length(), equalTo(100));

        assertThat(tx.accessionId(), equalTo("NM_123456.3"));
        assertThat(tx.isCoding(), equalTo(false));

        List<Exon> exons = tx.exons();
        assertThat(exons.get(0), equalTo(Exon.of(CoordinateSystem.zeroBased(), Position.of(300), Position.of(320))));
        assertThat(exons.get(1), equalTo(Exon.of(CoordinateSystem.zeroBased(), Position.of(330), Position.of(350))));
        assertThat(exons.get(2), equalTo(Exon.of(CoordinateSystem.zeroBased(), Position.of(370), Position.of(400))));
    }

    @Test
    public void withCoordinateSystem_noncodingTx() {
        Transcript instance = NonCodingTranscript.of(CONTIG, Strand.POSITIVE, CoordinateSystem.zeroBased(), 100, 200,
                "NM_123456.3", List.of(
                        Exon.of(CoordinateSystem.zeroBased(), Position.of(100), Position.of(130)),
                        Exon.of(CoordinateSystem.zeroBased(), Position.of(150), Position.of(170)),
                        Exon.of(CoordinateSystem.zeroBased(), Position.of(180), Position.of(200))));

        assertThat(instance.withCoordinateSystem(CoordinateSystem.zeroBased()), sameInstance(instance));

        Transcript tx = instance.withCoordinateSystem(CoordinateSystem.oneBased());

        assertThat(tx.start(), equalTo(101));
        assertThat(tx.end(), equalTo(200));
        assertThat(tx.length(), equalTo(100));

        assertThat(tx.accessionId(), equalTo("NM_123456.3"));
        assertThat(tx.isCoding(), equalTo(false));

        List<Exon> exons = tx.exons();
        assertThat(exons.get(0), equalTo(Exon.of(CoordinateSystem.oneBased(), Position.of(101), Position.of(130))));
        assertThat(exons.get(1), equalTo(Exon.of(CoordinateSystem.oneBased(), Position.of(151), Position.of(170))));
        assertThat(exons.get(2), equalTo(Exon.of(CoordinateSystem.oneBased(), Position.of(181), Position.of(200))));
    }
}