package org.jax.svanna.core.priority.additive;

import org.jax.svanna.core.TestDataConfig;
import org.jax.svanna.core.TestGene;
import org.jax.svanna.core.service.PhenotypeDataService;
import org.jax.svanna.model.HpoDiseaseSummary;
import org.jax.svanna.model.ModeOfInheritance;
import org.jax.svanna.model.gene.Gene;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.svart.CoordinateSystem;
import org.monarchinitiative.svart.GenomicAssemblies;
import org.monarchinitiative.svart.GenomicAssembly;
import org.monarchinitiative.svart.Strand;
import org.springframework.boot.test.context.SpringBootTest;

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
        Map<TermId, Collection<TermId>> diseaseIdToTermIds = Map.of(TermId.of("OMIM:154700"),marfanSampleFeatures);
        when(phenotypeDataService.computeSimilarityScore(patientFeatures, marfanSampleFeatures))
                .thenReturn(3.5);


        TermSimilarityGeneWeightCalculator calculator = new TermSimilarityGeneWeightCalculator(phenotypeDataService, patientFeatures, diseaseIdToTermIds);

        TermId accessionId = TermId.of("NCBIGene:2200");
        Gene gene = TestGene.of(accessionId, "FBN1",
                ASSEMBLY.contigByName("9"), Strand.POSITIVE, CoordinateSystem.oneBased(),
                48_408_313, 48_645_721);

        when(phenotypeDataService.getDiseasesForGene(accessionId))
                .thenReturn(Set.of(HpoDiseaseSummary.of("OMIM:154700", "Marfan Syndrome",
                        Set.of(ModeOfInheritance.AUTOSOMAL_DOMINANT))));


        assertThat(calculator.calculateRelevance(gene), is(closeTo(3.5, ERROR)));
    }

}