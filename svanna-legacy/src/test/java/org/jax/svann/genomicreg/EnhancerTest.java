package org.jax.svann.genomicreg;

import de.charite.compbio.jannovar.impl.intervals.IntervalArray;
import org.jax.svann.reference.genome.Contig;
import org.jax.svann.reference.genome.GenomeAssembly;
import org.jax.svann.reference.genome.GenomeAssemblyProvider;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class EnhancerTest {
    private static final Path EXAMPLE_TSPEC = Paths.get("src/test/resources/tspec-small.tsv");
    private static final TSpecParser parser = new TSpecParser(EXAMPLE_TSPEC.toAbsolutePath().toString());
//    private static final Map<TermId, String> hpoIdToLabelMap = parser.getId2labelMap();
//    private static final Map<TermId, List<Enhancer>> id2enhancerMap = parser.getId2enhancerMap();
    private static final Map<Integer, IntervalArray<Enhancer>> iamap = parser.getChromosomeToEnhancerIntervalArrayMap();
    private static final GenomeAssembly assembly = GenomeAssemblyProvider.getGrch38Assembly();
    private static final double EPSILON = 0.000_001;


    /**
     * Test that we retrieve an interval array for two chromosomes
     * (our test file only has one chromosome)
     */
    @Test
    public void testConstructIntervalTree() {
        assertEquals(2, iamap.size());
    }

    /**
     * Search for this enhancer
     * chr10	100006233	100006603	0.343407	CL:0000071	blood vessel endothelial cell	HP:0002597	Abnormality of the vasculature
     */
    @Test
    public void testEnhancer1() {
        Contig chrom10 = assembly.getContigById(10).orElseThrow();
        double tau = 0.343407;
        String tissueLabel = "blood vessel endothelial cell"; // represents an UBERON or CL term label
        Enhancer e = new Enhancer(chrom10, 100006233, 100006603, tau, TermId.of("HP:0002597"), tissueLabel);
        IntervalArray<Enhancer> iarray = iamap.get(10);
        assertNotNull(iarray);
        IntervalArray<Enhancer>.QueryResult qresult = iarray.findOverlappingWithInterval(100006350, 100006400);
        List<Enhancer> enhancers = qresult.getEntries();
        assertEquals(1, enhancers.size());
        Enhancer retrieved = enhancers.get(0);
        assertEquals(e, retrieved);
    }



    /**
     * Search for this enhancer
     * chr10	100006233	100006603	0.343407	CL:0000071	blood vessel endothelial cell	HP:0002597	Abnormality of the vasculature
     */
    @Test
    public void testEnhancerPartialOverlaps() {
        Contig chrom10 = assembly.getContigById(10).orElseThrow();
        double tau = 0.343407;
        String tissueLabel = "blood vessel endothelial cell"; // represents an UBERON or CL term label
        Enhancer e = new Enhancer(chrom10, 100006233, 100006603, tau, TermId.of("HP:0002597"), tissueLabel);
        IntervalArray<Enhancer> iarray = iamap.get(10);
        assertNotNull(iarray);
        IntervalArray<Enhancer>.QueryResult qresult = iarray.findOverlappingWithInterval(100006600, 100006607);
        List<Enhancer> enhancers = qresult.getEntries();
        assertEquals(1, enhancers.size());
        Enhancer retrieved = enhancers.get(0);
        assertEquals(e, retrieved);
        // now try overlap on other end
        qresult = iarray.findOverlappingWithInterval(100006200, 100006250);
        enhancers = qresult.getEntries();
        assertEquals(1, enhancers.size());
        retrieved = enhancers.get(0);
        assertEquals(e, retrieved);
    }


    @Test
    public void testNonOverlappingInterval() {
        IntervalArray<Enhancer> iarray = iamap.get(10);
        IntervalArray<Enhancer>.QueryResult qresult = iarray.findOverlappingWithInterval(1, 4);
        List<Enhancer> enhancers = qresult.getEntries();
        assertEquals(0, enhancers.size());
    }





}
