package org.jax.svann.reference;

import org.jax.svann.ChromosomalRegionImpl;
import org.jax.svann.ContigImpl;
import org.jax.svann.TestBase;
import org.jax.svann.reference.genome.Contig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Test default methods of {@link ChromosomalRegion}.
 */
@Deprecated
@Disabled // move tests to GenomicRegion
public class ChromosomalRegionTest extends TestBase {

    private static Contig CONTIG;

    private ChromosomalRegion region;

    @BeforeAll
    public static void beforeAll() {
        CONTIG = new ContigImpl(1, "contig", 1000);
    }

    @BeforeEach
    public void setUp() {
        region = new ChromosomalRegionImpl(CONTIG, Position.precise(101), Position.precise(200), Strand.FWD);
    }

    @Test
    public void getBegin() {
        assertThat(region.getBegin(), is(101));
    }

    @Test
    public void getEnd() {
        assertThat(region.getEnd(), is(200));
    }

    @Test
    public void length() {
        assertThat(region.length(), is(100));
    }

//    @ParameterizedTest
//    @CsvSource({
//            // first, on FWD strand
//            "105,195,FWD,true",
//            "101,200,FWD,true",
//            "95,101,FWD,true",
//            "200,205,FWD,true",
//            "91,100,FWD,false",
//            "201,205,FWD,false",
//
//            // now REV strand
//            "805,895,REV,true",
//            "801,900,REV,true",
//            "795,801,REV,true",
//            "900,905,REV,true",
//            "795,800,REV,false",
//            "901,905,REV,false"
//    })
//    public void overlapsWith(int begin, int end, Strand strand, boolean expected) {
//        ChromosomalRegion candidate = new ChromosomalRegionImpl(CONTIG, Position.precise(begin), Position.precise(end), strand);
//        assertThat(region.overlapsWith(candidate), is(expected));
//    }
//
//    @Test
//    public void overlapsWith_differentContig() {
//        final Contig contig = new ContigImpl(2, "other", 2_000);
//        final ChromosomalRegionImpl other = new ChromosomalRegionImpl(contig, Position.precise(105), Position.precise(195), Strand.FWD);
//        assertThat(region.overlapsWith(other), is(false));
//    }
//
//    @ParameterizedTest
//    @CsvSource({
//            // first, on FWD strand
//            "105,195,FWD,true",
//            "101,200,FWD,true",
//            "95,101,FWD,false",
//            "200,205,FWD,false",
//            "91,100,FWD,false",
//            "201,205,FWD,false",
//
//            // now REV strand
//            "805,895,REV,true",
//            "801,900,REV,true",
//            "795,801,REV,false",
//            "900,905,REV,false",
//            "795,800,REV,false",
//            "901,905,REV,false"
//    })
//    public void contains(int begin, int end, Strand strand, boolean expected) {
//        ChromosomalRegion candidate = new ChromosomalRegionImpl(CONTIG, Position.precise(begin), Position.precise(end), strand);
//        assertThat(region.contains(candidate), is(expected));
//    }
//
//    @Test
//    public void contains_differentContig() {
//        final Contig contig = new ContigImpl(2, "other", 2_000);
//        final ChromosomalRegionImpl other = new ChromosomalRegionImpl(contig, Position.precise(105), Position.precise(195), Strand.FWD);
//        assertThat(region.contains(other), is(false));
//    }
}
