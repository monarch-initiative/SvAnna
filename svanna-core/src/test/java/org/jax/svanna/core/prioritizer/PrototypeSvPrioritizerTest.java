package org.jax.svanna.core.prioritizer;

import de.charite.compbio.jannovar.impl.intervals.IntervalArray;
import org.jax.svanna.core.TestDataConfig;
import org.jax.svanna.core.hpo.GeneWithId;
import org.jax.svanna.core.hpo.HpoDiseaseSummary;
import org.jax.svanna.core.overlap.*;
import org.jax.svanna.core.reference.*;
import org.jax.svanna.test.TestVariants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.phenol.ontology.data.Term;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.svart.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.lessThan;

/**
 * Let's assume we're running prioritization for a patient with a causal SV in <em>GCK</em> gene. Variants in GCK lead to
 * Maturity Onset Diabetes of the Young type 2 (MODY2, OMIM:125851) which is a form of NIDDM (OMIM:125853) characterized
 * by monogenic autosomal dominant transmission and early age of onset.
 * <p>
 * Thus, the patient might have terms such as HP:0003074 (Hyperglycemia), and HP:0001508 (Failure to thrive).
 */
@SpringBootTest(classes = TestDataConfig.class)
public class PrototypeSvPrioritizerTest {

    private Map<Integer, IntervalArray<Enhancer>> enhancerMap;

    /**
     * Assuming the patient has variant in GCK, hyperglycemia and failure to thrive, these are the
     * sensible relevant enhancer top level terms.
     */
    private final Set<TermId> relevantEnhancerTopLevelTerms = Set.of(
            TermId.of("HP:0001507"), // Growth abnormality, based on failure to thrive
            TermId.of("HP:0011004"), //"Abnormal systemic arterial morphology"
            TermId.of("HP:0001939") // Abnormality of metabolism/homeostasis, based on hyperglycemia
    );

    private final Map<TermId, Set<HpoDiseaseSummary>> diseaseMap = makeDiseaseSummaryMap();
    private final Map<String, GeneWithId> geneSymbolMap = Map.of(
            "GCK", new GeneWithId("GCK", TermId.of("NCBIGene:2645")),
            "FBN1", new GeneWithId("FBN1", TermId.of("NCBIGene:2200")),
            "SURF1", new GeneWithId("SURF1", TermId.of("NCBIGene:6834")),
            "SURF2", new GeneWithId("SURF2", TermId.of("NCBIGene:6835")));

    private final Set<TermId> patientTerms = Set.of(
            TermId.of("HP:0003074"), // hyperglycemia
            TermId.of("HP:0001508")  // failure to thrive
    );

    @Autowired
    public TranscriptService transcriptService;

    @Autowired
    public TestVariants testVariants;

    @Autowired
    public GenomicAssembly genomicAssembly;

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
    private static Map<Integer, IntervalArray<Enhancer>> makeEnhancerMap(GenomicAssembly assembly) {
        EnhancerTissueSpecificity growth = EnhancerTissueSpecificity.of(Term.of("UBERON:0001017", "central nervous system"), Term.of("HP:0001507", "Growth abnormality"), .3);
        EnhancerTissueSpecificity brain = EnhancerTissueSpecificity.of(Term.of("UBERON:0000955", "brain"), Term.of("HP:0000707", "Abnormality of the nervous system"), .4);
        EnhancerTissueSpecificity liver = EnhancerTissueSpecificity.of(Term.of("UBERON:0002107", "liver"), Term.of("HP:0001939", "Abnormality of metabolism/homeostasis"), .5);
        EnhancerTissueSpecificity arteries = EnhancerTissueSpecificity.of(Term.of("UBERON:0000947", "aorta"), Term.of("HP:0011004", "Abnormal systemic arterial morphology"), .6);


        Enhancer surf1Enhancer = BaseEnhancer.of(assembly.contigByName("9"), Strand.POSITIVE, CoordinateSystem.zeroBased(), Position.of(133_356_501), Position.of(133_356_530),
                "surf1Enhancer", EnhancerSource.UNKNOWN, false, .3, Set.of(growth));
        Enhancer gckEnhancer = BaseEnhancer.of(assembly.contigByName("7"), Strand.POSITIVE, CoordinateSystem.zeroBased(),Position.of(44_190_001), Position.of(44_190_050),
                "gckEnhancer", EnhancerSource.UNKNOWN, false, .4, Set.of(liver));
        Enhancer chr20Enhancer = BaseEnhancer.of(assembly.contigByName("20"), Strand.POSITIVE, CoordinateSystem.zeroBased(), Position.of(51_642_723), Position.of(51_642_826),
                "chr20Enhancer", EnhancerSource.UNKNOWN, false, .5, Set.of(brain));
        Enhancer closeToGckNotPhenotypicallyRelevant = BaseEnhancer.of(assembly.contigByName("7"), Strand.POSITIVE, CoordinateSystem.zeroBased(), Position.of(44_195_001), Position.of(44_195_500),
                "closeToGckNotPhenotypicallyRelevant", EnhancerSource.UNKNOWN, true, .1, Set.of(brain));
        // the relevant HPO term for aorta is Abnormal systemic arterial morphology
        // Enhancers expect to get an HPO term and an UBERON/CL label
        int fbn1TSS = 48_646_788;
        Enhancer fbn190kbUpstream = BaseEnhancer.of(assembly.contigByName("15"), Strand.POSITIVE, CoordinateSystem.zeroBased(), Position.of(fbn1TSS + 90_000), Position.of(fbn1TSS + 90_000 + 300),
                        "fbn190kbUpstream", EnhancerSource.UNKNOWN, false, .6, Set.of(arteries));

        IntervalArray<Enhancer> chr7Array = new IntervalArray<>(List.of(gckEnhancer, closeToGckNotPhenotypicallyRelevant), new EnhancerEndExtractor());
        IntervalArray<Enhancer> chr9Array = new IntervalArray<>(List.of(surf1Enhancer), new EnhancerEndExtractor());
        IntervalArray<Enhancer> chr15Array = new IntervalArray<>(List.of(fbn190kbUpstream), new EnhancerEndExtractor());
        IntervalArray<Enhancer> chr20array = new IntervalArray<>(List.of(chr20Enhancer), new EnhancerEndExtractor());
        return Map.of(7, chr7Array, 9, chr9Array, 15, chr15Array,20, chr20array);
    }

    private static HpoDiseaseSummary makeDiseaseSummary(String name, TermId diseaseId) {
//        HpoDisease disease = new HpoDisease(name,
//                diseaseId,
//                List.of(HpoAnnotation.builder(TermId.of("HP:0003074")) // hyperglycemia
//                                .frequency(1., "Obligate")
//                                .onset(HpoOnset.INFANTILE_ONSET)
//                                .build(),
//                        HpoAnnotation.builder(TermId.of("HP:0001508")) // Failure to thrive
//                                .frequency(.9, "Very frequent")
//                                .onset(HpoOnset.CONGENITAL_ONSET)
//                                .build()),
//                List.of(TermId.of("HP:0000006")), // Autosomal dominant inheritance
//                List.of(), // not terms
//                List.of(), // clinical modifiers
//                List.of() // clinical courses
//        );
        return new HpoDiseaseSummary(diseaseId.getValue(), name);
    }

    private static Map<TermId, Set<HpoDiseaseSummary>> makeDiseaseSummaryMap() {
        HpoDiseaseSummary mody2 =
                makeDiseaseSummary("Maturity onset diabetes of the young, type 2; MODY2",TermId.of("OMIM:125851"));
        // SURF1 deletion example
        HpoDiseaseSummary cmt4k =
                makeDiseaseSummary("Charcot-Marie-Tooth disease, type 4K",TermId.of("OMIM:616684"));
        HpoDiseaseSummary marfan =
                makeDiseaseSummary("Marfan syndrome", TermId.of("OMIM:157000"));
        Map<TermId, Set<HpoDiseaseSummary>> diseaseMap = new HashMap<>();
        diseaseMap.put(TermId.of("NCBIGene:2645"), Set.of(mody2));
        diseaseMap.put(TermId.of("NCBIGene:6834"), Set.of(cmt4k));
        diseaseMap.put(TermId.of("NCBIGene:2200"), Set.of(marfan));
        return diseaseMap;
    }

    @BeforeEach
    public void setUp() {
        enhancerMap = makeEnhancerMap(genomicAssembly);
        Overlapper overlapper = new SvAnnOverlapper(transcriptService.getChromosomeMap());

        EnhancerOverlapper enhancerOverlapper = new EnhancerOverlapper(enhancerMap);
        prioritizer = new PrototypeSvPrioritizer(overlapper, enhancerOverlapper, geneSymbolMap, patientTerms, relevantEnhancerTopLevelTerms, diseaseMap);
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
        Variant sr = testVariants.deletions().surf1SingleExon_exon2();
        SvPriority result = prioritizer.prioritize(sr);
        assertThat(result.getImpact(), is(SvImpact.HIGH));
    }

    /**
     * Deletion of 2 exons is HIGH impact.
     */
    @Test
    public void prioritize_twoExonDeletion_SURF1_exons_6_and_7() {
        Variant sr = testVariants.deletions().surf1TwoExon_exons_6_and_7();
        SvPriority result = prioritizer.prioritize(sr);

        assertThat(result.getImpact(), is(SvImpact.VERY_HIGH));
    }

    /**
     * Deletion affecting a relevant enhancer is HIGH impact.
     */
    @Test
    public void prioritize_upstreamDeletion_GCK_inEnhancer() {
        Variant sr = testVariants.deletions().gckUpstreamIntergenic_affectingEnhancer();
        SvPriority result = prioritizer.prioritize(sr);

        assertThat(result.getImpact(), is(SvImpact.HIGH));
    }

    /**
     * Deletion affecting region <2kb from gene is INTERMEDIATE impact. (<500bp is high)
     * This gene is phenotypically relevant.
     * This is 1.56kb, so LOW
     */
    @Test
    public void prioritize_upstreamDeletion_GCK_notInEnhancer() {
        Variant sr = testVariants.deletions().gckUpstreamIntergenic_NotAffectingEnhancer();
        SvPriority result = prioritizer.prioritize(sr);

        assertThat(result.getImpact(), is(SvImpact.INTERMEDIATE));
    }

    /**
     * Deletion affecting phenotypically non-relevant enhancer is INTERMEDIATE impact.
     */
    @Test
    public void prioritize_upstreamDeletion_GCK_inNonrelevantEnhancer() {
        Variant sr = testVariants.deletions().gckUpstreamIntergenic_affectingPhenotypicallyNonrelevantEnhancer();
        SvPriority result = prioritizer.prioritize(sr);

        assertThat(result.getImpact(), is(SvImpact.INTERMEDIATE));
    }

    @Test
    public void prioritize_deletionAffectingMultipleGenes() {
        Variant sr = testVariants.deletions().surf1Surf2oneEntireTranscriptAndPartOfAnother();
        SvPriority result = prioritizer.prioritize(sr);

        assertThat(result.getImpact(), is(SvImpact.HIGH));
    }

    /* *****************************************************************************************************************
     *
     *                                          INSERTIONS
     *
     ******************************************************************************************************************/

    /**
     * Insertion into exonic sequence is HIGH. but in this case it is downgraded to
     * INTERMEDIATE because the gene is not phenotypically relevant.
     */
    @Test
    public void insertionInExonicRegion() {
        Variant sr = testVariants.insertions().surf2Exon4();
        SvPriority result = prioritizer.prioritize(sr);

        assertThat(result.getImpact(), is(SvImpact.INTERMEDIATE));
    }

    /**
     * Insertion deep in intron (>100) is LOW impact
     */
    @Test
    @Disabled("FIX") // TODO - fix
    public void insertionInIntronRegion() {
        Variant sr = testVariants.insertions().surf2Intron3();
        SvPriority result = prioritizer.prioritize(sr);

        // this fails because getting distance does not work correctly
        assertThat(result.getImpact(), is(SvImpact.LOW));
    }

    /**
     * Insertion in 5UTR or 3UTR is INTERMEDIATE, but is LOW if there is no associated disease
     * SURF2 does not have any associated disease, so it is LOW
     * SURF1 has an associated disease, so it is INTERMEDIATE
     */
    @Test
    public void insertionInUtr() {
        // 5UTR
        Variant sr = testVariants.insertions().surf2InsertionIn5UTR();
        SvPriority result = prioritizer.prioritize(sr);

        assertThat(result.getImpact(), is(SvImpact.INTERMEDIATE));

        // 3UTR
        sr = testVariants.insertions().surf1InsertionIn3UTR();
        result = prioritizer.prioritize(sr);

        assertThat(result.getImpact(), is(SvImpact.HIGH));
    }

    /**
     * Insertion in phenotypically relevant enhancer is HIGH.
     */
    @Test
    public void insertionInRelevantEnhancer() {
        Variant sr = testVariants.insertions().gckRelevantEnhancer();
        SvPriority result = prioritizer.prioritize(sr);

        assertThat(result.getImpact(), is(SvImpact.HIGH));
    }

    /**
     * Insertion in phenotypically non relevant enhancer is INTERMEDIATE.
     */
    @Test
    public void insertionInNonrelevantEnhancer() {
        Variant sr = testVariants.insertions().gckNonRelevantEnhancer();
        SvPriority result = prioritizer.prioritize(sr);

        assertThat(result.getImpact(), is(SvImpact.INTERMEDIATE));
    }

    /**
     * Insertion in intergenic region is LOW impact.
     */
    @Test
    public void insertionInIntergenicRegion() {
        Variant sr = testVariants.insertions().gckIntergenic();
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
        Variant sr = testVariants.inversions().gckIntronic();
        SvPriority result = prioritizer.prioritize(sr);

        // this fails because getting distance does not work correctly
        assertThat(result.getImpact(), is(SvImpact.LOW));
    }

    /**
     * Inversion that affects coding region is HIGH impact.
     */
    @Test
    public void inversionAffectingCodingRegion() {
        Variant sr = testVariants.inversions().gckExonic();
        SvPriority result = prioritizer.prioritize(sr);

        assertThat(result.getImpact(), is(SvImpact.HIGH));
    }

    /**
     * Test inversion that disrupts the sequence of this enhancer
     * chr10	100184852	100185124	0.420772	UBERON:0000955	brain	HP:0012443	Abnormality of brain morphology
     * The sequence impact is HIGH but there is not associated disease here so the final impact
     * should be INTERMEDIATE
     */
    @Test
    public void inversionAffectingAnEnhancer() {
        Variant sr = testVariants.inversions().brainEnhancerDisruptedByInversion();
        SvPriority result = prioritizer.prioritize(sr);
        assertThat(result.getImpact(), is(SvImpact.INTERMEDIATE));
    }

    /**
     * An inversion in the FBN1 promoter, 50 bp upstream
     */
    @Test
    public void inversionAffectingAPromoter() {
        Variant sr = testVariants.inversions().fbn1PromoterInversion();
        SvPriority result = prioritizer.prioritize(sr);
        assertThat(result.getImpact(), is(SvImpact.HIGH));
        assertThat(result.hasPhenotypicRelevance(), equalTo(true));
        List<Overlap> overlaps = result.getOverlaps();
        assertThat(overlaps.size(), is(1));
        Overlap olap = overlaps.get(0);
        assertThat(olap.getOverlapType(), is(OverlapType.UPSTREAM_GENE_VARIANT_500B));
        assertThat(olap.getDistance(), is(lessThan(500))); // 500bp class, actually it is 48bp
        assertThat(olap.getGeneSymbol(), is("FBN1"));
    }

    /**
     * A 300bp inversion, 25000 upstream of FBN1, does not affect an enhancer, expect low impact
     */
    @Test
    public void inversionUpstream() {
        Variant sr = testVariants.inversions().fbn1UpstreamInversion();
        SvPriority result = prioritizer.prioritize(sr);
        assertThat(result.getImpact(), is(SvImpact.LOW));
    }

    /**
     * This tests that an inversion of the entire FBN1 which does not disrupt the
     * gene, but is near to an enhancer (90kb away), is pathogenic for Marfan syndrome
     * (according to the logic of this test, anyway).
     */
    @Test
    public void inversionOfEntireFbn1() {
        Variant sr = testVariants.inversions().fbn1WholeGeneEnhancerAt90kb();
        SvPriority result = prioritizer.prioritize(sr);
        assertThat(result.getImpact(), is(SvImpact.HIGH));
        assertThat(result.getDiseases().stream().
                map(HpoDiseaseSummary::getDiseaseId)
                .anyMatch(value -> value.equals("OMIM:157000")), equalTo(true));
    }

    /* *****************************************************************************************************************
     *
     *                                          BREAKENDS
     *
     ******************************************************************************************************************/

    // TODO - add tests
}