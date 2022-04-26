package org.monarchinitiative.svanna.core.priority.additive;

import org.monarchinitiative.svanna.core.TestContig;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.monarchinitiative.sgenes.model.Located;
import org.monarchinitiative.svart.Contig;
import org.monarchinitiative.svart.CoordinateSystem;
import org.monarchinitiative.svart.GenomicRegion;
import org.monarchinitiative.svart.Strand;

import java.util.Arrays;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class ProjectionsTest {

    private static class SimpleLocated implements Located {
        private final GenomicRegion region;

        private SimpleLocated(GenomicRegion region) {
            this.region = region;
        }

        static SimpleLocated of(Contig contig, Strand strand, CoordinateSystem coordinateSystem, int start, int end) {
            return of(GenomicRegion.of(contig, strand, coordinateSystem, start, end));
        }

        static SimpleLocated of(GenomicRegion region) {
            return new SimpleLocated(region);
        }

        @Override
        public GenomicRegion location() {
            return region;
        }

    }

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
                            Segment.of(ctg1, Strand.POSITIVE, CoordinateSystem.zeroBased(), 0, 20, "upstream", Event.GAP, 1),
                            Segment.of(ctg1, Strand.POSITIVE, CoordinateSystem.zeroBased(), 20, 30, "deletion", Event.DELETION, 0),
                            Segment.of(ctg1, Strand.POSITIVE, CoordinateSystem.zeroBased(), 30, 50, "downstream", Event.GAP, 1)
                    ));

            Located query = SimpleLocated.of(ctg1, Strand.POSITIVE, CoordinateSystem.zeroBased(), start, end);

            List<Projection<Located>> projections = Projections.project(query, deletion);

            assertThat(!projections.isEmpty(), equalTo(expected));
            if (!projections.isEmpty()) {
                Projection<Located> projection = projections.get(0);
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
                            Segment.of(ctg1, Strand.POSITIVE, CoordinateSystem.zeroBased(), 0, 20, "upstream", Event.GAP, 1),
                            Segment.of(ctg1, Strand.POSITIVE, CoordinateSystem.zeroBased(), 20, 30, "duplication", Event.DUPLICATION, 2),
                            Segment.of(ctg1, Strand.POSITIVE, CoordinateSystem.zeroBased(), 30, 50, "downstream", Event.GAP, 1)
                    ));

            Located query = SimpleLocated.of(ctg1, Strand.POSITIVE, CoordinateSystem.zeroBased(), start, end);

            List<Projection<Located>> projections = Projections.project(query, deletion);

            assertThat(projections, hasSize(expectedSize));
            if (!projections.isEmpty()) {
                Projection<Located> projection = projections.get(0);
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
                            Segment.of(ctg1, Strand.POSITIVE, CoordinateSystem.zeroBased(), 0, 20, "upstream", Event.GAP, 1),
                            Segment.of(ctg1, Strand.POSITIVE, CoordinateSystem.zeroBased(), 20, 30, "duplication", Event.DUPLICATION, 2),
                            Segment.of(ctg1, Strand.POSITIVE, CoordinateSystem.zeroBased(), 30, 50, "downstream", Event.GAP, 1)
                    ));

            Located query = SimpleLocated.of(ctg1, Strand.POSITIVE, CoordinateSystem.zeroBased(), start, end);

            List<Projection<Located>> projections = Projections.project(query, deletion);

            assertThat(projections, hasSize(expectedNumberOfItems));
            if (!projections.isEmpty()) {
                int deletionSegmentIdx = 1; // the deletion segment is the 1th element of `deletion` list
                Projection<Located> projection = Projection.builder(deletion, query, deletion.neoContig(), Strand.POSITIVE, CoordinateSystem.zeroBased())
                        .start(expectedStart).setStartEvent(Projection.Location.of(deletionSegmentIdx, startEvent))
                        .end(expectedEnd).setEndEvent(Projection.Location.of(deletionSegmentIdx, endEvent))
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
                    Segment.of(ctg1, Strand.POSITIVE, CoordinateSystem.zeroBased(), 0, 20, "upstream", Event.GAP, 1),
                    Segment.insertion(ctg1, Strand.POSITIVE, CoordinateSystem.zeroBased(), 20, 20, "insertion", 20),
                    Segment.of(ctg1, Strand.POSITIVE, CoordinateSystem.zeroBased(), 20, 40, "downstream", Event.GAP, 1)
            ));

            Located query = SimpleLocated.of(ctg1, Strand.POSITIVE, CoordinateSystem.zeroBased(), start, end);

            List<Projection<Located>> projections = Projections.project(query, insertion);

            assertThat(!projections.isEmpty(), equalTo(expected));
            if (!projections.isEmpty()) {
                Projection<Located> projection = projections.get(0);
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
                    Segment.of(ctg1, Strand.POSITIVE, CoordinateSystem.zeroBased(), 0, 20, "upstream", Event.GAP, 1),
                    Segment.of(ctg1, Strand.POSITIVE, CoordinateSystem.zeroBased(), 20, 40, "inversion", Event.INVERSION, 1),
                    Segment.of(ctg1, Strand.POSITIVE, CoordinateSystem.zeroBased(), 40, 70, "downstream", Event.GAP, 1)
            ));

            Located query = SimpleLocated.of(ctg1, Strand.POSITIVE, CoordinateSystem.zeroBased(), start, end);

            List<Projection<Located>> projections = Projections.project(query, inversion);

            assertThat(!projections.isEmpty(), equalTo(expected));
            if (!projections.isEmpty()) {
                Projection<Located> projection = projections.get(0);
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
                    Segment.of(ctg1, Strand.POSITIVE, CoordinateSystem.zeroBased(), 0, 20, "upstream", Event.GAP, 1),
                    Segment.of(ctg1, Strand.POSITIVE, CoordinateSystem.zeroBased(), 20, 40, "inversion", Event.INVERSION, 1),
                    Segment.of(ctg1, Strand.POSITIVE, CoordinateSystem.zeroBased(), 40, 70, "downstream", Event.GAP, 1)
            ));

            SortedSet<Projection<? extends Located>> projections = new TreeSet<>(GenomicRegion::compare);

            Located one = SimpleLocated.of(ctg1, Strand.POSITIVE, CoordinateSystem.zeroBased(), 25, 30);
            projections.addAll(Projections.project(one, inversion));
            Located two = SimpleLocated.of(ctg1, Strand.POSITIVE, CoordinateSystem.zeroBased(), 35, 40);
            projections.addAll(Projections.project(two, inversion));

            assertThat(projections, hasSize(2));

            Projection<? extends Located> first = projections.first();
            assertThat(first.start(), equalTo(30));
            assertThat(first.end(), equalTo(35));
            assertThat(first.spannedEvents(), is(empty()));

            Projection<? extends Located> last = projections.last();
            assertThat(last.start(), equalTo(40));
            assertThat(last.end(), equalTo(45));
            assertThat(last.spannedEvents(), is(empty()));
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
                            Segment.of(ctg1, Strand.POSITIVE, CoordinateSystem.zeroBased(), 0, 20, "upstream", Event.GAP, 1),
                            Segment.of(ctg1, Strand.POSITIVE, CoordinateSystem.zeroBased(), 20, 20, "bndA", Event.BREAKEND, 1),
                            Segment.of(ctg2, Strand.POSITIVE, CoordinateSystem.zeroBased(), 120, 120, "bndB", Event.BREAKEND, 1),
                            Segment.of(ctg2, Strand.POSITIVE, CoordinateSystem.zeroBased(), 120, 150, "downstream", Event.GAP, 1)
                    ));

            Located query = SimpleLocated.of(ctg.equals("one") ? ctg1 : ctg2, Strand.POSITIVE, CoordinateSystem.zeroBased(), start, end);
            List<Projection<Located>> projections = Projections.project(query, breakend);

            assertThat(!projections.isEmpty(), equalTo(expected));
            if (!projections.isEmpty()) {
                Projection<Located> projection = projections.get(0);
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
                            Segment.of(ctg1, Strand.POSITIVE, CoordinateSystem.zeroBased(), 20, 40, "upstream", Event.GAP, 1),
                            Segment.of(ctg1, Strand.POSITIVE, CoordinateSystem.zeroBased(), 40, 50, "deletion", Event.DELETION, 0),
                            Segment.of(ctg1, Strand.POSITIVE, CoordinateSystem.zeroBased(), 50, 60, "downstream", Event.GAP, 1)
                    ));

            Located query = SimpleLocated.of(ctg1, Strand.POSITIVE, CoordinateSystem.zeroBased(), start, end);

            List<Projection<Located>> projections = Projections.project(query, deletion);

            assertThat(!projections.isEmpty(), equalTo(expected));
        }

        @Test
        public void projectOutOfContig() {
            TestContig ctg1 = TestContig.of(0, 100);
            TestContig ctg2 = TestContig.of(1, 200);
            Route deletion = Route.of(
                    List.of(
                            Segment.of(ctg1, Strand.POSITIVE, CoordinateSystem.zeroBased(), 20, 40, "upstream", Event.GAP, 1),
                            Segment.of(ctg1, Strand.POSITIVE, CoordinateSystem.zeroBased(), 40, 50, "deletion", Event.DELETION, 0),
                            Segment.of(ctg1, Strand.POSITIVE, CoordinateSystem.zeroBased(), 50, 60, "downstream", Event.GAP, 1)
                    ));

            Located query = SimpleLocated.of(ctg2, Strand.POSITIVE, CoordinateSystem.zeroBased(), 25, 35);
            List<Projection<Located>> projections = Projections.project(query, deletion);

            assertThat(projections.isEmpty(), equalTo(true));
        }

    }
}