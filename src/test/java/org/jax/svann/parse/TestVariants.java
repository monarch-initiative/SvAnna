package org.jax.svann.parse;


import org.jax.svann.TestBase;
import org.jax.svann.reference.Position;
import org.jax.svann.reference.SequenceRearrangement;
import org.jax.svann.reference.Strand;
import org.jax.svann.reference.SvType;
import org.jax.svann.reference.genome.Contig;

@SuppressWarnings("OptionalGetWithoutIsPresent")
public class TestVariants extends TestBase {

    /**
     * Single exon deletion
     * <p>
     * SURF2:NM_017503.5 deletion of exon 3, tx on (+) strand
     * chr9:133_357_501-133_358_000
     */
    public static SequenceRearrangement singleExonDeletion_SURF2_exon3() {
        SimpleBreakend left = SimpleBreakend.of(
                ChromosomalPosition.of(GENOME_ASSEMBLY.getContigByName("9").get(), Position.precise(133_357_501), Strand.FWD),
                "left_single_exon_del", "C");
        SimpleBreakend right = SimpleBreakend.of(
                ChromosomalPosition.of(GENOME_ASSEMBLY.getContigByName("9").get(), Position.precise(133_358_000), Strand.FWD),
                "right_single_exon_del", "A");

        return SimpleSequenceRearrangement.of(SvType.DELETION, SimpleAdjacency.empty(left, right));
    }


    /**
     * Two exon deletion.
     * <p>
     * SURF1:NM_003172.4 deletion of exons 6 and 7, tx on (-) strand
     * chr9:133_352_301-133_352_900
     */
    public static SequenceRearrangement twoExonDeletion_SURF1_exons_6_and_7() {
        SimpleBreakend left = SimpleBreakend.of(
                ChromosomalPosition.of(GENOME_ASSEMBLY.getContigByName("9").get(), Position.precise(133_352_301), Strand.FWD),
                "two_exon_del_l", "T");
        SimpleBreakend right = SimpleBreakend.of(
                ChromosomalPosition.of(GENOME_ASSEMBLY.getContigByName("9").get(), Position.precise(133_352_900), Strand.FWD),
                "two_exon_del_r", "C");

        return SimpleSequenceRearrangement.of(SvType.DELETION, SimpleAdjacency.empty(left, right));
    }

    /**
     * Deletion of one entire transcript and part of another.
     * <p>
     * SURF1:NM_003172.4 entirely deleted, SURF2:NM_017503.5 partially deleted
     * chr9:133_350_001-133_358_000
     */
    public static SequenceRearrangement deletionOfOneEntireTranscriptAndPartOfAnother() {
        SimpleBreakend left = SimpleBreakend.of(
                ChromosomalPosition.of(GENOME_ASSEMBLY.getContigByName("9").get(), Position.precise(133_350_001), Strand.FWD),
                "entire_tx_del_l", "G");
        SimpleBreakend right = SimpleBreakend.of(
                ChromosomalPosition.of(GENOME_ASSEMBLY.getContigByName("9").get(), Position.precise(133_358_000), Strand.FWD),
                "entire_tx_del_r", "A");

        return SimpleSequenceRearrangement.of(SvType.DELETION, SimpleAdjacency.empty(left, right));
    }


    /**
     * Deletion within an intron.
     * <p>
     * SURF2:NM_017503.5 700bp deletion within intron 3
     * chr9:133_359_001-133_359_700
     */
    public static SequenceRearrangement deletionWithinAnIntron() {
        SimpleBreakend left = SimpleBreakend.of(
                ChromosomalPosition.of(GENOME_ASSEMBLY.getContigByName("9").get(), Position.precise(133_359_001), Strand.FWD),
                "del_within_intron_l", "C");
        SimpleBreakend right = SimpleBreakend.of(
                ChromosomalPosition.of(GENOME_ASSEMBLY.getContigByName("9").get(), Position.precise(133_359_700), Strand.FWD),
                "del_within_intron_r", "G");

        return SimpleSequenceRearrangement.of(SvType.DELETION, SimpleAdjacency.empty(left, right));
    }

    /**
     * Deletion in 5UTR.
     * <p>
     * SURF2:NM_017503.5 20bp deletion in 5UTR
     * chr9:133_356_561-133_356_580
     */
    public static SequenceRearrangement deletionIn5UTR() {
        SimpleBreakend left = SimpleBreakend.of(
                ChromosomalPosition.of(GENOME_ASSEMBLY.getContigByName("9").get(), Position.precise(133_356_561), Strand.FWD),
                "del_in_5utr_l", "");
        SimpleBreakend right = SimpleBreakend.of(
                ChromosomalPosition.of(GENOME_ASSEMBLY.getContigByName("9").get(), Position.precise(133_356_580), Strand.FWD),
                "del_in_5utr_r", "");

        return SimpleSequenceRearrangement.of(SvType.DELETION, SimpleAdjacency.empty(left, right));
    }

    /**
     * Deletion in 3UTR.
     * <p>
     * SURF1:NM_003172.4 100bp deletion in 3UTR
     * chr9:133_351_801-133_351_900
     */
    public static SequenceRearrangement deletionIn3UTR() {
        SimpleBreakend left = SimpleBreakend.of(
                ChromosomalPosition.of(GENOME_ASSEMBLY.getContigByName("9").get(), Position.precise(133_351_801), Strand.FWD),
                "del_in_3utr_l", "C");
        SimpleBreakend right = SimpleBreakend.of(
                ChromosomalPosition.of(GENOME_ASSEMBLY.getContigByName("9").get(), Position.precise(133_351_900), Strand.FWD),
                "del_in_3utr_r", "G");

        return SimpleSequenceRearrangement.of(SvType.DELETION, SimpleAdjacency.empty(left, right));
    }

    /**
     * Deletion downstream intergenic.
     * <p>
     * SURF1:NM_003172.4 downstream, 10kb deletion
     * chr9:133_300_001-133_310_000
     */
    public static SequenceRearrangement deletionDownstreamIntergenic() {
        SimpleBreakend left = SimpleBreakend.of(
                ChromosomalPosition.of(GENOME_ASSEMBLY.getContigByName("9").get(), Position.precise(133_300_001), Strand.FWD),
                "del_downstream_intergenic_l", "C");
        SimpleBreakend right = SimpleBreakend.of(
                ChromosomalPosition.of(GENOME_ASSEMBLY.getContigByName("9").get(), Position.precise(133_310_000), Strand.FWD),
                "del_downstream_intergenic_l", "G");

        return SimpleSequenceRearrangement.of(SvType.DELETION, SimpleAdjacency.empty(left, right));
    }

    /**
     * Deletion upstream intergenic.
     * <p>
     * SURF1:NM_017503.5 upstream, 10kb deletion
     * chr9:133_380_001-133_381_000
     */
    public static SequenceRearrangement deletionUpstreamIntergenic() {
        SimpleBreakend left = SimpleBreakend.of(
                ChromosomalPosition.of(GENOME_ASSEMBLY.getContigByName("9").get(), Position.precise(133_380_001), Strand.FWD),
                "del_upstream_intergenic_l", "C");
        SimpleBreakend right = SimpleBreakend.of(
                ChromosomalPosition.of(GENOME_ASSEMBLY.getContigByName("9").get(), Position.precise(133_381_000), Strand.FWD),
                "del_upstream_intergenic_r", "G");

        return SimpleSequenceRearrangement.of(SvType.DELETION, SimpleAdjacency.empty(left, right));
    }

    /**
     * Insertion in 5'UTR.
     * <p>
     * SURF2:NM_017503.5 10bp insertion in 5UTR
     * chr9:133_356_571-133_356_571
     */
    public static SequenceRearrangement insertionIn5UTR() {
        Contig contig = new InsertionContig("ins5UTR", 10);
        Contig chr9 = GENOME_ASSEMBLY.getContigByName("9").get();
        SimpleBreakend alphaLeft = SimpleBreakend.of(
                ChromosomalPosition.of(chr9, Position.precise(133_356_571), Strand.FWD),
                "a_ins_5utr_l", "C");
        SimpleBreakend alphaRight = SimpleBreakend.of(
                ChromosomalPosition.of(contig, Position.precise(1), Strand.FWD),
                "a_ins_5utr_r", "");
        SimpleAdjacency alpha = SimpleAdjacency.empty(alphaLeft, alphaRight);

        SimpleBreakend betaLeft = SimpleBreakend.of(
                ChromosomalPosition.of(contig, Position.precise(10), Strand.FWD),
                "b_ins_5utr_l", "");
        SimpleBreakend betaRight = SimpleBreakend.of(
                ChromosomalPosition.of(chr9, Position.precise(133_356_572), Strand.FWD),
                "b_ins_5utr_r", "");
        SimpleAdjacency beta = SimpleAdjacency.empty(betaLeft, betaRight);

        return SimpleSequenceRearrangement.of(SvType.INSERTION, alpha, beta);
    }

    /**
     * Insertion in 3'UTR
     * <p>
     * SURF1:NM_003172.4 10bp insertion in 3UTR
     * chr9:133_351_851-133_351_851
     */
    public static SequenceRearrangement insertionIn3UTR() {
        Contig contig = new InsertionContig("ins3UTR", 10);
        Contig chr9 = GENOME_ASSEMBLY.getContigByName("9").get();
        SimpleBreakend alphaLeft = SimpleBreakend.of(
                ChromosomalPosition.of(chr9, Position.precise(133_351_851), Strand.FWD),
                "a_ins_3utr_l", "");
        SimpleBreakend alphaRight = SimpleBreakend.of(
                ChromosomalPosition.of(contig, Position.precise(1), Strand.FWD),
                "a_ins_3utr_r", "");
        SimpleAdjacency alpha = SimpleAdjacency.empty(alphaLeft, alphaRight);

        SimpleBreakend betaLeft = SimpleBreakend.of(
                ChromosomalPosition.of(contig, Position.precise(10), Strand.FWD),
                "b_ins_3utr_l", "");
        SimpleBreakend betaRight = SimpleBreakend.of(
                ChromosomalPosition.of(chr9, Position.precise(133_351_852), Strand.FWD),
                "b_ins_3utr_r", "");
        SimpleAdjacency beta = SimpleAdjacency.empty(betaLeft, betaRight);

        return SimpleSequenceRearrangement.of(SvType.INSERTION, alpha, beta);
    }

    /**
     * Insertion in exon.
     * <p>
     * SURF2:NM_017503.5 10bp insertion in exon 4
     * chr9:133_360_001-133_360_001
     */
    public static SequenceRearrangement insertionInExon4() {
        Contig contig = new InsertionContig("insInExon4", 10);
        Contig chr9 = GENOME_ASSEMBLY.getContigByName("9").get();
        SimpleBreakend alphaLeft = SimpleBreakend.of(
                ChromosomalPosition.of(chr9, Position.precise(133_360_001), Strand.FWD),
                "a_ins_exon_l", "");
        SimpleBreakend alphaRight = SimpleBreakend.of(
                ChromosomalPosition.of(contig, Position.precise(1), Strand.FWD),
                "a_ins_exon_r", "");
        SimpleAdjacency alpha = SimpleAdjacency.empty(alphaLeft, alphaRight);

        SimpleBreakend betaLeft = SimpleBreakend.of(
                ChromosomalPosition.of(contig, Position.precise(10), Strand.FWD),
                "b_ins_exon_l", "");
        SimpleBreakend betaRight = SimpleBreakend.of(
                ChromosomalPosition.of(chr9, Position.precise(133_360_002), Strand.FWD),
                "b_ins_exon_r", "");
        SimpleAdjacency beta = SimpleAdjacency.empty(betaLeft, betaRight);

        return SimpleSequenceRearrangement.of(SvType.INSERTION, alpha, beta);
    }


    /**
     * Insertion in intron.
     * <p>
     * SURF2:NM_017503.5 10bp insertion in intron 3
     * chr9:133_359_001-133_359_001
     */
    public static SequenceRearrangement insertionInIntron3() {
        Contig contig = new InsertionContig("insInIntron3", 10);
        Contig chr9 = GENOME_ASSEMBLY.getContigByName("9").get();
        SimpleBreakend alphaLeft = SimpleBreakend.of(
                ChromosomalPosition.of(chr9, Position.precise(133_359_001), Strand.FWD),
                "a_ins_intron_l", "");
        SimpleBreakend alphaRight = SimpleBreakend.of(
                ChromosomalPosition.of(contig, Position.precise(1), Strand.FWD),
                "a_ins_intron_r", "");
        SimpleAdjacency alpha = SimpleAdjacency.empty(alphaLeft, alphaRight);

        SimpleBreakend betaLeft = SimpleBreakend.of(
                ChromosomalPosition.of(contig, Position.precise(10), Strand.FWD),
                "b_ins_intron_l", "");
        SimpleBreakend betaRight = SimpleBreakend.of(
                ChromosomalPosition.of(chr9, Position.precise(133_359_002), Strand.FWD),
                "b_ins_intron_r", "");
        SimpleAdjacency beta = SimpleAdjacency.empty(betaLeft, betaRight);

        return SimpleSequenceRearrangement.of(SvType.INSERTION, alpha, beta);
    }

    /**
     * Translocation where one CDS is disrupted and the other is not
     * <p>
     * left mate, SURF2:NM_017503.5 intron 3 (disrupted CDS)
     * chr9:133_359_000 (+)
     * right mate, upstream from BRCA2 (not disrupted)
     * chr13:32_300_000 (+)
     */
    public static SequenceRearrangement translocationWhereOneCdsIsDisruptedAndTheOtherIsNot() {
        Contig chr9 = GENOME_ASSEMBLY.getContigByName("9").get();
        SimpleBreakend left = SimpleBreakend.of(
                ChromosomalPosition.of(chr9, Position.precise(133_359_000), Strand.FWD),
                "tra_l", "");
        Contig chr13 = GENOME_ASSEMBLY.getContigByName("13").get();
        SimpleBreakend right = SimpleBreakend.of(
                ChromosomalPosition.of(chr13, Position.precise(32_300_000), Strand.FWD),
                "tra_r", "");

        return SimpleSequenceRearrangement.of(SvType.TRANSLOCATION, SimpleAdjacency.empty(left, right));
    }

}
