package org.jax.svanna.model.gene;

import org.jax.svanna.test.TestContig;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.svart.Contig;
import org.monarchinitiative.svart.CoordinateSystem;
import org.monarchinitiative.svart.Coordinates;
import org.monarchinitiative.svart.Strand;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.sameInstance;

public class NonCodingTranscriptTest {

    private static final Contig CONTIG = TestContig.of(1, 500);

    @Test
    public void properties() {
        List<Coordinates> txExons = List.of(
                Coordinates.of(CoordinateSystem.zeroBased(), 100, 130),
                Coordinates.of(CoordinateSystem.zeroBased(), 150, 170),
                Coordinates.of(CoordinateSystem.zeroBased(), 180, 200));
        Transcript tx = Transcript.noncoding(CONTIG, Strand.POSITIVE, CoordinateSystem.zeroBased(), 100, 200,
                "NM_123456.3",  txExons);

        assertThat(tx.start(), equalTo(100));
        assertThat(tx.end(), equalTo(200));
        assertThat(tx.length(), equalTo(100));

        assertThat(tx.isCoding(), equalTo(false));

        assertThat(tx.accessionId(), equalTo("NM_123456.3"));

        List<Coordinates> exons = tx.exons();
        assertThat(exons.get(0), equalTo(Coordinates.of(CoordinateSystem.zeroBased(), 100, 130)));
        assertThat(exons.get(1), equalTo(Coordinates.of(CoordinateSystem.zeroBased(), 150, 170)));
        assertThat(exons.get(2), equalTo(Coordinates.of(CoordinateSystem.zeroBased(), 180, 200)));
    }

    @Test
    public void withStrand_noncodingTx() {
        Transcript instance = NonCodingTranscript.of(CONTIG, Strand.POSITIVE, CoordinateSystem.zeroBased(), 100, 200,
                "NM_123456.3", List.of(
                        Coordinates.of(CoordinateSystem.zeroBased(), 100, 130),
                        Coordinates.of(CoordinateSystem.zeroBased(), 150, 170),
                        Coordinates.of(CoordinateSystem.zeroBased(), 180, 200)));

        assertThat(instance.withStrand(Strand.POSITIVE), sameInstance(instance));

        Transcript tx = instance.withStrand(Strand.NEGATIVE);

        assertThat(tx.start(), equalTo(300));
        assertThat(tx.end(), equalTo(400));
        assertThat(tx.length(), equalTo(100));

        assertThat(tx.accessionId(), equalTo("NM_123456.3"));
        assertThat(tx.isCoding(), equalTo(false));

        List<Coordinates> exons = tx.exons();
        assertThat(exons.get(0), equalTo(Coordinates.of(CoordinateSystem.zeroBased(), 300, 320)));
        assertThat(exons.get(1), equalTo(Coordinates.of(CoordinateSystem.zeroBased(), 330, 350)));
        assertThat(exons.get(2), equalTo(Coordinates.of(CoordinateSystem.zeroBased(), 370, 400)));
    }

    @Test
    public void withCoordinateSystem_noncodingTx() {
        Transcript instance = NonCodingTranscript.of(CONTIG, Strand.POSITIVE, CoordinateSystem.zeroBased(), 100, 200,
                "NM_123456.3", List.of(
                        Coordinates.of(CoordinateSystem.zeroBased(), 100, 130),
                        Coordinates.of(CoordinateSystem.zeroBased(), 150, 170),
                        Coordinates.of(CoordinateSystem.zeroBased(), 180, 200)));

        assertThat(instance.withCoordinateSystem(CoordinateSystem.zeroBased()), sameInstance(instance));

        Transcript tx = instance.withCoordinateSystem(CoordinateSystem.oneBased());

        assertThat(tx.start(), equalTo(101));
        assertThat(tx.end(), equalTo(200));
        assertThat(tx.length(), equalTo(100));

        assertThat(tx.accessionId(), equalTo("NM_123456.3"));
        assertThat(tx.isCoding(), equalTo(false));

        List<Coordinates> exons = tx.exons();
        assertThat(exons.get(0), equalTo(Coordinates.of(CoordinateSystem.oneBased(), 101, 130)));
        assertThat(exons.get(1), equalTo(Coordinates.of(CoordinateSystem.oneBased(), 151, 170)));
        assertThat(exons.get(2), equalTo(Coordinates.of(CoordinateSystem.oneBased(), 181, 200)));
    }
}