package org.jax.svanna.core.filter;

import org.jax.svanna.core.TestContig;
import org.jax.svanna.core.TestDataConfig;
import org.jax.svanna.core.landscape.AnnotationDataService;
import org.jax.svanna.core.landscape.RepeatFamily;
import org.jax.svanna.core.landscape.RepetitiveRegion;
import org.jax.svanna.core.reference.SvannaVariant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.monarchinitiative.svart.Contig;
import org.monarchinitiative.svart.CoordinateSystem;
import org.monarchinitiative.svart.Position;
import org.monarchinitiative.svart.Strand;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = TestDataConfig.class)
public class RepetitiveRegionVariantFilterTest {

    @Mock
    public AnnotationDataService annotationDataService;

    private final float similarityThreshold = 80.F;

    private RepetitiveRegionVariantFilter instance;

    @BeforeEach
    public void setUp() {
        instance = new RepetitiveRegionVariantFilter(annotationDataService, similarityThreshold);
    }

    @ParameterizedTest
    @CsvSource({
            " 5, 10,   PASS",
            " 5, 16,   PASS",
            "10, 17,   FAIL",
            "10, 22,   FAIL",
            "10, 23,   PASS",
    })
    public void runFilter(int start, int end, String expected) {
        Contig ctg = TestContig.of(1, 100);
        SvannaVariant variant = TestVariant.of(ctg, "id", Strand.POSITIVE, CoordinateSystem.zeroBased(),
                Position.of(start), Position.of(end), "N", "<DEL>", -(end - start));
        List<RepetitiveRegion> repeats = List.of(
                RepetitiveRegion.of(ctg, Strand.POSITIVE, CoordinateSystem.zeroBased(), Position.of(10), Position.of(20), RepeatFamily.SIMPLE_REPEAT),
                RepetitiveRegion.of(ctg, Strand.POSITIVE, CoordinateSystem.zeroBased(), Position.of(30), Position.of(40), RepeatFamily.SIMPLE_REPEAT));
        when(annotationDataService.overlappingRepetitiveRegions(variant))
                .thenReturn(repeats.stream().filter(r -> r.overlapsWith(variant)).collect(Collectors.toList()));

        FilterResult result = instance.runFilter(variant);

        assertThat(result, equalTo(parseExpected(expected)));
    }

    private FilterResult parseExpected(String expected) {
        switch (expected) {
            case "FAIL":
                return FilterResult.fail(FilterType.REPETITIVE_REGION_FILTER);
            case "PASS":
                return FilterResult.pass(FilterType.REPETITIVE_REGION_FILTER);
            case "NOT_RUN":
                return FilterResult.notRun(FilterType.REPETITIVE_REGION_FILTER);
            default:
                fail("Unknown expected filter result");
                throw new RuntimeException();// unreachable
        }
    }
}