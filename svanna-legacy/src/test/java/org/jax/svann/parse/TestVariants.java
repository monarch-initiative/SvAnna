package org.jax.svann.parse;


import org.jax.svann.TestBase;
import org.jax.svann.genomicreg.Enhancer;
import org.jax.svann.reference.*;
import org.jax.svann.reference.genome.Contig;
import org.monarchinitiative.phenol.ontology.data.TermId;

public class TestVariants extends TestBase {

    public static class Deletions {
        /**
         * Deletion upstream intergenic | GCK.
         * <p>
         * GCK:NM_000162 upstream, 200b deletion
         * chr7:44_189_901-44_190_100
         */
        public static SequenceRearrangement gckUpstreamIntergenic_affectingEnhancer() {
            Contig chr7 = GENOME_ASSEMBLY.getContigByName("7").orElseThrow();
            BreakendDefault left = BreakendDefault.preciseWithRef(chr7, 44_189_901, Strand.FWD, "gck_del_upstream_intergenic_enhancer_l", "G");
            BreakendDefault right = BreakendDefault.preciseWithRef(chr7, 44_190_100, Strand.FWD, "gck_del_upstream_intergenic_enhancer_r", "G");

            return SequenceRearrangementDefault.of(SvType.DELETION, AdjacencyDefault.empty(left, right));
        }


        /**
         * Deletion upstream intergenic | GCK.
         * <p>
         * GCK:NM_000162 upstream, 1kb deletion
         * chr7:44_191_001-44_192_000
         */
        public static SequenceRearrangement gckUpstreamIntergenic_NotAffectingEnhancer() {
            Contig chr7 = GENOME_ASSEMBLY.getContigByName("7").orElseThrow();
            BreakendDefault left = BreakendDefault.preciseWithRef(chr7, 44_191_001, Strand.FWD, "gck_del_upstream_intergenic_l", "t");
            BreakendDefault right = BreakendDefault.preciseWithRef(chr7, 44_192_000, Strand.FWD, "gck_del_upstream_intergenic_r", "t");

            return SequenceRearrangementDefault.of(SvType.DELETION, AdjacencyDefault.empty(left, right));
        }

        /**
         * Deletion upstream intergenic | GCK.
         * <p>
         * GCK:NM_000162 upstream, 1kb deletion
         * chr7:44_191_001-44_192_000
         */
        public static SequenceRearrangement gckUpstreamIntergenic_affectingPhenotypicallyNonrelevantEnhancer() {
            Contig chr7 = GENOME_ASSEMBLY.getContigByName("7").orElseThrow();
            BreakendDefault left = BreakendDefault.preciseWithRef(chr7, 44_194_501, Strand.FWD, "gck_del_upstream_intergenic_phenotypically_nonrelevant_enhancer_l", "t");
            BreakendDefault right = BreakendDefault.preciseWithRef(chr7, 44_195_500, Strand.FWD, "gck_del_upstream_intergenic_phenotypically_nonrelevant_enhancer_r", "t");

            return SequenceRearrangementDefault.of(SvType.DELETION, AdjacencyDefault.empty(left, right));
        }

        /**
         * Single exon deletion
         * <p>
         * SURF2:NM_017503.5 deletion of exon 3, tx on (+) strand
         * chr9:133_357_501-133_358_000
         */
        public static SequenceRearrangement surf2singleExon_exon3() {
            BreakendDefault left = BreakendDefault.preciseWithRef(GENOME_ASSEMBLY.getContigByName("9").orElseThrow(), 133_357_501, Strand.FWD, "left_single_exon_del", "C");
            BreakendDefault right = BreakendDefault.preciseWithRef(GENOME_ASSEMBLY.getContigByName("9").orElseThrow(), 133_358_000, Strand.FWD, "right_single_exon_del", "A");

            return SequenceRearrangementDefault.of(SvType.DELETION, AdjacencyDefault.empty(left, right));
        }


        /**
         * Two exon deletion.
         * <p>
         * SURF1:NM_003172.4 deletion of exons 6 and 7, tx on (-) strand
         * chr9:133_352_301-133_352_900
         */
        public static SequenceRearrangement surf1TwoExon_exons_6_and_7() {
            Contig chr9 = GENOME_ASSEMBLY.getContigByName("9").orElseThrow();
            BreakendDefault left = BreakendDefault.preciseWithRef(chr9, 133_352_301, Strand.FWD, "two_exon_del_l", "T");
            BreakendDefault right = BreakendDefault.preciseWithRef(chr9, 133_352_900, Strand.FWD, "two_exon_del_r", "C");

            return SequenceRearrangementDefault.of(SvType.DELETION, AdjacencyDefault.empty(left, right));
        }

        /**
         * Single exon deletion.
         * <p>
         * SURF1:NM_003172.4 deletion of the exon 2, tx on (-) strand
         * chr9:133_356_251-133_356_350
         */
        public static SequenceRearrangement surf1SingleExon_exon2() {
            Contig chr9 = GENOME_ASSEMBLY.getContigByName("9").orElseThrow();
            BreakendDefault left = BreakendDefault.preciseWithRef(chr9, 133_356_251, Strand.FWD, "surf1_exon2_del_l", "C");
            BreakendDefault right = BreakendDefault.preciseWithRef(chr9, 133_356_350, Strand.FWD, "surf1_exon2_del_r", "C");

            return SequenceRearrangementDefault.of(SvType.DELETION, AdjacencyDefault.empty(left, right));
        }

        /**
         * Deletion of one entire transcript and part of another.
         * <p>
         * SURF1:NM_003172.4 entirely deleted, SURF2:NM_017503.5 partially deleted
         * chr9:133_350_001-133_358_000
         */
        public static SequenceRearrangement surf1Surf2oneEntireTranscriptAndPartOfAnother() {
            Contig chr9 = GENOME_ASSEMBLY.getContigByName("9").orElseThrow();
            BreakendDefault left = BreakendDefault.preciseWithRef(chr9, 133_350_001, Strand.FWD, "entire_tx_del_l", "G");
            BreakendDefault right = BreakendDefault.preciseWithRef(chr9, 133_358_000, Strand.FWD, "entire_tx_del_r", "A");

            return SequenceRearrangementDefault.of(SvType.DELETION, AdjacencyDefault.empty(left, right));
        }


        /**
         * Deletion within an intron.
         * <p>
         * SURF2:NM_017503.5 700bp deletion within intron 3
         * chr9:133_359_001-133_359_700
         */
        public static SequenceRearrangement surf2WithinAnIntron() {
            Contig chr9 = GENOME_ASSEMBLY.getContigByName("9").orElseThrow();
            BreakendDefault left = BreakendDefault.preciseWithRef(chr9, 133_359_001, Strand.FWD, "del_within_intron_l", "C");
            BreakendDefault right = BreakendDefault.preciseWithRef(chr9, 133_359_700, Strand.FWD, "del_within_intron_r", "G");

            return SequenceRearrangementDefault.of(SvType.DELETION, AdjacencyDefault.empty(left, right));
        }

        /**
         * ZBTB48, is forward strand. Test a variant in intron 1, i.e.,  between	6_580_137-6_580_540
         */
        public static SequenceRearrangement zbtb48intron1() {
            Contig chr1 = GENOME_ASSEMBLY.getContigByName("1").orElseThrow();
            BreakendDefault left = BreakendDefault.preciseWithRef(chr1, 6_580_300, Strand.FWD, "del_within_intron_l", "C");
            BreakendDefault right = BreakendDefault.preciseWithRef(chr1, 6_580_400, Strand.FWD, "del_within_intron_r", "G");

            return SequenceRearrangementDefault.of(SvType.DELETION, AdjacencyDefault.empty(left, right));
        }





        /**
         * Deletion in 5UTR.
         * <p>
         * SURF2:NM_017503.5 20bp deletion in 5UTR
         * chr9:133_356_561-133_356_580
         */
        public static SequenceRearrangement surf2In5UTR() {
            Contig chr9 = GENOME_ASSEMBLY.getContigByName("9").orElseThrow();
            BreakendDefault left = BreakendDefault.preciseWithRef(chr9, 133_356_561, Strand.FWD, "del_in_5utr_l", "T");
            BreakendDefault right = BreakendDefault.preciseWithRef(chr9, 133_356_580, Strand.FWD, "del_in_5utr_r", "G");

            return SequenceRearrangementDefault.of(SvType.DELETION, AdjacencyDefault.empty(left, right));
        }

        /**
         * Deletion in 3UTR.
         * <p>
         * SURF1:NM_003172.4 100bp deletion in 3UTR
         * chr9:133_351_801-133_351_900
         */
        public static SequenceRearrangement surf1In3UTR() {
            Contig chr9 = GENOME_ASSEMBLY.getContigByName("9").orElseThrow();
            BreakendDefault left = BreakendDefault.preciseWithRef(chr9, 133_351_801, Strand.FWD, "del_in_3utr_l", "G");
            BreakendDefault right = BreakendDefault.preciseWithRef(chr9, 133_351_900, Strand.FWD, "del_in_3utr_r", "A");

            return SequenceRearrangementDefault.of(SvType.DELETION, AdjacencyDefault.empty(left, right));
        }

        /**
         * Deletion downstream intergenic.
         * <p>
         * SURF1:NM_003172.4 downstream, 10kb deletion
         * chr9:133_300_001-133_310_000
         */
        public static SequenceRearrangement surf1DownstreamIntergenic() {
            Contig chr9 = GENOME_ASSEMBLY.getContigByName("9").orElseThrow();
            BreakendDefault left = BreakendDefault.preciseWithRef(chr9, 133_300_001, Strand.FWD, "del_downstream_intergenic_l", "t");
            BreakendDefault right = BreakendDefault.preciseWithRef(chr9, 133_310_000, Strand.FWD, "del_downstream_intergenic_l", "C");

            return SequenceRearrangementDefault.of(SvType.DELETION, AdjacencyDefault.empty(left, right));
        }

        /**
         * Deletion upstream intergenic.
         * <p>
         * hg38 chr15:48,408,306-48,645,849 Size: 237,544 Total Exon Count: 66 Strand: -
         * upstream, 10kb deletion
         * chr15:48_655_000-48_665_000
         */
        public static SequenceRearrangement brca2UpstreamIntergenic() {
            Contig chr15 = GENOME_ASSEMBLY.getContigByName("15").orElseThrow();
            BreakendDefault left = BreakendDefault.preciseWithRef(chr15, 48_655_000, Strand.FWD, "del_upstream_intergenic_l", "T");
            BreakendDefault right = BreakendDefault.preciseWithRef(chr15, 48_665_000, Strand.FWD, "del_upstream_intergenic_r", "G");

            return SequenceRearrangementDefault.of(SvType.DELETION, AdjacencyDefault.empty(left, right));
        }
    }

    public static class Insertions {
        /**
         * Insertion in 5'UTR.
         * <p>
         * SURF2:NM_017503.5 10bp insertion in 5UTR
         * chr9:133_356_571-133_356_571
         */
        public static SequenceRearrangement surf2InsertionIn5UTR() {
            Contig contig = new InsertionContig("ins5UTR", 10);
            Contig chr9 = GENOME_ASSEMBLY.getContigByName("9").orElseThrow();
            BreakendDefault alphaLeft = BreakendDefault.preciseWithRef(chr9, 133_356_571, Strand.FWD, "a_ins_5utr_l", "C");
            BreakendDefault alphaRight = BreakendDefault.preciseWithRef(contig, 1, Strand.FWD, "a_ins_5utr_r", "");
            AdjacencyDefault alpha = AdjacencyDefault.empty(alphaLeft, alphaRight);

            BreakendDefault betaLeft = BreakendDefault.preciseWithRef(contig, 10, Strand.FWD, "b_ins_5utr_l", "");
            BreakendDefault betaRight = BreakendDefault.preciseWithRef(chr9, 133_356_572, Strand.FWD, "b_ins_5utr_r", "T");
            AdjacencyDefault beta = AdjacencyDefault.empty(betaLeft, betaRight);

            return SequenceRearrangementDefault.of(SvType.INSERTION, alpha, beta);
        }

        /**
         * Insertion in 3'UTR
         * <p>
         * SURF1:NM_003172.4 10bp insertion in 3UTR
         * chr9:133_351_851-133_351_851
         */
        public static SequenceRearrangement surf1InsertionIn3UTR() {
            Contig contig = new InsertionContig("ins3UTR", 10);
            Contig chr9 = GENOME_ASSEMBLY.getContigByName("9").orElseThrow();
            BreakendDefault alphaLeft = BreakendDefault.preciseWithRef(chr9, 133_351_851, Strand.FWD, "a_ins_3utr_l", "C");
            BreakendDefault alphaRight = BreakendDefault.preciseWithRef(contig, 1, Strand.FWD, "a_ins_3utr_r", "");
            AdjacencyDefault alpha = AdjacencyDefault.empty(alphaLeft, alphaRight);

            BreakendDefault betaLeft = BreakendDefault.preciseWithRef(contig, 10, Strand.FWD, "b_ins_3utr_l", "");
            BreakendDefault betaRight = BreakendDefault.preciseWithRef(chr9, 133_351_852, Strand.FWD, "b_ins_3utr_r", "A");
            AdjacencyDefault beta = AdjacencyDefault.empty(betaLeft, betaRight);

            return SequenceRearrangementDefault.of(SvType.INSERTION, alpha, beta);
        }

        /**
         * Insertion in exon.
         * <p>
         * SURF2:NM_017503.5 10bp insertion in exon 4
         * chr9:133_360_001-133_360_001
         */
        public static SequenceRearrangement surf2Exon4() {
            Contig contig = new InsertionContig("insInExon4", 10);
            Contig chr9 = GENOME_ASSEMBLY.getContigByName("9").orElseThrow();
            BreakendDefault alphaLeft = BreakendDefault.preciseWithRef(
                    chr9, 133_360_001, Strand.FWD,
                    "a_ins_exon_l", "A");
            BreakendDefault alphaRight = BreakendDefault.preciseWithRef(contig, 1, Strand.FWD, "a_ins_exon_r", "");
            AdjacencyDefault alpha = AdjacencyDefault.empty(alphaLeft, alphaRight);

            BreakendDefault betaLeft = BreakendDefault.preciseWithRef(contig, 10, Strand.FWD, "b_ins_exon_l", "");
            BreakendDefault betaRight = BreakendDefault.preciseWithRef(chr9, 133_360_002, Strand.FWD, "b_ins_exon_r", "C");
            AdjacencyDefault beta = AdjacencyDefault.empty(betaLeft, betaRight);

            return SequenceRearrangementDefault.of(SvType.INSERTION, alpha, beta);
        }


        /**
         * Insertion in intron.
         * <p>
         * SURF2:NM_017503.5 10bp insertion in intron 3
         * chr9:133_359_001-133_359_001
         */
        public static SequenceRearrangement surf2Intron3() {
            Contig contig = new InsertionContig("insInIntron3", 10);
            Contig chr9 = GENOME_ASSEMBLY.getContigByName("9").orElseThrow();
            BreakendDefault alphaLeft = BreakendDefault.preciseWithRef(chr9, 133_359_001, Strand.FWD, "a_ins_intron_l", "C");
            BreakendDefault alphaRight = BreakendDefault.preciseWithRef(contig, 1, Strand.FWD, "a_ins_intron_r", "");
            AdjacencyDefault alpha = AdjacencyDefault.empty(alphaLeft, alphaRight);

            BreakendDefault betaLeft = BreakendDefault.preciseWithRef(contig, 10, Strand.FWD, "b_ins_intron_l", "");
            BreakendDefault betaRight = BreakendDefault.preciseWithRef(chr9, 133_359_002, Strand.FWD, "b_ins_intron_r", "A");
            AdjacencyDefault beta = AdjacencyDefault.empty(betaLeft, betaRight);

            return SequenceRearrangementDefault.of(SvType.INSERTION, alpha, beta);
        }


        /**
         * Insertion in GCK enhancer.
         * <p>
         * GCK:NM_000162 upstream, 200b insertion
         * chr7:44_190_025-44_190_026
         */
        public static SequenceRearrangement gckRelevantEnhancer() {
            int inserted = 200;
            Contig contig = new InsertionContig("gckEnhancerInsertion", inserted);
            Contig chr7 = GENOME_ASSEMBLY.getContigByName("7").orElseThrow();
            BreakendDefault alphaLeft = BreakendDefault.preciseWithRef(chr7, 44_190_025, Strand.FWD, "a_ins_intron_l", "T");
            BreakendDefault alphaRight = BreakendDefault.preciseWithRef(contig, 1, Strand.FWD, "a_ins_intron_r", "");
            AdjacencyDefault alpha = AdjacencyDefault.empty(alphaLeft, alphaRight);

            BreakendDefault betaLeft = BreakendDefault.preciseWithRef(contig, inserted, Strand.FWD, "b_ins_intron_l", "");
            BreakendDefault betaRight = BreakendDefault.preciseWithRef(chr7, 44_190_026, Strand.FWD, "b_ins_intron_r", "G");
            AdjacencyDefault beta = AdjacencyDefault.empty(betaLeft, betaRight);

            return SequenceRearrangementDefault.of(SvType.INSERTION, alpha, beta);
        }

        /**
         * Insertion in GCK enhancer.
         * <p>
         * GCK:NM_000162 upstream, 200b insertion
         * chr7:44_195_025-44_195_026
         */
        public static SequenceRearrangement gckNonRelevantEnhancer() {
            int inserted = 200;
            Contig contig = new InsertionContig("gckNonrelevantEnhancerInsertion", inserted);
            Contig chr7 = GENOME_ASSEMBLY.getContigByName("7").orElseThrow();
            BreakendDefault alphaLeft = BreakendDefault.preciseWithRef(chr7, 44_195_025, Strand.FWD, "a_ins_intron_l", "T");
            BreakendDefault alphaRight = BreakendDefault.preciseWithRef(contig, 1, Strand.FWD, "a_ins_intron_r", "");
            AdjacencyDefault alpha = AdjacencyDefault.empty(alphaLeft, alphaRight);

            BreakendDefault betaLeft = BreakendDefault.preciseWithRef(contig, inserted, Strand.FWD, "b_ins_intron_l", "");
            BreakendDefault betaRight = BreakendDefault.preciseWithRef(chr7, 44_195_026, Strand.FWD, "b_ins_intron_r", "G");
            AdjacencyDefault beta = AdjacencyDefault.empty(betaLeft, betaRight);

            return SequenceRearrangementDefault.of(SvType.INSERTION, alpha, beta);
        }

        /**
         * Insertion in GCK intergenic region.
         * <p>
         * GCK:NM_000162 upstream, 200b insertion
         * chr7:44_196_025-44_196_026
         */
        public static SequenceRearrangement gckIntergenic() {
            int inserted = 200;
            Contig contig = new InsertionContig("gckIntergenicInsertion", inserted);
            Contig chr7 = GENOME_ASSEMBLY.getContigByName("7").orElseThrow();
            BreakendDefault alphaLeft = BreakendDefault.preciseWithRef(chr7, 44_196_025, Strand.FWD, "a_ins_intron_l", "t");
            BreakendDefault alphaRight = BreakendDefault.preciseWithRef(contig, 1, Strand.FWD, "a_ins_intron_r", "");
            AdjacencyDefault alpha = AdjacencyDefault.empty(alphaLeft, alphaRight);

            BreakendDefault betaLeft = BreakendDefault.preciseWithRef(contig, inserted, Strand.FWD, "b_ins_intron_l", "");
            BreakendDefault betaRight = BreakendDefault.preciseWithRef(chr7, 44_196_026, Strand.FWD, "b_ins_intron_r", "c");
            AdjacencyDefault beta = AdjacencyDefault.empty(betaLeft, betaRight);

            return SequenceRearrangementDefault.of(SvType.INSERTION, alpha, beta);
        }

    }

    public static class Enhancers {
        public static Enhancer enhancer90kbUpstreamOfFBN1() {
            Contig chr15 = GENOME_ASSEMBLY.getContigByName("15").orElseThrow();
            int fbn1Tss = 48_646_788;
            int enhancerBegin = fbn1Tss + 90_000;
            int enhancerEnd = enhancerBegin + 300;
            double tau = 0.8;
            TermId skeletonId = TermId.of("UBERON:0004288");

           return new Enhancer(chr15, enhancerBegin, enhancerEnd, tau, skeletonId, "skeleton");
        }
    }

    public static class Inversions {

        public static SequenceRearrangement gckIntronic() {
            Contig chr7 = GENOME_ASSEMBLY.getContigByName("7").orElseThrow();
            int begin = 44_178_001;
            int end = 44_180_000;

            return makeInversion(chr7, begin, end);
        }

        /**
         * FBN1 is NM_000138.4  , chr15:48408306-48645788  (-)
         *  Here, we want a 100bp inversion that is 50bp upstream of the TSS in the promoter
         * @return Inversion 48bp upstream of FBN1 TSS
         */
        public static SequenceRearrangement fbn1PromoterInversion() {
            Contig chr15 = GENOME_ASSEMBLY.getContigByName("15").orElseThrow();
            int begin = 48_645_838 ;
            int end = 48_645_938;

            return makeInversion(chr15, begin, end);
        }

        /**
         * FBN1 is NM_000138.4  , chr15:48408306-48645788  (-)
         *  Here, we want a 100bp inversion that is 25000bp upstream of the TSS in the promoter
         *  This should be a LOW impact
         * @return Inversion 25000bp upstream of FBN1 TSS
         */
        public static SequenceRearrangement fbn1UpstreamInversion() {
            Contig chr15 = GENOME_ASSEMBLY.getContigByName("15").orElseThrow();
            int TSS = 48_645_788;
            int begin = TSS + 25_000; ;
            int end = begin + 300;

            return makeInversion(chr15, begin, end);
        }

        /**
         * Simulate the case where there is an inversion of the entire gene. The gene
         * itself is not disrupted, but there is an enhancer that was 90kb upstream of
         * the gene that is now more distant from the promoter -- a possible regulatory
         * mutation.
         * FBN1 is NM_000138.4  , chr15:48408306-48645788  (-)
         * @return
         */
        public static SequenceRearrangement fbn1WholeGeneEnhancerAt90kb() {
            Contig chr15 = GENOME_ASSEMBLY.getContigByName("15").orElseThrow();
            int begin = 48_407_306 ;
            int end = 48_646_788;

            return makeInversion(chr15, begin, end);
        }


        /**
         * Inversion that disrupts the sequence of this enhancer
         * chr20	51642723	51642826	0.557366	UBERON:0000955	brain	HP:0012443	Abnormality of brain morphology
         */
        public static SequenceRearrangement brainEnhancerDisruptedByInversion() {
            Contig chr20 = GENOME_ASSEMBLY.getContigByName("20").orElseThrow();
            int begin = 51_642_780 ;
            int end = 51_642_880;
            return makeInversion(chr20, begin, end);
        }



        /**
         * This inversion affects part of exon 2 and intron 1. The <em>GCK</em> gene is on REV strand.
         *
         * <p>
         * GCK:NM_000162 upstream, 200b inversion
         * chr7:44_153_401-44_153_600
         *
         * @return inversion affecting an exon of GCK
         */
        public static StructuralVariant gckExonic() {
            Contig chr7 = GENOME_ASSEMBLY.getContigByName("7").orElseThrow();
            int begin = 44_153_401;
            int end = 44_153_600;

            return makeInversion(chr7, begin, end);
        }

        private static StructuralVariant makeInversion(Contig contig, int begin, int end) {
            int alphaRightPos = contig.getLength() - end + 1;
            int betaLeftPos = contig.getLength() - begin + 1;

            Breakend alphaLeft = BreakendDefault.precise(contig, begin - 1, Strand.FWD, "alphaLeft");
            Breakend alphaRight = BreakendDefault.precise(contig, alphaRightPos, Strand.REV, "alphaRight");
            Adjacency alpha = AdjacencyDefault.empty(alphaLeft, alphaRight);

            Breakend betaLeft = BreakendDefault.precise(contig, betaLeftPos, Strand.REV, "betaLeft");
            Breakend betaRight = BreakendDefault.precise(contig, end + 1, Strand.FWD, "betaRight");
            Adjacency beta = AdjacencyDefault.empty(betaLeft, betaRight);

            return StructuralVariantDefault.of(SvType.INVERSION, alpha, beta);
        }

    }

    public static class Translocations {

        /**
         * Translocation where one CDS is disrupted and the other is not
         * <p>
         * left mate, SURF2:NM_017503.5 intron 3 (disrupted CDS)
         * chr9:133_359_000 (+)
         * right mate, upstream from BRCA2 (not disrupted)
         * chr13:32_300_000 (+)
         */
        public static StructuralVariant translocationWhereOneCdsIsDisruptedAndTheOtherIsNot() {
            Contig chr9 = GENOME_ASSEMBLY.getContigByName("9").orElseThrow();
            BreakendDefault left = BreakendDefault.preciseWithRef(chr9, 133_359_000, Strand.FWD, "tra_l", "G");
            Contig chr13 = GENOME_ASSEMBLY.getContigByName("13").orElseThrow();
            BreakendDefault right = BreakendDefault.preciseWithRef(chr13, 32_300_000, Strand.FWD, "tra_r", "A");

            return StructuralVariantDefault.of(SvType.TRANSLOCATION, AdjacencyDefault.empty(left, right));
        }

    }


}
