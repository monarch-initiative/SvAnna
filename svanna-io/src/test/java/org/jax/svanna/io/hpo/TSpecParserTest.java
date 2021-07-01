package org.jax.svanna.io.hpo;


import org.jax.svanna.core.reference.SomeEnhancer;
import org.jax.svanna.io.TestDataConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.svart.ConfidenceInterval;
import org.monarchinitiative.svart.Contig;
import org.monarchinitiative.svart.GenomicAssembly;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * This class uses the example file {@code src/test/resources/tspec-small.tsv} to test
 * the {@link TSpecParser} and the {@link SomeEnhancer} classes. The file contains information about
 * ten enhancers.
 */
@SpringBootTest(classes = TestDataConfig.class)
public class TSpecParserTest {
    private static final Path EXAMPLE_TSPEC = Paths.get("src/test/resources/tspec-small.tsv");
    private static final double EPSILON = 5E-7;

    @Autowired
    public GenomicAssembly genomicAssembly;

    private TSpecParser instance;

    @BeforeEach
    public void setUp() {
        instance = new TSpecParser(EXAMPLE_TSPEC, genomicAssembly);
    }

    /**
     * There are a total of 11 difference enhancers in 7 tissues.
     * id2enhancerMap contains lists of enhancers, we add up their sizes
     */
    @Test
    public void if_eleven_enhancers_retrieved_then_ok() {
        Map<TermId, List<SomeEnhancer>> id2enhancerMap = instance.getId2enhancerMap();

        int actual = id2enhancerMap.values().stream().mapToInt(List::size).sum();
        assertEquals(11, actual);
    }

    @Test
    public void if_seven_tissues_retrieved_then_ok() {
        Map<TermId, List<SomeEnhancer>> id2enhancerMap = instance.getId2enhancerMap();
        assertEquals(7, id2enhancerMap.size());
    }

    /**
     * hpoIdToLabelMap is a convenience variable that returns labels for HPO ids used for the
     * enhancer map. It should have seven entries. We test two of them that they have the right label
     */
    @Test
    public void testHpoLabels() {
        Map<TermId, String> id2labelMap = instance.getId2labelMap();

        assertEquals(7, id2labelMap.size());
        TermId brain = TermId.of("UBERON:0000955");
        assertEquals("brain", id2labelMap.get(brain));
        TermId vSMC = TermId.of("CL:0000359");
        assertEquals("vascular associated smooth muscle cell", id2labelMap.get(vSMC));
    }

    /**
     * There is only one thymus enhancer,
     * chr10	100014348	100014634	0.728857	HP:0000777	Abnormality of the thymus
     */
    @Test
    public void testThymusEnhancer() {
        Map<TermId, List<SomeEnhancer>> id2enhancerMap = instance.getId2enhancerMap();
        TermId thymusHpoId = TermId.of("HP:0000777");
        TermId thymus = TermId.of("UBERON:0002370");
        List<SomeEnhancer> enhancers = id2enhancerMap.get(thymus);
        assertEquals(1, enhancers.size());
        SomeEnhancer thymusEnhancer = enhancers.get(0);
        assertNotNull(thymusEnhancer);
        Contig chr10 =  genomicAssembly.contigByName("chr10");
        assertEquals(chr10, thymusEnhancer.contig());
        assertEquals(100014348, thymusEnhancer.start());
        assertEquals(100014634, thymusEnhancer.end());
        assertEquals(thymusHpoId, thymusEnhancer.maxTauHpoTermId());
        assertEquals(0.708151, thymusEnhancer.maxTau(), EPSILON);
        // both the start and end are precise, i.e., the confidence interval is +/- 0
        assertEquals(ConfidenceInterval.precise(), thymusEnhancer.startPosition().confidenceInterval());
        assertEquals(ConfidenceInterval.precise(), thymusEnhancer.endPosition().confidenceInterval());
    }

    @Test
    public void if_five_brain_enhancers_retrieved_then_ok() {
        Map<TermId, List<SomeEnhancer>> id2enhancerMap = instance.getId2enhancerMap();
        TermId brainHpd = TermId.of("HP:0012443"); // 	Abnormality of brain morphology
        TermId brain = TermId.of("UBERON:0000955");
        List<SomeEnhancer> enhancers = id2enhancerMap.get(brain);
        assertEquals(5, enhancers.size());
    }


}
