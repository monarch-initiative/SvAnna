package org.jax.svann.priority;

import de.charite.compbio.jannovar.impl.intervals.IntervalArray;
import org.jax.svann.TestBase;
import org.jax.svann.genomicreg.Enhancer;
import org.jax.svann.hpo.HpoDiseaseSummary;
import org.jax.svann.parse.TestVariants;
import org.jax.svann.reference.SequenceRearrangement;
import org.jax.svann.reference.genome.Contig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public class PrototypeSvPrioritizerTest extends TestBase {

    private static final Set<TermId> PATIENT_TERM_IDS = Set.of(
            TermId.of("HP:0003128"), // Lactic acidosis
            TermId.of("HP:0001266") // Choreoathetosis
    );

    // TODO(PNR): 10/29/20 complete
    private static final Map<TermId, Set<HpoDiseaseSummary>> DISEASE_MAP = Map.of();
    private static Map<Integer, IntervalArray<Enhancer>> ENHANCER_MAP = makeEnhancerMap();
    private PrototypeSvPrioritizer prioritizer;

    @BeforeAll
    public static void beforeAll() {
        ENHANCER_MAP = makeEnhancerMap();
    }

    /**
     * Prepare an interval array map with a single enhancer interval array for chromosome 9, where SURF1 and SURF2
     * are located.
     *
     * The interval array contains a single enhancer spanning the region chr9:133,356,500-133,356,550, which is
     * a region upstream from SURF1 5'UTR, thus a realistic region.
     *
     * The enhancer has fictional tau=0.8 and TermId `HP:0001939` (Abnormality of metabolism/homeostasis)
     *
     */
    private static Map<Integer, IntervalArray<Enhancer>> makeEnhancerMap() {
        Contig chr9 = GENOME_ASSEMBLY.getContigByName("9").orElseThrow();

        Enhancer enhancer = new Enhancer(chr9, 133_356_500, 133_356_550, .8, TermId.of("HP:0001939"));

        List<Enhancer> enhancers = List.of(enhancer);
        IntervalArray<Enhancer> eArray = new IntervalArray<>(enhancers, new EnhancerEndExtractor());
        return Map.of(9, eArray);
    }

    @BeforeEach
    public void setUp() {
        prioritizer = new PrototypeSvPrioritizer(GENOME_ASSEMBLY, PATIENT_TERM_IDS, ENHANCER_MAP, DISEASE_MAP, JANNOVAR_DATA);
    }

    @Test
    public void prioritize_twoExonDeletion_SURF1_exons_6_and_7() {
        SequenceRearrangement se = TestVariants.twoExonDeletion_SURF1_exons_6_and_7();
        PrioritizedSv result = prioritizer.prioritize(se);
        System.err.println(result);
    }

}