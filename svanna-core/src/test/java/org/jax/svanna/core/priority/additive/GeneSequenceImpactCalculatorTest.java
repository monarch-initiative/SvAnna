package org.jax.svanna.core.priority.additive;

import org.jax.svanna.core.TestContig;
import org.jax.svanna.core.TestTranscript;
import org.jax.svanna.core.reference.Gene;
import org.jax.svanna.core.reference.GeneDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.svart.CoordinateSystem;
import org.monarchinitiative.svart.Position;
import org.monarchinitiative.svart.Strand;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.junit.jupiter.api.Assertions.fail;

public class GeneSequenceImpactCalculatorTest {

    private static final double ERROR = 1E-15;

    private GeneSequenceImpactCalculator instance;

    @BeforeEach
    public void setUp() {
        instance = new GeneSequenceImpactCalculator(1.);
    }

    @ParameterizedTest
    @CsvSource({
            "100, 400,          100,120, 240,260, 380,400,           .1", // the middle exon is deleted
            "100, 400,          100,120, 325,350, 380,400,          1.",  // the middle exon is neighboring the deletion
            "100, 400,          100,120, 150,194, 380,400,          1.",  // the middle exon is neighboring the deletion
//            " 50, 200,           50,100, 110,120, 180,200,          1.",  // the deletion is downstream of the gene
//            "300, 400,          300,320, 330,370, 380,400,          1.",  // the deletion is upstream of the gene
    })
    public void deletion(int start, int end,
                         int oneStart, int oneEnd, int twoStart, int twoEnd, int threeStart, int threeEnd,
                         double expected) {

        TestContig ctg1 = TestContig.of(0, 1000);
        Route route = Route.of(
                List.of(
                        Segment.of(ctg1, Strand.POSITIVE, CoordinateSystem.zeroBased(), Position.of(0), Position.of(200), "upstream", Event.GAP, 1),
                        Segment.of(ctg1, Strand.POSITIVE, CoordinateSystem.zeroBased(), Position.of(200), Position.of(300), "deletion", Event.DELETION, 0),
                        Segment.of(ctg1, Strand.POSITIVE, CoordinateSystem.zeroBased(), Position.of(300), Position.of(500), "downstream", Event.GAP, 1)
                ));

        Gene gene = GeneDefault.builder()
                .geneName("A")
                .accessionId(TermId.of("NCBIGene:123"))
                .addTranscript(TestTranscript.of(ctg1, Strand.POSITIVE, start, end, List.of(oneStart, oneEnd, twoStart, twoEnd, threeStart, threeEnd)))
                .build();

        List<Projection<Gene>> projections = Projections.projectAll(gene, route);
        if (projections.isEmpty()) fail();


        assertThat(instance.projectImpact(projections.get(0)), closeTo(expected, ERROR));
    }


    @ParameterizedTest
    @CsvSource({
            "100, 400,          100,120, 240,260, 380,400,           .0", // the middle exon is inverted
            "100, 400,          100,120, 140,180, 380,400,           1.", // intronic inversion
    })
    public void inversion(int start, int end,
                          int oneStart, int oneEnd, int twoStart, int twoEnd, int threeStart, int threeEnd,
                          double expected) {

        TestContig ctg1 = TestContig.of(0, 1000);
        Route route = Route.of(
                List.of(
                        Segment.of(ctg1, Strand.POSITIVE, CoordinateSystem.zeroBased(), Position.of(0), Position.of(200), "upstream", Event.GAP, 1),
                        Segment.of(ctg1, Strand.POSITIVE, CoordinateSystem.zeroBased(), Position.of(200), Position.of(300), "deletion", Event.INVERSION, 1),
                        Segment.of(ctg1, Strand.POSITIVE, CoordinateSystem.zeroBased(), Position.of(300), Position.of(500), "downstream", Event.GAP, 1)
                ));

        Gene gene = GeneDefault.builder()
                .geneName("A")
                .accessionId(TermId.of("NCBIGene:123"))
                .addTranscript(TestTranscript.of(ctg1, Strand.POSITIVE, start, end, List.of(oneStart, oneEnd, twoStart, twoEnd, threeStart, threeEnd)))
                .build();

        List<Projection<Gene>> projections = Projections.projectAll(gene, route);
        if (projections.isEmpty()) fail();


        assertThat(instance.projectImpact(projections.get(0)), closeTo(expected, ERROR));
    }

}