package org.jax.svanna.core.priority.additive;

import org.jax.svanna.core.TestDataConfig;
import org.jax.svanna.core.service.PhenotypeDataService;
import org.jax.svanna.model.HpoDiseaseSummary;
import org.jax.svanna.model.ModeOfInheritance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.svart.*;
import org.springframework.boot.test.context.SpringBootTest;
import xyz.ielis.silent.genes.model.Gene;
import xyz.ielis.silent.genes.model.GeneIdentifier;
import xyz.ielis.silent.genes.model.Transcript;
import xyz.ielis.silent.genes.model.TranscriptIdentifier;

import java.util.Collection;
import java.util.List;
import java.util.Map;
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
    private PhenotypeDataService phenotypeDataService;


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
        Map<TermId, Collection<TermId>> diseaseIdToTermIds = Map.of(TermId.of("OMIM:154700"), marfanSampleFeatures);
        when(phenotypeDataService.computeSimilarityScore(patientFeatures, marfanSampleFeatures))
                .thenReturn(3.5);


        TermSimilarityGeneWeightCalculator calculator = new TermSimilarityGeneWeightCalculator(phenotypeDataService, patientFeatures, diseaseIdToTermIds);

        String geneAccessionId = "NCBIGene:2200";
        GeneIdentifier id = GeneIdentifier.of(geneAccessionId, "FBN1", "HGNC:3603", "NCBIGene:2200");
        GenomicRegion location = GenomicRegion.of(ASSEMBLY.contigByName("9"), Strand.POSITIVE, CoordinateSystem.oneBased(), 48_408_313, 48_645_721);
        TranscriptIdentifier txId = TranscriptIdentifier.of("TX_ACCESSION", "FBN1", null);
        List<Coordinates> exons = List.of(Coordinates.of(CoordinateSystem.oneBased(), 48_408_313, 48_645_721));
        Coordinates startCodong = Coordinates.of(CoordinateSystem.oneBased(), 48_408_313, 48_408_315);
        Coordinates stopCodon  = Coordinates.of(CoordinateSystem.oneBased(), 48_645_719, 48_645_721);
        Set<Transcript> transcripts = Set.of(Transcript.coding(txId, location, exons, startCodong, stopCodon));
        Gene gene = Gene.of(id, location, transcripts);


        when(phenotypeDataService.getDiseasesForGene(geneAccessionId))
                .thenReturn(Set.of(HpoDiseaseSummary.of("OMIM:154700", "Marfan Syndrome",
                        Set.of(ModeOfInheritance.AUTOSOMAL_DOMINANT))));


        assertThat(calculator.calculateRelevance(gene), is(closeTo(3.5, ERROR)));
    }

}