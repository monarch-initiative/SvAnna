package org.jax.svanna.core.priority.additive.impact;

import org.jax.svanna.core.TestContig;
import org.jax.svanna.core.TestDataConfig;
import org.jax.svanna.core.priority.additive.*;
import org.jax.svanna.core.service.GeneDosageDataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.monarchinitiative.svart.CoordinateSystem;
import org.monarchinitiative.svart.Coordinates;
import org.monarchinitiative.svart.GenomicRegion;
import org.monarchinitiative.svart.Strand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import xyz.ielis.silent.genes.model.Gene;
import xyz.ielis.silent.genes.model.GeneIdentifier;
import xyz.ielis.silent.genes.model.Transcript;
import xyz.ielis.silent.genes.model.TranscriptIdentifier;

import java.util.List;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.junit.jupiter.api.Assertions.fail;

@SpringBootTest(classes = TestDataConfig.class)
public class GeneSequenceImpactCalculatorTest {

    private static final double ERROR = 1E-12;

    @Autowired
    public GeneDosageDataService geneDosageDataService;

    private GeneSequenceImpactCalculator instance;

    private static Gene makeGene(TestContig contig, int start, int end, int oneStart, int oneEnd, int twoStart, int twoEnd, int threeStart, int threeEnd) {
        GenomicRegion location = GenomicRegion.of(contig, Strand.POSITIVE, CoordinateSystem.zeroBased(), start, end);

        // make transcript
        TranscriptIdentifier txId = TranscriptIdentifier.of("TX1", "TX1_SYMBOL", null);
        List<Coordinates> exons = makeExons(oneStart, oneEnd, twoStart, twoEnd, threeStart, threeEnd);
        Coordinates startCodon = Coordinates.of(CoordinateSystem.zeroBased(), start + 10, start + 13);
        Coordinates stopCodon = Coordinates.of(CoordinateSystem.zeroBased(), end - 13, end - 10);
        Transcript tx = Transcript.coding(txId, location, exons, startCodon, stopCodon);

        // make gene
        GeneIdentifier gId = GeneIdentifier.of("NCBIGene:123", "A", null, null);
        return Gene.of(gId, location, Set.of(tx));
    }

    private static List<Coordinates> makeExons(int oneStart, int oneEnd, int twoStart, int twoEnd, int threeStart, int threeEnd) {
        return List.of(
                Coordinates.of(CoordinateSystem.zeroBased(), oneStart, oneEnd),
                Coordinates.of(CoordinateSystem.zeroBased(), twoStart, twoEnd),
                Coordinates.of(CoordinateSystem.zeroBased(), threeStart, threeEnd));
    }

    @BeforeEach
    public void setUp() {
        instance = new GeneSequenceImpactCalculator(geneDosageDataService, 1., 50, .6);
    }

    @ParameterizedTest
    @CsvSource({
            "100, 400,          100,120, 240,260, 380,400,           .0", // the middle exon is deleted
            "100, 400,          100,120, 325,350, 380,400,          1.",  // the middle exon is neighboring the deletion
            "100, 400,          100,120, 150,194, 380,400,          1.",  // the middle exon is neighboring the deletion
            " 50, 200,           50,100, 110,120, 180,200,          1.",  // the deletion is downstream of the gene
            "300, 400,          300,320, 330,370, 380,400,           .6",  // the deletion is upstream of the gene (promoter)
            "350, 500,          350,370, 380,420, 430,500,          1.",  // the deletion is just upstream of the promoter
    })
    public void deletion(int start, int end,
                         int oneStart, int oneEnd, int twoStart, int twoEnd, int threeStart, int threeEnd,
                         double expected) {

        TestContig ctg1 = TestContig.of(0, 1000);
        Route route = Route.of(
                List.of(
                        Segment.of(ctg1, Strand.POSITIVE, CoordinateSystem.zeroBased(), 0, 200, "upstream", Event.GAP, 1),
                        Segment.of(ctg1, Strand.POSITIVE, CoordinateSystem.zeroBased(), 200, 300, "deletion", Event.DELETION, 0),
                        Segment.of(ctg1, Strand.POSITIVE, CoordinateSystem.zeroBased(), 300, 500, "downstream", Event.GAP, 1)
                ));

        Gene gene = makeGene(ctg1, start, end, oneStart, oneEnd, twoStart, twoEnd, threeStart, threeEnd);

        List<Projection<Gene>> projections = Projections.project(gene, route);
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
                        Segment.of(ctg1, Strand.POSITIVE, CoordinateSystem.zeroBased(), 0, 200, "upstream", Event.GAP, 1),
                        Segment.of(ctg1, Strand.POSITIVE, CoordinateSystem.zeroBased(), 200, 300, "inversion", Event.INVERSION, 1),
                        Segment.of(ctg1, Strand.POSITIVE, CoordinateSystem.zeroBased(), 300, 500, "downstream", Event.GAP, 1)
                ));
        Gene gene = makeGene(ctg1, start, end, oneStart, oneEnd, twoStart, twoEnd, threeStart, threeEnd);

        List<Projection<Gene>> projections = Projections.project(gene, route);
        if (projections.isEmpty()) fail();


        assertThat(instance.projectImpact(projections.get(0)), closeTo(expected, ERROR));
    }

    @ParameterizedTest
    @CsvSource({
            "187, 400,          187,220, 250,270, 380,400,  3,           .8", // in frame insertion that does not disrupt the reading frame
            "187, 400,          187,220, 250,270, 380,400,  5,           .5", // in frame insertion that disrupts the reading frame
            "186, 400,          186,220, 250,270, 380,400,  3,           .1", // out of frame insertion
            "195, 400,          195,220, 250,270, 380,400,  3,           .7", // 5'UTR insertion
            "10,  205,           10, 30, 150,170, 190,205,  3,           .769230769231", // 3'UTR insertion (crazy number since TER is part of 3'UTR)
    })
    public void insertion(int start, int end,
                          int oneStart, int oneEnd, int twoStart, int twoEnd, int threeStart, int threeEnd,
                          int insertionLength,
                          double expected) {

        TestContig ctg1 = TestContig.of(0, 1000);
        Route route = Route.of(
                List.of(
                        Segment.of(ctg1, Strand.POSITIVE, CoordinateSystem.zeroBased(), 0, 200, "upstream", Event.GAP, 1),
                        Segment.insertion(ctg1, Strand.POSITIVE, CoordinateSystem.zeroBased(), 200, 200, "insertion", insertionLength),
                        Segment.of(ctg1, Strand.POSITIVE, CoordinateSystem.zeroBased(), 200, 400, "downstream", Event.GAP, 1)
                ));

        Gene gene = makeGene(ctg1, start, end, oneStart, oneEnd, twoStart, twoEnd, threeStart, threeEnd);

        List<Projection<Gene>> projections = Projections.project(gene, route);
        if (projections.isEmpty()) fail();


        double impact = instance.projectImpact(projections.get(0));
        assertThat(impact, closeTo(expected, ERROR));
    }

}