package org.jax.svann.priority;

import de.charite.compbio.jannovar.impl.intervals.IntervalArray;
import org.jax.svann.TestBase;
import org.jax.svann.genomicreg.Enhancer;
import org.jax.svann.hpo.HpoDiseaseSummary;
import org.jax.svann.parse.TestVariants;
import org.jax.svann.reference.SequenceRearrangement;
import org.jax.svann.reference.genome.Contig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoAnnotation;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoOnset;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Let's assume we're running prioritization for a patient with a causal SV in SURF1 gene. Variants in SURF1 lead to
 * Leigh syndrome (OMIM:256000)- a progressive neurological disease defined by specific neuropathological features
 * associating brainstem and basal ganglia lesions.
 * <p>
 * Thus, the patient might have terms such as HP:0002490 (Increased CSF lactate), and HP:0001290 (Generalized hypotonia).
 */
public class PrototypeSvPrioritizerTest extends TestBase {

    /**
     * Assuming the patient has variant in SURF1, increased CSV lactate and generalized hypotonia, these are the
     * sensible relevant enhancer top level terms.
     */
    private static final Set<TermId> RELEVANT_ENHANCER_TOP_LEVEL_TERMS = Set.of(
            TermId.of("HP:0000707"), // Abnormality of the nervous system, based on increased CSF lactate
            TermId.of("HP:0001507") // Abnormality of the musculoskeletal system, based on hypotonia
    );


    private static final Map<TermId, Set<HpoDiseaseSummary>> DISEASE_MAP = makeDiseaseSummaryMap();
    private static final Map<Integer, IntervalArray<Enhancer>> ENHANCER_MAP = makeEnhancerMap();
    private PrototypeSvPrioritizer prioritizer;


    /**
     * Prepare an interval array map with a enhancers for <em>SURF1</em> and <em>SURF2</em> (chr9) and <em>GCK</em>
     * (chr7).
     * <p>
     * The interval array contains a single enhancer per gene:
     * <ul>
     *     <li><em>SURF[12]</em> - region chr9:133,356,501-133,356,530</li>
     *     <li><em>GCK</em> - region chr7:44,190,001-44,190,050</li>
     * <p>
     * Both enhancers have fictional tau=0.8 and TermId `HP:0001939` (Abnormality of metabolism/homeostasis)
     */
    private static Map<Integer, IntervalArray<Enhancer>> makeEnhancerMap() {
        Contig chr9 = GENOME_ASSEMBLY.getContigByName("9").orElseThrow();
        Contig chr7 = GENOME_ASSEMBLY.getContigByName("7").orElseThrow();

        Enhancer surf1Enhancer = new Enhancer(chr9, 133_356_501, 133_356_530, .8, TermId.of("HP:0001939"));
        Enhancer gckEnhancer = new Enhancer(chr7, 44_190_001, 44_190_050, .8, TermId.of("HP:0001939"));

        IntervalArray<Enhancer> chr7Array = new IntervalArray<>(List.of(gckEnhancer), new EnhancerEndExtractor());
        IntervalArray<Enhancer> chr9Array = new IntervalArray<>(List.of(surf1Enhancer), new EnhancerEndExtractor());
        return Map.of(7, chr7Array, 9, chr9Array);
    }

    private static Map<TermId, Set<HpoDiseaseSummary>> makeDiseaseSummaryMap() {
        HpoDiseaseSummary leighSyndrome = new HpoDiseaseSummary(
                new HpoDisease("Leigh syndrome",
                        TermId.of("OMIM:256000"),
                        List.of(HpoAnnotation.builder(TermId.of("HP:0001290")) // generalized hypotonia
                                        .frequency(1., "Obligate")
                                        .onset(HpoOnset.INFANTILE_ONSET)
                                        .build(),
                                HpoAnnotation.builder(TermId.of("HP:0002490")) // Increased CSF lactate
                                        .frequency(.9, "Very frequent")
                                        .onset(HpoOnset.CONGENITAL_ONSET)
                                        .build()),
                        List.of(
                                TermId.of("HP:0000007"), // Autosomal recessive inheritance
                                TermId.of("HP:0001417") // X-linked inheritance
                        ),
                        List.of(), // not terms
                        List.of(), // clinical modifiers
                        List.of() // clinical courses
                ));

        return Map.of(TermId.of("ENTREZ:6834"), Set.of(leighSyndrome));
    }

    @BeforeEach
    public void setUp() {
        prioritizer = new PrototypeSvPrioritizer(GENOME_ASSEMBLY, ENHANCER_MAP, Map.of(), JANNOVAR_DATA);
    }

    @Test
    public void prioritize_singleExonDeletion_SURF1_exon2() {
        SequenceRearrangement sr = TestVariants.singleExonDeletion_SURF1_exon2();
        SvPriority result = prioritizer.prioritize(sr);
        assertThat(result.getImpact(), is(SvImpact.HIGH_IMPACT));
    }

    @Test
    public void prioritize_twoExonDeletion_SURF1_exons_6_and_7() {
        SequenceRearrangement sr = TestVariants.twoExonDeletion_SURF1_exons_6_and_7();
        SvPriority result = prioritizer.prioritize(sr);
        System.err.println(result);
    }

    @Test
    public void prioritize_upstreamDeletion_GCK_inEnhancer() {
        SequenceRearrangement sr = TestVariants.deletionGCKUpstreamIntergenic_affectingEnhancer();
        SvPriority result = prioritizer.prioritize(sr);
        System.err.println(result);
    }

    @Test
    public void prioritize_upstreamDeletion_GCK_notInEnhancer() {
        SequenceRearrangement sr = TestVariants.deletionGCKUpstreamIntergenic_NotAffectingEnhancer();
        SvPriority result = prioritizer.prioritize(sr);
        System.err.println(result);
    }
}