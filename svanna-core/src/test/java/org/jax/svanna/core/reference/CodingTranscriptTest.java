package org.jax.svanna.core.reference;

import org.jax.svanna.core.TestContig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.monarchinitiative.svart.Contig;
import org.monarchinitiative.svart.CoordinateSystem;
import org.monarchinitiative.svart.Coordinates;
import org.monarchinitiative.svart.Strand;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.sameInstance;

public class CodingTranscriptTest {

    private static final Contig CONTIG = TestContig.of(1, 500);

    @Test
    public void properties() {
        List<Coordinates> txExons = List.of(
                Coordinates.of(CoordinateSystem.zeroBased(), 100, 130),
                Coordinates.of(CoordinateSystem.zeroBased(), 150, 170),
                Coordinates.of(CoordinateSystem.zeroBased(), 180, 200));
        CodingTranscript tx = CodingTranscript.of(CONTIG, Strand.POSITIVE, CoordinateSystem.zeroBased(), 100, 200,
                "NM_123456.3", txExons,  115, 182);

        assertThat(tx.start(), equalTo(100));
        assertThat(tx.end(), equalTo(200));
        assertThat(tx.length(), equalTo(100));

        assertThat(tx.isCoding(), equalTo(true));
        assertThat(tx.codingStart(), equalTo(115));
        assertThat(tx.codingEnd(), equalTo(182));

        assertThat(tx.accessionId(), equalTo("NM_123456.3"));

        List<Coordinates> exons = tx.exons();
        assertThat(exons.get(0), equalTo(Coordinates.of(CoordinateSystem.zeroBased(), 100, 130)));
        assertThat(exons.get(1), equalTo(Coordinates.of(CoordinateSystem.zeroBased(), 150, 170)));
        assertThat(exons.get(2), equalTo(Coordinates.of(CoordinateSystem.zeroBased(), 180, 200)));

        assertThat(tx.fivePrimeUtrLength(), equalTo(15));
        assertThat(tx.threePrimeUtrLength(), equalTo(18));
    }

    @Test
    public void withStrand() {
        List<Coordinates> txExons = List.of(
                Coordinates.of(CoordinateSystem.zeroBased(), 100, 130),
                Coordinates.of(CoordinateSystem.zeroBased(), 150, 170),
                Coordinates.of(CoordinateSystem.zeroBased(), 180, 200));
        CodingTranscript instance = CodingTranscript.of(CONTIG, Strand.POSITIVE, CoordinateSystem.zeroBased(), 100, 200,
                "NM_123456.3", txExons, 110, 190);
        assertThat(instance.withStrand(Strand.POSITIVE), sameInstance(instance));

        CodingTranscript tx = instance.withStrand(Strand.NEGATIVE);

        assertThat(tx.start(), equalTo(300));
        assertThat(tx.end(), equalTo(400));
        assertThat(tx.strand(), equalTo(Strand.NEGATIVE));
        assertThat(tx.coordinateSystem(), equalTo(CoordinateSystem.zeroBased()));
        assertThat(tx.length(), equalTo(100));

        assertThat(tx.accessionId(), equalTo("NM_123456.3"));
        assertThat(tx.isCoding(), equalTo(true));
        assertThat(tx.codingStart(), equalTo(310));
        assertThat(tx.codingEnd(), equalTo(390));


        List<Coordinates> exons = tx.exons();
        assertThat(exons.get(0), equalTo(Coordinates.of(CoordinateSystem.zeroBased(), 300, 320)));
        assertThat(exons.get(1), equalTo(Coordinates.of(CoordinateSystem.zeroBased(), 330, 350)));
        assertThat(exons.get(2), equalTo(Coordinates.of(CoordinateSystem.zeroBased(), 370, 400)));
    }


    @Test
    public void withCoordinateSystem() {
        List<Coordinates> txExons = List.of(
                Coordinates.of(CoordinateSystem.zeroBased(), 100, 130),
                Coordinates.of(CoordinateSystem.zeroBased(), 150, 170),
                Coordinates.of(CoordinateSystem.zeroBased(), 180, 200));
        CodingTranscript instance = CodingTranscript.of(CONTIG, Strand.POSITIVE, CoordinateSystem.zeroBased(), 100, 200,
                "NM_123456.3", txExons, 110, 190);
        assertThat(instance.withCoordinateSystem(CoordinateSystem.zeroBased()), sameInstance(instance));

        CodingTranscript tx = instance.withCoordinateSystem(CoordinateSystem.oneBased());

        assertThat(tx.start(), equalTo(101));
        assertThat(tx.end(), equalTo(200));
        assertThat(tx.isCoding(), equalTo(true));
        assertThat(tx.codingStart(), equalTo(111));
        assertThat(tx.codingEnd(), equalTo(190));

        assertThat(tx.accessionId(), equalTo("NM_123456.3"));
        assertThat(tx.isCoding(), equalTo(true));
        assertThat(tx.length(), equalTo(100));

        List<Coordinates> exons = tx.exons();
        assertThat(exons.get(1), equalTo(Coordinates.of(CoordinateSystem.oneBased(), 151, 170)));
        assertThat(exons.get(0), equalTo(Coordinates.of(CoordinateSystem.oneBased(), 101, 130)));
        assertThat(exons.get(2), equalTo(Coordinates.of(CoordinateSystem.oneBased(), 181, 200)));
    }

    @ParameterizedTest
    @CsvSource({
            "100, 190,    0",
            "105, 190,    5",
            "150, 190,   30",
            "180, 190,   50",
    })
    public void fiveUtrLength(int cdsStart, int cdsEnd, int length) {
        List<Coordinates> txExons = List.of(
                Coordinates.of(CoordinateSystem.zeroBased(), 100, 130),
                Coordinates.of(CoordinateSystem.zeroBased(), 150, 170),
                Coordinates.of(CoordinateSystem.zeroBased(), 180, 200));
        CodingTranscript tx = CodingTranscript.of(CONTIG, Strand.POSITIVE, CoordinateSystem.zeroBased(), 100, 200,
                "NM_123456.3", txExons,  cdsStart, cdsEnd);

        assertThat(tx.fivePrimeUtrLength(), equalTo(length));
    }

    @ParameterizedTest
    @CsvSource({
            "105, 200,     0",
            "105, 170,    20",
            "105, 130,    40",
    })
    public void threeUtrLength(int cdsStart, int cdsEnd, int length) {
        List<Coordinates> txExons = List.of(
                Coordinates.of(CoordinateSystem.zeroBased(), 100, 130),
                Coordinates.of(CoordinateSystem.zeroBased(), 150, 170),
                Coordinates.of(CoordinateSystem.zeroBased(), 180, 200));
        CodingTranscript tx = CodingTranscript.of(CONTIG, Strand.POSITIVE, CoordinateSystem.zeroBased(), 100, 200,
                "NM_123456.3", txExons,  cdsStart, cdsEnd);

        assertThat(tx.threePrimeUtrLength(), equalTo(length));
    }
}