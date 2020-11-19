package org.jax.svann.priority;

import de.charite.compbio.jannovar.impl.intervals.IntervalArray;
import org.jax.svann.TestBase;
import org.jax.svann.genomicreg.Enhancer;
import org.jax.svann.hpo.GeneWithId;
import org.jax.svann.hpo.HpoDiseaseSummary;
import org.jax.svann.overlap.EnhancerOverlapper;
import org.jax.svann.overlap.Overlapper;
import org.jax.svann.overlap.SvAnnOverlapper;
import org.jax.svann.parse.TestVariants.Deletions;
import org.jax.svann.parse.TestVariants.Insertions;
import org.jax.svann.parse.TestVariants.Inversions;
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
 * Let's assume we're running prioritization for a patient with a causal SV in <em>GCK</em> gene. Variants in GCK lead to
 * Maturity Onset Diabetes of the Young type 2 (MODY2, OMIM:125851) which is a form of NIDDM (OMIM:125853) characterized
 * by monogenic autosomal dominant transmission and early age of onset.
 * <p>
 * Thus, the patient might have terms such as HP:0003074 (Hyperglycemia), and HP:0001508 (Failure to thrive).
 */
public class PrototypeSvPrioritizerTest extends TestBase {

    /**
     * Assuming the patient has variant in GCK, hyperglycemia and failure to thrive, these are the
     * sensible relevant enhancer top level terms.
     */
    private static final Set<TermId> RELEVANT_ENHANCER_TOP_LEVEL_TERMS = Set.of(
            TermId.of("HP:0001507"), // Growth abnormality, based on failure to thrive
            TermId.of("HP:0001939") // Abnormality of metabolism/homeostasis, based on hyperglycemia
    );


    private static final Map<TermId, Set<HpoDiseaseSummary>> DISEASE_MAP = makeDiseaseSummaryMap();
    private static final Map<Integer, IntervalArray<Enhancer>> ENHANCER_MAP = makeEnhancerMap();
    private static final Map<String, GeneWithId> GENE_SYMBOL_MAP = Map.of(
            "GCK", new GeneWithId("GCK", TermId.of("ENTREZ:2645")),
            "SURF1", new GeneWithId("SURF1", TermId.of("ENTREZ:6834")),
            "SURF2", new GeneWithId("SURF2", TermId.of("ENTREZ:6835")));

    private static final Set<TermId> PATIENT_TERMS = Set.of(
            TermId.of("HP:0003074"), // hyperglycemia
            TermId.of("HP:0001508")  // failure to thrive
    );

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
        String metabolism = "metabolism"; // represents an UBERON/CL term.
        String cns = "CNS";
        Enhancer surf1Enhancer = new Enhancer(chr9, 133_356_501, 133_356_530, .8, TermId.of("HP:0001939"),metabolism);
        Enhancer gckEnhancer = new Enhancer(chr7, 44_190_001, 44_190_050, .8, TermId.of("HP:0001939"),metabolism);
        Enhancer closeToGckNotPhenotypicallyRelevant = new Enhancer(chr7, 44_195_001, 44_195_500, .8, TermId.of("HP:0000707"), cns); // abnormality of the nervous system

        IntervalArray<Enhancer> chr7Array = new IntervalArray<>(List.of(gckEnhancer, closeToGckNotPhenotypicallyRelevant), new EnhancerEndExtractor());
        IntervalArray<Enhancer> chr9Array = new IntervalArray<>(List.of(surf1Enhancer), new EnhancerEndExtractor());
        return Map.of(7, chr7Array, 9, chr9Array);
    }

    private static Map<TermId, Set<HpoDiseaseSummary>> makeDiseaseSummaryMap() {
        HpoDiseaseSummary mody2 = new HpoDiseaseSummary(
                new HpoDisease("Maturity onset diabetes of the young, type 2; MODY2",
                        TermId.of("OMIM:125851"),
                        List.of(HpoAnnotation.builder(TermId.of("HP:0003074")) // hyperglycemia
                                        .frequency(1., "Obligate")
                                        .onset(HpoOnset.INFANTILE_ONSET)
                                        .build(),
                                HpoAnnotation.builder(TermId.of("HP:0001508")) // Failure to thrive
                                        .frequency(.9, "Very frequent")
                                        .onset(HpoOnset.CONGENITAL_ONSET)
                                        .build()),
                        List.of(TermId.of("HP:0000006")), // Autosomal dominant inheritance
                        List.of(), // not terms
                        List.of(), // clinical modifiers
                        List.of() // clinical courses
                ));

        return Map.of(TermId.of("ENTREZ:2645"), Set.of(mody2));
    }

    @BeforeEach
    public void setUp() {
        Overlapper overlapper = new SvAnnOverlapper(TX_SERVICE.getChromosomeMap());
        EnhancerOverlapper enhancerOverlapper = new EnhancerOverlapper(ENHANCER_MAP);
        prioritizer = new PrototypeSvPrioritizer(overlapper, enhancerOverlapper, GENE_SYMBOL_MAP, PATIENT_TERMS, RELEVANT_ENHANCER_TOP_LEVEL_TERMS, DISEASE_MAP);
    }

    /* *****************************************************************************************************************
     *
     *                                          DELETIONS
     *
     ******************************************************************************************************************/

    /**
     * Deletion of a single exon is HIGH impact.
     */
    @Test
    public void prioritize_singleExonDeletion_SURF1_exon2() {
        SequenceRearrangement sr = Deletions.surf1SingleExon_exon2();
        SvPriority result = prioritizer.prioritize(sr);
        assertThat(result.getImpact(), is(SvImpact.HIGH));
    }

    /**
     * Deletion of 2 exons is HIGH impact.
     */
    @Test
    public void prioritize_twoExonDeletion_SURF1_exons_6_and_7() {
        SequenceRearrangement sr = Deletions.surf1TwoExon_exons_6_and_7();
        SvPriority result = prioritizer.prioritize(sr);

        assertThat(result.getImpact(), is(SvImpact.HIGH));
    }

    /**
     * Deletion affecting a relevant enhancer is HIGH impact.
     */
    @Test
    public void prioritize_upstreamDeletion_GCK_inEnhancer() {
        SequenceRearrangement sr = Deletions.gckUpstreamIntergenic_affectingEnhancer();
        SvPriority result = prioritizer.prioritize(sr);

        assertThat(result.getImpact(), is(SvImpact.HIGH));
    }

    /**
     * Deletion affecting region <2kb from gene is HIGH impact.
     */
    @Test
    public void prioritize_upstreamDeletion_GCK_notInEnhancer() {
        SequenceRearrangement sr = Deletions.gckUpstreamIntergenic_NotAffectingEnhancer();
        SvPriority result = prioritizer.prioritize(sr);

        assertThat(result.getImpact(), is(SvImpact.HIGH));
    }

    /**
     * Deletion affecting phenotypically non-relevant enhancer is INTERMEDIATE impact.
     */
    @Test
    public void prioritize_upstreamDeletion_GCK_inNonrelevantEnhancer() {
        SequenceRearrangement sr = Deletions.gckUpstreamIntergenic_affectingPhenotypicallyNonrelevantEnhancer();
        SvPriority result = prioritizer.prioritize(sr);

        assertThat(result.getImpact(), is(SvImpact.INTERMEDIATE));
    }

    @Test
    public void prioritize_deletionAffectingMultipleGenes() {
        SequenceRearrangement sr = Deletions.surf1Surf2oneEntireTranscriptAndPartOfAnother();
        SvPriority result = prioritizer.prioritize(sr);

        assertThat(result.getImpact(), is(SvImpact.HIGH));
    }

    /* *****************************************************************************************************************
     *
     *                                          INSERTIONS
     *
     ******************************************************************************************************************/

    /**
     * Insertion into exonic sequence is HIGH.
     */
    @Test
    public void insertionInExonicRegion() {
        SequenceRearrangement sr = Insertions.surf2Exon4();
        SvPriority result = prioritizer.prioritize(sr);

        assertThat(result.getImpact(), is(SvImpact.HIGH));
    }

    /**
     * Insertion deep in intron (>100) is LOW impact
     */
    @Test
    public void insertionInIntronRegion() {
        SequenceRearrangement sr = Insertions.surf2Intron3();
        SvPriority result = prioritizer.prioritize(sr);

        // this fails because getting distance does not work correctly
        assertThat(result.getImpact(), is(SvImpact.LOW));
    }

    /**
     * Insertion in 5UTR or 3UTR is INTERMEDIATE
     */
    @Test
    public void insertionInUtr() {
        // 5UTR
        SequenceRearrangement sr = Insertions.surf2InsertionIn5UTR();
        SvPriority result = prioritizer.prioritize(sr);

        assertThat(result.getImpact(), is(SvImpact.INTERMEDIATE));

        // 3UTR
        sr = Insertions.surf1InsertionIn3UTR();
        result = prioritizer.prioritize(sr);

        assertThat(result.getImpact(), is(SvImpact.INTERMEDIATE));
    }

    /**
     * Insertion in phenotypically relevant enhancer is HIGH.
     */
    @Test
    public void insertionInRelevantEnhancer() {
        SequenceRearrangement sr = Insertions.gckRelevantEnhancer();
        SvPriority result = prioritizer.prioritize(sr);

        assertThat(result.getImpact(), is(SvImpact.HIGH));
    }

    /**
     * Insertion in phenotypically non relevant enhancer is INTERMEDIATE.
     */
    @Test
    public void insertionInNonrelevantEnhancer() {
        SequenceRearrangement sr = Insertions.gckNonRelevantEnhancer();
        SvPriority result = prioritizer.prioritize(sr);

        assertThat(result.getImpact(), is(SvImpact.INTERMEDIATE));
    }

    /**
     * Insertion in intergenic region is LOW impact.
     */
    @Test
    public void insertionInIntergenicRegion() {
        SequenceRearrangement sr = Insertions.gckIntergenic();
        SvPriority result = prioritizer.prioritize(sr);

        assertThat(result.getImpact(), is(SvImpact.LOW));
    }

    /* *****************************************************************************************************************
     *
     *                                          INVERSIONS
     *
     ******************************************************************************************************************/

    /**
     * Inversion where both ends are within the same intron is LOW impact.
     */
    @Test
    public void inversionWhereBothBreakendsAreWithinTheSameIntron() {
        SequenceRearrangement sr = Inversions.gckIntronic();
        SvPriority result = prioritizer.prioritize(sr);

        // this fails because getting distance does not work correctly
        assertThat(result.getImpact(), is(SvImpact.LOW));
    }

    /**
     * Inversion that affects coding region is HIGH impact.
     */
    @Test
    public void inversionAffectingCodingRegion() {
        SequenceRearrangement sr = Inversions.gckExonic();
        SvPriority result = prioritizer.prioritize(sr);

        assertThat(result.getImpact(), is(SvImpact.HIGH));
    }

    @Test
    public void inversionAffectingAnEnhancer() {
        // TODO: 4. 11. 2020 implement
    }

    @Test
    public void inversionAffectingAPromoter() {
        // TODO: 4. 11. 2020 implement
    }

    @Test
    public void inversionUpstream() {
        // TODO: 4. 11. 2020 implement
    }
}