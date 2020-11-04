package org.jax.svann.genomicreg;


import org.jax.svann.reference.ConfidenceInterval;
import org.jax.svann.reference.genome.Contig;
import org.jax.svann.reference.genome.GenomeAssembly;
import org.jax.svann.reference.genome.GenomeAssemblyProvider;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * This class uses the example file {@code src/test/resources/tspec-small.tsv} to test
 * the {@link TSpecParser} and the {@link Enhancer} classes. The file contains information about
 * ten enhancers.
 */
public class TSpecParserTest {
    static Path EXAMPLE_TSPEC = Paths.get("src/test/resources/tspec-small.tsv");
    private static final TSpecParser parser = new TSpecParser(EXAMPLE_TSPEC.toAbsolutePath().toString());
    private static final Map<TermId, String> hpoIdToLabelMap = parser.getId2labelMap();
    private static final Map<TermId, List<Enhancer>> id2enhancerMap = parser.getId2enhancerMap();
    private static final GenomeAssembly assembly = GenomeAssemblyProvider.getGrch38Assembly();
    private static final double EPSILON = 0.000_001;

    /**
     * There are a total of ten difference enhancers in 7 tissues.
     * id2enhancerMap contains lists of enhancers, we add up their sizes
     */
    @Test
    public void if_ten_enhancers_retrieved_then_ok() {
        int N = 0;
        for (var e : id2enhancerMap.values()) {
            N += e.size();
        }
        assertEquals(10, N);
    }

    @Test
    public void if_seven_tissues_retrieved_then_ok() {
        assertEquals(7, id2enhancerMap.size());
    }

    /**
     * hpoIdToLabelMap is a convenience variable that returns labels for HPO ids used for the
     * enhancer map. It should have seven entries. We test two of them that they have the right label
     */
    @Test
    public void testHpoLabels() {
        assertEquals(7, hpoIdToLabelMap.size());
        TermId vascular = TermId.of("HP:0025015");
        assertEquals("Abnormal vascular morphology", hpoIdToLabelMap.get(vascular));
        TermId heart = TermId.of("HP:0001627");
        assertEquals("Abnormal heart morphology", hpoIdToLabelMap.get(heart));
    }

    /**
     * There is only one thymus enhancer,
     * chr10	100014348	100014634	0.728857	HP:0000777	Abnormality of the thymus
     */
    @Test
    public void testThymusEnhancer() {
        TermId thymusHpoId = TermId.of("HP:0000777");
        List<Enhancer> enhancers = id2enhancerMap.get(thymusHpoId);
        assertEquals(1, enhancers.size());
        Enhancer thymusEnhancer = enhancers.get(0);
        assertNotNull(thymusEnhancer);
        Optional<Contig> chr10opt = assembly.getContigByName("chr10");
        assertTrue(chr10opt.isPresent());
        Contig chr10 = chr10opt.get();
        assertEquals(chr10, thymusEnhancer.getContig());
        assertEquals(100014348, thymusEnhancer.getStart().getPosition());
        assertEquals(100014634, thymusEnhancer.getEnd().getPosition());
        assertEquals(thymusHpoId, thymusEnhancer.getTermId());
        assertEquals(0.728857, thymusEnhancer.getTau(), EPSILON);
        // both the start and end are precise, i.e., the confidence interval is +/- 0
        assertEquals(ConfidenceInterval.precise(), thymusEnhancer.getStart().getCi());
        assertEquals(ConfidenceInterval.precise(), thymusEnhancer.getEnd().getCi());
    }

    @Test
    public void if_three_brain_enhancers_retrieved_then_ok() {
        TermId brainHpd = TermId.of("HP:0012443"); // 	Abnormality of brain morphology
        List<Enhancer> enhancers = id2enhancerMap.get(brainHpd);
        assertEquals(3, enhancers.size());
    }


}
