package org.jax.svanna.core.priority.additive;

import org.jax.svanna.core.TestDataConfig;
import org.jax.svanna.core.hpo.SimilarityScoreCalculator;
import org.jax.svanna.core.service.PhenotypeDataService;
import org.jax.svanna.model.HpoDiseaseSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.sgenes.model.Gene;
import org.monarchinitiative.sgenes.model.GeneIdentifier;
import org.monarchinitiative.sgenes.model.Transcript;
import org.monarchinitiative.sgenes.model.TranscriptIdentifier;
import org.monarchinitiative.svart.*;
import org.monarchinitiative.svart.assembly.GenomicAssemblies;
import org.monarchinitiative.svart.assembly.GenomicAssembly;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = TestDataConfig.class)
public class TermSimilarityGeneWeightCalculatorTest {

    private static final GenomicAssembly ASSEMBLY = GenomicAssemblies.GRCh38p13();
    private static final double ERROR = 1e-12;

    @Mock
    public PhenotypeDataService phenotypeDataService;

    @Mock
    public SimilarityScoreCalculator similarityScoreCalculator;


    private static List<TermId> configurePatientFeatures() {
        TermId arachnodactyly = TermId.of("HP:0001166");
        TermId dolichocephaly = TermId.of("HP:0000268");
        TermId ectopiaLentis = TermId.of("HP:0001083");
        TermId aorticDissection = TermId.of("HP:0002647");
        TermId striaeDistensae = TermId.of("HP:0001065");
        return List.of(arachnodactyly, dolichocephaly, ectopiaLentis, aorticDissection, striaeDistensae);
    }

    @BeforeEach
    public void setUp() {

    }

    @Test
    public void calculateRelevance() {
        List<TermId> patientFeatures = configurePatientFeatures();
        List<TermId> marfanSampleFeatures = List.of(TermId.of("HP:0002143"), // Abnormality of the spinal cord
                TermId.of("HP:0100491"), // Abnormality of lower limb joint
                TermId.of("HP:0000518"), // Cataract
                TermId.of("HP:0008132"), // Medial rotation of the medial malleolus
                TermId.of("HP:0000517")  // Abnormality of the lens
        );
        when(similarityScoreCalculator.computeSimilarityScore(patientFeatures, marfanSampleFeatures))
                .thenReturn(3.5);


        TermSimilarityGeneWeightCalculator calculator = new TermSimilarityGeneWeightCalculator(phenotypeDataService, similarityScoreCalculator, patientFeatures);

        String geneAccessionId = "NCBIGene:2200";
        String hgncId = "HGNC:3603";
        GeneIdentifier id = GeneIdentifier.of(geneAccessionId, "FBN1", hgncId, geneAccessionId);
        GenomicRegion location = GenomicRegion.of(ASSEMBLY.contigByName("9"), Strand.POSITIVE, CoordinateSystem.oneBased(), 48_408_313, 48_645_721);
        TranscriptIdentifier txId = TranscriptIdentifier.of("TX_ACCESSION", "FBN1", null);
        List<Coordinates> exons = List.of(Coordinates.of(CoordinateSystem.oneBased(), 48_408_313, 48_645_721));
        Coordinates cdsCoordinates = Coordinates.of(CoordinateSystem.oneBased(), 48_408_313, 48_645_721);
        List<Transcript> transcripts = List.of(Transcript.of(txId, location, exons, cdsCoordinates));
        Gene gene = Gene.of(id, location, transcripts);


        when(phenotypeDataService.getDiseasesForGene(hgncId))
                .thenReturn(List.of(HpoDiseaseSummary.of("OMIM:154700", "Marfan Syndrome")));
        when(phenotypeDataService.phenotypicAbnormalitiesForDiseaseId("OMIM:154700"))
                .thenReturn(marfanSampleFeatures);


        assertThat(calculator.calculateRelevance(gene), is(closeTo(3.5, ERROR)));
    }

}