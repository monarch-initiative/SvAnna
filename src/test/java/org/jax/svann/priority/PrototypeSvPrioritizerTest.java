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
 * Let's assume we're running prioritization for a patient with a causal SV in GCK gene (glucokinase -- hexokinase
 * specific pancreatic beta-cells). Variants in GCK lead to MODY2 - a mild form of non-insulin dependent diabetes mellitus
 * manifesting with a mild fasting hyperglycemia.
 * <p>
 * Thus, the patient might have terms such as HP:0003074 (Hyperglycemia), and HP:0025502 (Overweight).
 */
public class PrototypeSvPrioritizerTest extends TestBase {

    /**
     * Assuming the patient has variant in GCK, hyperglycemia and overweight, these are the sensible relevant enhancer
     * top level terms.
     */
    private static final Set<TermId> RELEVANT_ENHANCER_TOP_LEVEL_TERMS = Set.of(
            TermId.of("HP:0001939"), // Abnormality of metabolism/homeostasis, based on Hyperglycemia
            TermId.of("HP:HP:0001507") // Growth abnormality, based on Overweight
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
     * Prepare an interval array map with a enhancers for <em>SURF1</em> and <em>SURF2</em> (chr9) and <em>GCK</em>
     * (chr7).
     * <p>
     * The interval array contains a single enhancer per gene:
     * <ul>
     *     <li><em>SURF[12]</em> - region chr9:133,356,500-133,356,550</li>
     *     <li><em>GCK</em> - region chr7:44,190,001-44,190,050</li>
     * <p>
     * Both enhancers have fictional tau=0.8 and TermId `HP:0001939` (Abnormality of metabolism/homeostasis)
     */
    private static Map<Integer, IntervalArray<Enhancer>> makeEnhancerMap() {
        Contig chr9 = GENOME_ASSEMBLY.getContigByName("9").orElseThrow();
        Contig chr7 = GENOME_ASSEMBLY.getContigByName("7").orElseThrow();

        Enhancer surf1Enhancer = new Enhancer(chr9, 133_356_500, 133_356_550, .8, TermId.of("HP:0001939"));
        Enhancer gckEnhancer = new Enhancer(chr7, 44_190_001, 44_190_050, .8, TermId.of("HP:0001939"));

        IntervalArray<Enhancer> chr7Array = new IntervalArray<>(List.of(gckEnhancer), new EnhancerEndExtractor());
        IntervalArray<Enhancer> chr9Array = new IntervalArray<>(List.of(surf1Enhancer), new EnhancerEndExtractor());
        return Map.of(7, chr7Array, 9, chr9Array);
    }

    @BeforeEach
    public void setUp() {
        prioritizer = new PrototypeSvPrioritizer(GENOME_ASSEMBLY, RELEVANT_ENHANCER_TOP_LEVEL_TERMS, ENHANCER_MAP, DISEASE_MAP, JANNOVAR_DATA);
    }

    @Test
    public void prioritize_twoExonDeletion_SURF1_exons_6_and_7() {
        SequenceRearrangement se = TestVariants.twoExonDeletion_SURF1_exons_6_and_7();
        PrioritizedSv result = prioritizer.prioritize(se);
        System.err.println(result);
    }

    @Test
    public void prioritize_upstreamDeletion_GCK_inEnhancer() {
        SequenceRearrangement se = TestVariants.deletionGCKUpstreamIntergenic_affectingEnhancer();
        PrioritizedSv result = prioritizer.prioritize(se);
        System.err.println(result);
    }

    @Test
    public void prioritize_upstreamDeletion_GCK_notInEnhancer() {
        SequenceRearrangement se = TestVariants.deletionGCKUpstreamIntergenic_NotAffectingEnhancer();
        PrioritizedSv result = prioritizer.prioritize(se);
        System.err.println(result);
    }
}