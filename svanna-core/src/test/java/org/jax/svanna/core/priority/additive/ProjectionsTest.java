package org.jax.svanna.core.priority.additive;

import org.jax.svanna.core.TestContig;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.monarchinitiative.svart.CoordinateSystem;
import org.monarchinitiative.svart.GenomicRegion;
import org.monarchinitiative.svart.Position;
import org.monarchinitiative.svart.Strand;

import java.util.Arrays;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class ProjectionsTest {

    @Nested
    public class SimpleAll {

        @ParameterizedTest
        @CsvSource({
                "10, 20,      true, 10, 20,      GAP,     GAP, ''",
                "10, 21,     false,  0,  0,  UNKNOWN, UNKNOWN, ''",
                "20, 30,     false,  0,  0,  UNKNOWN, UNKNOWN, ''",
                "29, 35,     false,  0,  0,  UNKNOWN, UNKNOWN, ''",
                "30, 45,      true, 20, 35,      GAP,     GAP, ''",

                "15, 35,      true, 15, 25,      GAP,     GAP, DELETION", // spanning through

        })
        public void projectOnDeletion(int start, int end, boolean expected, int expectedStart, int expectedEnd,
                                      Event startEvent, Event endEvent, String spannedEvents) {
            TestContig ctg1 = TestContig.of(0, 100);
            Route deletion = Route.of(
                    List.of(
                            Segment.of(ctg1, Strand.POSITIVE, CoordinateSystem.zeroBased(), Position.of(0), Position.of(20), "upstream", Event.GAP, 1),
                            Segment.of(ctg1, Strand.POSITIVE, CoordinateSystem.zeroBased(), Position.of(20), Position.of(30), "deletion", Event.DELETION, 0),
                            Segment.of(ctg1, Strand.POSITIVE, CoordinateSystem.zeroBased(), Position.of(30), Position.of(50), "downstream", Event.GAP, 1)
                    ));

            GenomicRegion query = GenomicRegion.of(ctg1, Strand.POSITIVE, CoordinateSystem.zeroBased(), Position.of(start), Position.of(end));

            List<Projection<GenomicRegion>> projections = Projections.projectAll(query, deletion);

            assertThat(!projections.isEmpty(), equalTo(expected));
            if (!projections.isEmpty()) {
                Projection<GenomicRegion> projection = projections.get(0);
                assertThat(projection.start(), equalTo(expectedStart));
                assertThat(projection.startEvent(), equalTo(startEvent));
                assertThat(projection.end(), equalTo(expectedEnd));
                assertThat(projection.endEvent(), equalTo(endEvent));
                if (!spannedEvents.isEmpty()) {
                    Arrays.stream(spannedEvents.split("\\|"))
                            .map(Event::valueOf)
                            .forEach(event -> assertThat(projection.spannedEvents().contains(event), equalTo(true)));
                }
            }
        }

        @ParameterizedTest
        @CsvSource({
                "10, 15,     1, 10, 15, GAP,                 GAP, ''",
                "10, 25,     1, 10, 25, GAP,         DUPLICATION, ''",
                "10, 40,     1, 10, 50, GAP,                 GAP, DUPLICATION",
                "25, 35,     1, 35, 45, DUPLICATION,         GAP, ''",
                "35, 45,     1, 45, 55, GAP,                 GAP, ''",
        })
        public void projectOnDuplication(int start, int end, int expectedSize, int expectedStart, int expectedEnd,
                                         Event startEvent, Event endEvent, String spannedEvents) {
            TestContig ctg1 = TestContig.of(0, 100);
            Route deletion = Route.of(
                    List.of(
                            Segment.of(ctg1, Strand.POSITIVE, CoordinateSystem.zeroBased(), Position.of(0), Position.of(20), "upstream", Event.GAP, 1),
                            Segment.of(ctg1, Strand.POSITIVE, CoordinateSystem.zeroBased(), Position.of(20), Position.of(30), "duplication", Event.DUPLICATION, 2),
                            Segment.of(ctg1, Strand.POSITIVE, CoordinateSystem.zeroBased(), Position.of(30), Position.of(50), "downstream", Event.GAP, 1)
                    ));

            GenomicRegion query = GenomicRegion.of(ctg1, Strand.POSITIVE, CoordinateSystem.zeroBased(), Position.of(start), Position.of(end));

            List<Projection<GenomicRegion>> projections = Projections.projectAll(query, deletion);

            assertThat(projections, hasSize(expectedSize));
            if (!projections.isEmpty()) {
                Projection<GenomicRegion> projection = projections.get(0);
                assertThat(projection.start(), equalTo(expectedStart));
                assertThat(projection.startEvent(), equalTo(startEvent));
                assertThat(projection.end(), equalTo(expectedEnd));
                assertThat(projection.endEvent(), equalTo(endEvent));
                if (!spannedEvents.isEmpty())
                    Arrays.stream(spannedEvents.split("\\|"))
                            .map(Event::valueOf)
                            .forEach(event -> assertThat(projection.spannedEvents().contains(event), equalTo(true)));
            }
        }

        @ParameterizedTest
        @CsvSource({
                "22, 27,     2, 1, 22, 27, DUPLICATION, DUPLICATION",
                "22, 27,     2, 2, 32, 37, DUPLICATION, DUPLICATION",
        })
        public void projectOnDuplicationWhenWithinTheDuplicatedRegion(int start, int end, int expectedNumberOfItems,
                                                                      int segmentIdx, int expectedStart, int expectedEnd,
                                                                      Event startEvent, Event endEvent) {
            TestContig ctg1 = TestContig.of(0, 100);
            Route deletion = Route.of(
                    List.of(
                            Segment.of(ctg1, Strand.POSITIVE, CoordinateSystem.zeroBased(), Position.of(0), Position.of(20), "upstream", Event.GAP, 1),
                            Segment.of(ctg1, Strand.POSITIVE, CoordinateSystem.zeroBased(), Position.of(20), Position.of(30), "duplication", Event.DUPLICATION, 2),
                            Segment.of(ctg1, Strand.POSITIVE, CoordinateSystem.zeroBased(), Position.of(30), Position.of(50), "downstream", Event.GAP, 1)
                    ));

            GenomicRegion query = GenomicRegion.of(ctg1, Strand.POSITIVE, CoordinateSystem.zeroBased(), Position.of(start), Position.of(end));

            List<Projection<GenomicRegion>> projections = Projections.projectAll(query, deletion);

            assertThat(projections, hasSize(expectedNumberOfItems));
            if (!projections.isEmpty()) {
                int deletionSegmentIdx = 1; // the deletion segment is the 1th element of `deletion` list
                Projection<GenomicRegion> projection = Projection.builder(deletion, query, deletion.neoContig(), Strand.POSITIVE, CoordinateSystem.zeroBased())
                        .start(Position.of(expectedStart)).setStartEvent(Projection.Location.of(deletionSegmentIdx, startEvent))
                        .end(Position.of(expectedEnd)).setEndEvent(Projection.Location.of(deletionSegmentIdx, endEvent))
                        .build();
                assertThat(projections, hasItem(projection));
            }
        }

        @ParameterizedTest
        @CsvSource({
                "10, 20,     true,  10, 20, GAP, GAP, ''",
                "10, 21,     true,  10, 41, GAP, GAP, INSERTION",
                "19, 25,     true,  19, 45, GAP, GAP, INSERTION",
                "20, 30,     true,  40, 50, GAP, GAP, ''",
        })
        public void projectOnInsertion(int start, int end,
                                       boolean expected, int expectedStart, int expectedEnd,
                                       Event startEvent, Event endEvent, String spannedEvents) {
            TestContig ctg1 = TestContig.of(0, 100);
            Route insertion = Route.of(List.of(
                    Segment.of(ctg1, Strand.POSITIVE, CoordinateSystem.zeroBased(), Position.of(0), Position.of(20), "upstream", Event.GAP, 1),
                    Segment.insertion(ctg1, Strand.POSITIVE, CoordinateSystem.zeroBased(), Position.of(20), Position.of(20), "insertion", 20),
                    Segment.of(ctg1, Strand.POSITIVE, CoordinateSystem.zeroBased(), Position.of(20), Position.of(40), "downstream", Event.GAP, 1)
            ));

            GenomicRegion query = GenomicRegion.of(ctg1, Strand.POSITIVE, CoordinateSystem.zeroBased(), Position.of(start), Position.of(end));

            List<Projection<GenomicRegion>> projections = Projections.projectAll(query, insertion);

            assertThat(!projections.isEmpty(), equalTo(expected));
            if (!projections.isEmpty()) {
                Projection<GenomicRegion> projection = projections.get(0);
                assertThat(projection.start(), equalTo(expectedStart));
                assertThat(projection.startEvent(), equalTo(startEvent));
                assertThat(projection.end(), equalTo(expectedEnd));
                assertThat(projection.endEvent(), equalTo(endEvent));
                if (!spannedEvents.isEmpty())
                    Arrays.stream(spannedEvents.split("\\|"))
                            .map(Event::valueOf)
                            .forEach(event -> assertThat(projection.spannedEvents().contains(event), equalTo(true)));
            }
        }

        @ParameterizedTest
        @CsvSource({
                "10, 15,      true, 10, 15,      POSITIVE,       GAP,       GAP, ''",
                "10, 25,     false,  0,  0,      POSITIVE,   UNKNOWN,   UNKNOWN, ''",
                "25, 30,      true, 40, 45,      NEGATIVE, INVERSION, INVERSION, ''", // fully within the inverted region
                "15, 45,      true, 15, 45,      POSITIVE,       GAP,       GAP, INVERSION", // spanning the inversion
                "25, 45,     false,  0,  0,      POSITIVE,   UNKNOWN,   UNKNOWN, ''",
                "45, 55,      true, 45, 55,      POSITIVE,       GAP,       GAP, ''",
        })
        public void projectOnInversion(int start, int end, boolean expected, int expectedStart, int expectedEnd, Strand strand,
                                       Event startEvent, Event endEvent, String spannedEvents) {
            TestContig ctg1 = TestContig.of(0, 100);
            Route inversion = Route.of(List.of(
                    Segment.of(ctg1, Strand.POSITIVE, CoordinateSystem.zeroBased(), Position.of(0), Position.of(20), "upstream", Event.GAP, 1),
                    Segment.of(ctg1, Strand.POSITIVE, CoordinateSystem.zeroBased(), Position.of(20), Position.of(40), "inversion", Event.INVERSION, 1),
                    Segment.of(ctg1, Strand.POSITIVE, CoordinateSystem.zeroBased(), Position.of(40), Position.of(70), "downstream", Event.GAP, 1)
            ));

            GenomicRegion query = GenomicRegion.of(ctg1, Strand.POSITIVE, CoordinateSystem.zeroBased(), Position.of(start), Position.of(end));

            List<Projection<GenomicRegion>> projections = Projections.projectAll(query, inversion);

            assertThat(!projections.isEmpty(), equalTo(expected));
            if (!projections.isEmpty()) {
                Projection<GenomicRegion> projection = projections.get(0);
                assertThat(projection.start(), equalTo(expectedStart));
                assertThat(projection.startEvent(), equalTo(startEvent));
                assertThat(projection.end(), equalTo(expectedEnd));
                assertThat(projection.endEvent(), equalTo(endEvent));
                assertThat(projection.strand(), equalTo(strand));
                if (!spannedEvents.isEmpty())
                    Arrays.stream(spannedEvents.split("\\|"))
                            .map(Event::valueOf)
                            .forEach(event -> assertThat(projection.spannedEvents().contains(event), equalTo(true)));
            }
        }

        @Test
        public void inv() {
            TestContig ctg1 = TestContig.of(0, 100);
            Route inversion = Route.of(List.of(
                    Segment.of(ctg1, Strand.POSITIVE, CoordinateSystem.zeroBased(), Position.of(0), Position.of(20), "upstream", Event.GAP, 1),
                    Segment.of(ctg1, Strand.POSITIVE, CoordinateSystem.zeroBased(), Position.of(20), Position.of(40), "inversion", Event.INVERSION, 1),
                    Segment.of(ctg1, Strand.POSITIVE, CoordinateSystem.zeroBased(), Position.of(40), Position.of(70), "downstream", Event.GAP, 1)
            ));

            SortedSet<Projection<? extends GenomicRegion>> projections = new TreeSet<>(GenomicRegion::compare);

            GenomicRegion first = GenomicRegion.of(ctg1, Strand.POSITIVE, CoordinateSystem.zeroBased(), Position.of(25), Position.of(30));
            projections.addAll(Projections.projectAll(first, inversion));
            GenomicRegion second = GenomicRegion.of(ctg1, Strand.POSITIVE, CoordinateSystem.zeroBased(), Position.of(35), Position.of(40));
            projections.addAll(Projections.projectAll(second, inversion));

            projections.forEach(System.err::println);
        }

        @ParameterizedTest
        @CsvSource({
                "one,   10,   15,      true, 10, 15,     GAP,     GAP, ''",
                "one,   10,   21,     false,  0,  0, UNKNOWN, UNKNOWN, ''",
                "one,   21,   30,     false,  0,  0, UNKNOWN, UNKNOWN, ''",

                "two,  100,  120,     false,  0,  0, UNKNOWN, UNKNOWN, ''",
                "two,  100,  130,     false,  0,  0, UNKNOWN, UNKNOWN, ''",
                "two,  130,  140,      true, 30, 40,     GAP,     GAP, ''",
        })
        public void projectOnBreakend(String ctg, int start, int end, boolean expected, int expectedStart, int expectedEnd,
                                      Event startEvent, Event endEvent, String spannedEvents) {
            TestContig ctg1 = TestContig.of(0, 100);
            TestContig ctg2 = TestContig.of(1, 200);
            Route breakend = Route.of(
                    List.of(
                            Segment.of(ctg1, Strand.POSITIVE, CoordinateSystem.zeroBased(), Position.of(0), Position.of(20), "upstream", Event.GAP, 1),
                            Segment.of(ctg1, Strand.POSITIVE, CoordinateSystem.zeroBased(), Position.of(20), Position.of(20), "bndA", Event.BREAKEND, 1),
                            Segment.of(ctg2, Strand.POSITIVE, CoordinateSystem.zeroBased(), Position.of(120), Position.of(120), "bndB", Event.BREAKEND, 1),
                            Segment.of(ctg2, Strand.POSITIVE, CoordinateSystem.zeroBased(), Position.of(120), Position.of(150), "downstream", Event.GAP, 1)
                    ));

            GenomicRegion query = GenomicRegion.of(ctg.equals("one") ? ctg1 : ctg2, Strand.POSITIVE, CoordinateSystem.zeroBased(), Position.of(start), Position.of(end));
            List<Projection<GenomicRegion>> projections = Projections.projectAll(query, breakend);

            assertThat(!projections.isEmpty(), equalTo(expected));
            if (!projections.isEmpty()) {
                Projection<GenomicRegion> projection = projections.get(0);
                assertThat(projection.start(), equalTo(expectedStart));
                assertThat(projection.startEvent(), equalTo(startEvent));
                assertThat(projection.end(), equalTo(expectedEnd));
                assertThat(projection.endEvent(), equalTo(endEvent));
                if (!spannedEvents.isEmpty())
                    Arrays.stream(spannedEvents.split("\\|"))
                            .map(Event::valueOf)
                            .forEach(event -> assertThat(projection.spannedEvents().contains(event), equalTo(true)));
            }
        }

        @ParameterizedTest
        @CsvSource({
                "10, 20,      false",
                "10, 25,      false",
                "55, 65,      false",
                "61, 65,      false",
        })
        public void projectOutOfSegments(int start, int end, boolean expected) {
            TestContig ctg1 = TestContig.of(0, 100);
            Route deletion = Route.of(
                    List.of(
                            Segment.of(ctg1, Strand.POSITIVE, CoordinateSystem.zeroBased(), Position.of(20), Position.of(40), "upstream", Event.GAP, 1),
                            Segment.of(ctg1, Strand.POSITIVE, CoordinateSystem.zeroBased(), Position.of(40), Position.of(50), "deletion", Event.DELETION, 0),
                            Segment.of(ctg1, Strand.POSITIVE, CoordinateSystem.zeroBased(), Position.of(50), Position.of(60), "downstream", Event.GAP, 1)
                    ));

            GenomicRegion query = GenomicRegion.of(ctg1, Strand.POSITIVE, CoordinateSystem.zeroBased(), Position.of(start), Position.of(end));

            List<Projection<GenomicRegion>> projections = Projections.projectAll(query, deletion);

            assertThat(!projections.isEmpty(), equalTo(expected));
        }

        @Test
        public void projectOutOfContig() {
            TestContig ctg1 = TestContig.of(0, 100);
            TestContig ctg2 = TestContig.of(1, 200);
            Route deletion = Route.of(
                    List.of(
                            Segment.of(ctg1, Strand.POSITIVE, CoordinateSystem.zeroBased(), Position.of(20), Position.of(40), "upstream", Event.GAP, 1),
                            Segment.of(ctg1, Strand.POSITIVE, CoordinateSystem.zeroBased(), Position.of(40), Position.of(50), "deletion", Event.DELETION, 0),
                            Segment.of(ctg1, Strand.POSITIVE, CoordinateSystem.zeroBased(), Position.of(50), Position.of(60), "downstream", Event.GAP, 1)
                    ));

            GenomicRegion query = GenomicRegion.of(ctg2, Strand.POSITIVE, CoordinateSystem.zeroBased(), Position.of(25), Position.of(35));
            List<Projection<GenomicRegion>> projections = Projections.projectAll(query, deletion);

            assertThat(projections.isEmpty(), equalTo(true));
        }

    }


//    @ParameterizedTest
//    @CsvSource({
//            "one,  30,  55,   true, 10, 25",
//            "two, 130, 160,   true, 30, 65",
//
//            // trimmed
//            "one,  30,  45,   true, 10, 20",
//    })
//    @Disabled
//    public void projectOnDelBndDup(String ctg, int start, int end, boolean expected, int expectedStart, int expectedEnd) {
//        TestContig ctg1 = TestContig.of(0, 100);
//        TestContig ctg2 = TestContig.of(1, 200);
//        Route delBnd = Route.of(
//                List.of(
//                        Segment.of(ctg1, Strand.POSITIVE, CoordinateSystem.zeroBased(), Position.of(20), Position.of(40), "upstream", Event.GAP, 1),      // 20 (20)
//                        Segment.of(ctg1, Strand.POSITIVE, CoordinateSystem.zeroBased(), Position.of(40), Position.of(50), "deletion", Event.DELETION, 0),      // 0  (20)
//                        Segment.of(ctg1, Strand.POSITIVE, CoordinateSystem.zeroBased(), Position.of(50), Position.of(55), "gap-1", Event.GAP, 1),         // 5  (25)
//                        Segment.of(ctg1, Strand.POSITIVE, CoordinateSystem.zeroBased(), Position.of(55), Position.of(55), "bndA", Event.BREAKEND, 1),          // 0  (25)
//                        Segment.of(ctg2, Strand.POSITIVE, CoordinateSystem.zeroBased(), Position.of(125), Position.of(125), "bndB", Event.BREAKEND, 1),        // 0  (25)
//                        Segment.of(ctg2, Strand.POSITIVE, CoordinateSystem.zeroBased(), Position.of(125), Position.of(145), "gap-2", Event.GAP, 1),       // 20 (45)
//                        Segment.of(ctg2, Strand.POSITIVE, CoordinateSystem.zeroBased(), Position.of(145), Position.of(150), "duplication", Event.DUPLICATION, 2), // 10 (55)
//                        Segment.of(ctg2, Strand.POSITIVE, CoordinateSystem.zeroBased(), Position.of(150), Position.of(180), "downstream", Event.GAP, 1)   // 30 (85)
//                ));
//
//        GenomicRegion query = GenomicRegion.of(ctg.equals("one") ? ctg1 : ctg2, Strand.POSITIVE, CoordinateSystem.zeroBased(), Position.of(start), Position.of(end));
//
//        List<Projection<GenomicRegion>> projectOpt = Projections.projectAll(query, delBnd);
//
//        assertThat(!projectOpt.isEmpty(), equalTo(expected));
//        if (!projectOpt.isEmpty()) {
//            Projection<GenomicRegion> projection = projectOpt.get(0);
//            assertThat(projection.start(), equalTo(expectedStart));
//            assertThat(projection.end(), equalTo(expectedEnd));
//        }
//    }

    private static void printOutProjection(Projection<GenomicRegion> projection) {
        System.err.println(projection.startLocation());
        System.err.println(projection.endLocation());
        System.err.println(projection.spannedLocations());
        System.err.println(projection.source());
    }
}