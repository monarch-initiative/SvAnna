package org.jax.svanna.db.additive.dispatch;

import org.jax.svanna.db.TestContig;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.svart.*;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class RouteAssemblyUtilsTest {

    @Nested
    public class IntrachromosomalVariantsTest {

        @Test
        public void multipleVariantsOnSingleChromosome() {
            TestContig contig = TestContig.of(1, 100);
            List<GenomicVariant> variants = List.of(
                    GenomicVariant.of(contig, "one", Strand.POSITIVE, CoordinateSystem.oneBased(), 5, "C", "G"),
                    GenomicVariant.of(contig, "three", Strand.POSITIVE, CoordinateSystem.oneBased(), 30, "CCC", "C"),
                    GenomicVariant.of(contig, "two", Strand.NEGATIVE, CoordinateSystem.oneBased(), 80, "TTT", "C")
            );

            VariantArrangement assembled = RouteAssemblyUtils.assemble(variants);

            assertThat(assembled.variants(), hasSize(3));
            assertThat(assembled.variants().get(0), equalTo(variants.get(0).withStrand(Strand.POSITIVE)));
            assertThat(assembled.variants().get(1), equalTo(variants.get(2).withStrand(Strand.POSITIVE)));
            assertThat(assembled.variants().get(2), equalTo(variants.get(1).withStrand(Strand.POSITIVE)));
        }
    }

    @Nested
    public class InterchromosomalVariantsTest {

        @Test
        public void assembleWithBreakend() {
            TestContig ctg1 = TestContig.of(1, 100);
            TestContig ctg2 = TestContig.of(1, 100);
            List<GenomicVariant> variants = List.of(
                    GenomicVariant.of( "three",
                            GenomicBreakend.of(ctg1, "threeLeft", Strand.POSITIVE, Coordinates.of(CoordinateSystem.zeroBased(), 40, 40)),
                            GenomicBreakend.of(ctg2, "threeRight", Strand.NEGATIVE, Coordinates.of(CoordinateSystem.zeroBased(), 30, 30)),
                            "", "C"),
                    GenomicVariant.of(ctg1, "two", Strand.POSITIVE, CoordinateSystem.oneBased(), 30, "CCC", "C"),
                    GenomicVariant.of(ctg1, "one", Strand.POSITIVE, CoordinateSystem.oneBased(), 5, "C", "G"),
                    GenomicVariant.of(ctg2, "five", Strand.POSITIVE, CoordinateSystem.oneBased(), 20, "AAA", "C"),
                    GenomicVariant.of(ctg2, "four", Strand.NEGATIVE, CoordinateSystem.oneBased(), 40, "C", "G")
            );

            VariantArrangement assembled = RouteAssemblyUtils.assemble(variants);

            assertThat(assembled.variants(), hasSize(5));
            assertThat(assembled.variants().get(0), equalTo(variants.get(2)));
            assertThat(assembled.variants().get(1), equalTo(variants.get(1)));
            assertThat(assembled.variants().get(2), equalTo(variants.get(0)));
            assertThat(assembled.variants().get(3), equalTo(variants.get(4)));
            assertThat(assembled.variants().get(4), equalTo(variants.get(3).withStrand(Strand.NEGATIVE)));
        }


        @Test
        public void failWhenVariantIsNotUpstreamOfLeftBreakend() {
            TestContig ctg1 = TestContig.of(1, 100);
            TestContig ctg2 = TestContig.of(1, 100);
            List<GenomicVariant> variants = List.of(
                    GenomicVariant.of(ctg1, "two", Strand.POSITIVE, CoordinateSystem.oneBased(), 50, "CCC", "C"), // culprit

                    GenomicVariant.of(ctg1, "one", Strand.POSITIVE, CoordinateSystem.oneBased(), 5, "C", "G"),
                    GenomicVariant.of( "three",
                            GenomicBreakend.of(ctg1, "threeLeft", Strand.POSITIVE, Coordinates.of(CoordinateSystem.zeroBased(), 40, 40)),
                            GenomicBreakend.of(ctg2, "threeRight", Strand.NEGATIVE, Coordinates.of(CoordinateSystem.zeroBased(), 30, 30)),
                            "", "C"),
                    GenomicVariant.of(ctg2, "five", Strand.POSITIVE, CoordinateSystem.oneBased(), 20, "AAA", "C"),
                    GenomicVariant.of(ctg2, "four", Strand.NEGATIVE, CoordinateSystem.oneBased(), 40, "C", "G")
            );

            RouteAssemblyException e = assertThrows(RouteAssemblyException.class, () -> RouteAssemblyUtils.assemble(variants));
            assertThat(e.getMessage(), containsString("Variant two 1:50-52 CCC>C is not upstream of the breakend threeLeft 1:40-40"));
        }

        @Test
        public void failWhenVariantIsNotDownstreamOfRightBreakend() {
            TestContig ctg1 = TestContig.of(1, 100);
            TestContig ctg2 = TestContig.of(1, 100);
            List<GenomicVariant> variants = List.of(
                    GenomicVariant.of(ctg1, "two", Strand.POSITIVE, CoordinateSystem.oneBased(), 30, "CCC", "C"),
                    GenomicVariant.of(ctg1, "one", Strand.POSITIVE, CoordinateSystem.oneBased(), 5, "C", "G"),
                    GenomicVariant.of( "three",
                            GenomicBreakend.of(ctg1, "threeLeft", Strand.POSITIVE, Coordinates.of(CoordinateSystem.zeroBased(), 40, 40)),
                            GenomicBreakend.of(ctg2, "threeRight", Strand.NEGATIVE, Coordinates.of(CoordinateSystem.zeroBased(), 30, 30)),
                            "", "C"),
                    GenomicVariant.of(ctg2, "five", Strand.POSITIVE, CoordinateSystem.oneBased(), 20, "AAA", "C"),
                    GenomicVariant.of(ctg2, "four", Strand.NEGATIVE, CoordinateSystem.oneBased(), 25, "C", "G")  // culprit
            );

            RouteAssemblyException e = assertThrows(RouteAssemblyException.class, () -> RouteAssemblyUtils.assemble(variants));
            assertThat(e.getMessage(), containsString("Variant four 1:76-76 C>G is not downstream of the breakend threeRight 1:70-70"));
        }
    }


    @Nested
    public class SpecialCasesTest {

        @Test
        public void failOnAssemblyOfMoreThanOneBreakend() {
            TestContig ctg1 = TestContig.of(1, 100);
            TestContig ctg2 = TestContig.of(1, 100);
            List<GenomicVariant> variants = List.of(
                    GenomicVariant.of( "one",
                            GenomicBreakend.of(ctg1, "oneLeft", Strand.POSITIVE, Coordinates.of(CoordinateSystem.zeroBased(), 5, 5)),
                            GenomicBreakend.of(ctg2, "oneRight", Strand.POSITIVE, Coordinates.of(CoordinateSystem.zeroBased(), 30, 30)), "", "C"),
                    GenomicVariant.of( "one",
                            GenomicBreakend.of(ctg2, "twoLeft", Strand.POSITIVE, Coordinates.of(CoordinateSystem.zeroBased(), 40, 40)),
                            GenomicBreakend.of(ctg1, "twoRight", Strand.POSITIVE, Coordinates.of(CoordinateSystem.zeroBased(), 60, 60)), "", "")
            );

            RouteAssemblyException e = assertThrows(RouteAssemblyException.class, () -> RouteAssemblyUtils.assemble(variants));
            assertThat(e.getMessage(), containsString("Unable to assemble a list of 2(>1) breakend variants"));
        }

        @Test
        public void failOnAssemblyOnMoreThanOneContig() {
            TestContig ctg1 = TestContig.of(1, 100);
            TestContig ctg2 = TestContig.of(1, 100);
            List<GenomicVariant> variants = List.of(
                    GenomicVariant.of(ctg1, "one", Strand.POSITIVE, CoordinateSystem.oneBased(), 5, "CCCC", "C"),
                    GenomicVariant.of(ctg2, "two", Strand.POSITIVE, CoordinateSystem.oneBased(), 6, "CCC", "C")
            );

            RouteAssemblyException e = assertThrows(RouteAssemblyException.class, () -> RouteAssemblyUtils.assemble(variants));
            assertThat(e.getMessage(), containsString("Unable to assemble variants on 2(>1) contigs without knowing the breakend"));
        }

        @Test
        public void failOnAssemblyOfOverlappingVariants() {
            TestContig contig = TestContig.of(1, 100);
            List<GenomicVariant> variants = List.of(
                    GenomicVariant.of(contig, "one", Strand.POSITIVE, CoordinateSystem.oneBased(), 5, "CCCC", "C"),
                    GenomicVariant.of(contig, "two", Strand.POSITIVE, CoordinateSystem.oneBased(), 6, "CCC", "C")
            );

            RouteAssemblyException e = assertThrows(RouteAssemblyException.class, () -> RouteAssemblyUtils.assemble(variants));
            assertThat(e.getMessage(), containsString("Unable to assemble overlapping variants: one 1:5-8 CCCC>C two 1:6-8 CCC>C"));
        }

        @Test
        public void singleVariantIsAlreadyAssembled() {
            TestContig contig = TestContig.of(1, 10);
            List<GenomicVariant> variants = List.of(GenomicVariant.of(contig, "rs123", Strand.POSITIVE, CoordinateSystem.oneBased(), 5, "C", "G"));

            VariantArrangement assembled = RouteAssemblyUtils.assemble(variants);

            assertThat(assembled.variants(), hasSize(1));
            assertThat(assembled.variants(), hasItem(variants.get(0)));
        }

        @Test
        public void emptyThrowsException() {
            RouteAssemblyException e = assertThrows(RouteAssemblyException.class, () -> RouteAssemblyUtils.assemble(List.of()));
            assertThat(e.getMessage(), containsString("Variant list must not be empty"));
        }
    }

}