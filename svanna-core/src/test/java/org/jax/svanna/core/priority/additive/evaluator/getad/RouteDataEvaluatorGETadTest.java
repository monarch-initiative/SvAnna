package org.jax.svanna.core.priority.additive.evaluator.getad;

import org.jax.svanna.core.TestContig;
import org.jax.svanna.core.TestEnhancer;
import org.jax.svanna.core.TestTad;
import org.jax.svanna.core.priority.additive.*;
import org.jax.svanna.core.priority.additive.impact.SequenceImpactCalculator;
import org.jax.svanna.core.priority.additive.impact.SimpleSequenceImpactCalculator;
import org.jax.svanna.model.landscape.enhancer.Enhancer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.monarchinitiative.svart.*;
import org.monarchinitiative.sgenes.model.Gene;
import org.monarchinitiative.sgenes.model.GeneIdentifier;
import org.monarchinitiative.sgenes.model.Transcript;
import org.monarchinitiative.sgenes.model.TranscriptIdentifier;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;

public class RouteDataEvaluatorGETadTest {

    private static final double ERROR = 1E-6;

    private final SequenceImpactCalculator<Gene> geneImpact = new SimpleSequenceImpactCalculator<>(10.);
    private final GeneWeightCalculator geneWeightCalculator = GeneWeightCalculator.defaultGeneRelevanceCalculator();

    private final SequenceImpactCalculator<Enhancer> enhancerImpact = new SimpleSequenceImpactCalculator<>(1.);
    private final EnhancerGeneRelevanceCalculator enhancerGeneRelevanceCalculator = EnhancerGeneRelevanceCalculator.defaultCalculator();

    private RouteDataEvaluator<RouteDataGETad, ? extends RouteResult> evaluator;

    private static Gene makeGene(String id, String symbol, Contig contig, int start, int end) {
        GenomicRegion location = GenomicRegion.of(contig, Strand.POSITIVE, CoordinateSystem.zeroBased(), start, end);

        TranscriptIdentifier txId = TranscriptIdentifier.of(id + "_tx", symbol + "_tx", null);
        List<Coordinates> exons = List.of(Coordinates.of(CoordinateSystem.zeroBased(), start, end));
        Coordinates cdsCoordinates = Coordinates.of(CoordinateSystem.zeroBased(), start, end);
        Transcript tx = Transcript.of(txId, location, exons, cdsCoordinates);

        GeneIdentifier geneId = GeneIdentifier.of(id, symbol, null, null);
        return Gene.of(geneId, location, List.of(tx));
    }

    private static Routes makeRoutes(GenomicRegion reference, Segment... segments) {
        return Routes.of(List.of(reference), List.of(Route.of(List.of(segments))));
    }

    private static Segment positiveDeletion(Contig contig, int start, int end) {
        return positiveSegment("del", contig, start, end, Event.DELETION, 0);
    }

    private static Segment positiveDuplication(Contig contig, int start, int end) {
        return positiveSegment("del", contig, start, end, Event.DUPLICATION, 2);
    }

    private static Segment positiveInversion(Contig contig, int start, int end) {
        return positiveSegment("inv", contig, start, end, Event.INVERSION, 1);
    }

    private static Segment positiveGap(Contig contig, int start, int end) {
        return positiveSegment("gap", contig, start, end, Event.GAP, 1);
    }

    private static Segment positiveBreakend(Contig contig, int pos) {
        return positiveSegment("bnd", contig, pos, pos, Event.BREAKEND, 1);
    }

    private static Segment positiveSegment(String id, Contig contig, int start, int end, Event event, int copies) {
        return Segment.of(contig, Strand.POSITIVE, CoordinateSystem.zeroBased(), start, end, id, event, copies);
    }

    private static Segment negativeSegment(String id, Contig contig, int start, int end, Event event, int copies) {
        return Segment.of(contig, Strand.NEGATIVE, CoordinateSystem.zeroBased(), start, end, id, event, copies);
    }

    @BeforeEach
    public void setUp() {
        evaluator = new GranularRouteDataEvaluatorGETad(geneImpact, geneWeightCalculator, enhancerImpact, enhancerGeneRelevanceCalculator);
    }

    @ParameterizedTest
    @CsvSource({
            "0,   10,  15,   80,               .0",      //  nothing is knocked out
            "0,    4,  10,   80,               .2",      //  enhancer knocked out
            "0,   30,  40,   80,             27.382818", //  gene knocked out
            "0,   30,  55,   80,             27.482818", // `gene and enhancer knocked out
            "0,   30,  70,   80,             54.765636", //  2 genes and enhancer knocked out
    })
    public void evaluateDeletion(int start, int delStart, int delEnd, int end, double expected) {
        Contig ctg1 = TestContig.of(0, 100);
        Routes routes = makeRoutes(
                GenomicRegion.of(ctg1, Strand.POSITIVE, CoordinateSystem.zeroBased(), start, end),
                positiveGap(ctg1, start, delStart),
                positiveDeletion(ctg1, delStart, delEnd),
                positiveGap(ctg1, delEnd, end));
        RouteDataGETad routeData = RouteDataGETad.builder(routes)
                .addEnhancer(TestEnhancer.of(ctg1, Strand.POSITIVE, CoordinateSystem.zeroBased(), 5, 10, "a"))
                .addGene(makeGene("NCBIGene:A", "A", ctg1, 20, 40))
                .addEnhancer(TestEnhancer.of(ctg1, Strand.POSITIVE, CoordinateSystem.zeroBased(), 50, 55, "b"))
                .addGene(makeGene("NCBIGene:B", "B", ctg1, 60, 70))
                .build();

        double score = evaluator.evaluate(routeData).priority();

        assertThat(score, closeTo(expected, ERROR));
    }

    @ParameterizedTest
    @CsvSource({
            "0,   10,  15,   80,               .0",      //  nothing is knocked out
            "0,    4,  10,   80,               .1",      //  enhancer is knocked out
            "0,   45,  50,   80,               .2",      //  TAD is knocked out
            "0,   30,  40,   80,             27.282818", // `A` knocked out
            "0,   30,  70,   80,             54.565636", // `AbB` knocked out
    })
    public void evaluateDeletionWithTad(int start, int delStart, int delEnd, int end, double expected) {
        Contig ctg1 = TestContig.of(0, 100);
        Routes routes = makeRoutes(
                GenomicRegion.of(ctg1, Strand.POSITIVE, CoordinateSystem.zeroBased(), start, end),
                positiveGap(ctg1, start, delStart),
                positiveDeletion(ctg1, delStart, delEnd),
                positiveGap(ctg1, delEnd, end));
        RouteDataGETad routeData = RouteDataGETad.builder(routes)
                .addEnhancer(TestEnhancer.of(ctg1, Strand.POSITIVE, CoordinateSystem.zeroBased(), 5, 10, "a"))
                .addGene(makeGene("NCBIGene:A", "A", ctg1, 20, 40))
                .addTadBoundary(TestTad.of("X", ctg1, Strand.POSITIVE, CoordinateSystem.zeroBased(), 45, 47))
                .addEnhancer(TestEnhancer.of(ctg1, Strand.POSITIVE, CoordinateSystem.zeroBased(), 50, 55, "b"))
                .addGene(makeGene("NCBIGene:B", "B", ctg1, 60, 70))
                .build();

        double score = evaluator.evaluate(routeData).priority();

        assertThat(score, closeTo(expected, ERROR));
    }

    @ParameterizedTest
    @CsvSource({
            "0,   10,  15,   80,                .0",        //  nothing is duplicated
            "0,    5,  10,   80,                .1",        //  enhancer is duplicated
            "0,    7,  13,   80,                .0",        //  enhancer is spanned by a tandem duplication
            "0,   15,  55,   80,              27.282818",  //  gene duplication and enhancer adoption `HGNC:A` <-> `b`
            "0,   15,  45,   80,              27.282818",  //  `HGNC:A` duplication
            "0,   15,  50,   80,              27.182818",  //  `HGNC:A` duplication within a neo-TAD
            "0,   40,  55,   80,               0.",        //  enhancer duplication
            "0,   20,  70,   80,              54.565636",  //  `AXbB` duplicated
    })
    public void evaluateDuplicationWithTad(int start, int dupStart, int dupEnd, int end, double expected) {
        Contig ctg1 = TestContig.of(0, 100);
        Routes routes = makeRoutes(
                GenomicRegion.of(ctg1, Strand.POSITIVE, CoordinateSystem.zeroBased(), start, end),
                positiveGap(ctg1, start, dupStart),
                positiveDuplication(ctg1, dupStart, dupEnd),
                positiveGap(ctg1, dupEnd, end));
        RouteDataGETad routeData = RouteDataGETad.builder(routes)
                .addEnhancer(TestEnhancer.of(ctg1, Strand.POSITIVE, CoordinateSystem.zeroBased(), 5, 10, "a"))
                .addGene(makeGene("NCBIGene:A", "A", ctg1, 20, 40))
                .addTadBoundary(TestTad.of("X", ctg1, Strand.POSITIVE, CoordinateSystem.zeroBased(), 45, 47))
                .addEnhancer(TestEnhancer.of(ctg1, Strand.POSITIVE, CoordinateSystem.zeroBased(), 50, 55, "b"))
                .addGene(makeGene("NCBIGene:B", "B", ctg1, 60, 70))
                .build();

        double score = evaluator.evaluate(routeData).priority();

        assertThat(score, closeTo(expected, ERROR));
    }

    @ParameterizedTest
    @CsvSource({
            "0,   10,  15,   80,               0.",        //  nothing relevant is inverted
            "0,    5,  10,   80,               0.",        //  enhancer is inverted
            "0,    7,  15,   80,               0.1",        //  enhancer is disrupted by an inversion
            "0,   15,  42,   80,               0.",        //  gene is inverted
            "0,   25,  45,   80,              27.282818",  //  gene is disrupted by an inversion

            //  enhancer adoption (0 is correct since we work with constant enhancer-gene relevance. No net loss/gain of gene/enhancer interaction)
            "0,    40,  55,   80,              0.",

            "0,   20,  50,   80,               0.",         // separate `a` from the rest, again no net loss/gain here
    })
    public void evaluateInversionWithTad(int start, int invStart, int invEnd, int end, double expected) {
        Contig ctg1 = TestContig.of(0, 100);
        Routes routes = makeRoutes(
                GenomicRegion.of(ctg1, Strand.POSITIVE, CoordinateSystem.zeroBased(), start, end),
                positiveGap(ctg1, start, invStart),
                positiveInversion(ctg1, invStart, invEnd),
                positiveGap(ctg1, invEnd, end));
        RouteDataGETad routeData = RouteDataGETad.builder(routes)
                .addEnhancer(TestEnhancer.of(ctg1, Strand.POSITIVE, CoordinateSystem.zeroBased(), 5, 10, "a"))
                .addGene(makeGene("NCBIGene:A", "A", ctg1, 20, 40))
                .addTadBoundary(TestTad.of("X", ctg1, Strand.POSITIVE, CoordinateSystem.zeroBased(), 45, 47))
                .addEnhancer(TestEnhancer.of(ctg1, Strand.POSITIVE, CoordinateSystem.zeroBased(), 50, 55, "b"))
                .addGene(makeGene("NCBIGene:B", "B", ctg1, 60, 70))
                .build();

        double score = evaluator.evaluate(routeData).priority();

        assertThat(score, closeTo(expected, ERROR));
    }

    @ParameterizedTest
    @CsvSource({
            " 5,   100,     11.",
            "10,   100,     10.",
            "70,   100,     13.",
    })
    @Disabled("Fix the route setup")
    public void evaluateBreakendWithTads(int left, int right, double expected) {
        TestContig ctg1 = TestContig.of(0, 100);
        TestContig ctg2 = TestContig.of(1, 200);
        Routes routes = makeRoutes(GenomicRegion.of(ctg1, Strand.POSITIVE, CoordinateSystem.zeroBased(), 0, 80),
                positiveGap(ctg1, 0, left),
                positiveBreakend(ctg1, left),
                positiveBreakend(ctg2, right),
                positiveGap(ctg2, right, 200)
        );
        // TODO - fix the route setup
        RouteDataGETad routeData = RouteDataGETad.builder(routes)
                .addEnhancer(TestEnhancer.of(ctg1, Strand.POSITIVE, CoordinateSystem.zeroBased(), 5, 10, "a"))
                .addGene(makeGene("NCBIGene:A", "A", ctg1, 20, 40))
                .addTadBoundary(TestTad.of("X", ctg1, Strand.POSITIVE, CoordinateSystem.zeroBased(), 45, 47))
                .addEnhancer(TestEnhancer.of(ctg1, Strand.POSITIVE, CoordinateSystem.zeroBased(), 50, 55, "b"))
                .addGene(makeGene("NCBIGene:B", "B", ctg1, 60, 70))
                .addEnhancer(TestEnhancer.of(ctg2, Strand.POSITIVE, CoordinateSystem.zeroBased(), 105, 110, "c"))
                .addGene(makeGene("NCBIGene:C", "C", ctg2, 120, 140))
                .build();

        double score = evaluator.evaluate(routeData).priority();
        assertThat(score, closeTo(expected, ERROR));
    }
}