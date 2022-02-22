package org.jax.svanna.test;


import org.monarchinitiative.svart.*;
import org.monarchinitiative.svart.assembly.GenomicAssembly;

// Let's keep the variant declarations within the test class even at the cost of code duplication.
// The in situ variant declarations improve tests' independence
@Deprecated
public class TestVariants {

    private final GenomicAssembly assembly;
    private final Insertions insertions;
    private final Deletions deletions;
    private final Inversions inversions;
//    private final Enhancers enhancers;
    private final Translocations translocations;

    public TestVariants(GenomicAssembly assembly) {
        this.assembly = assembly;
        insertions = new Insertions();
        deletions = new Deletions();
        inversions = new Inversions();
//        enhancers = new Enhancers();
        translocations = new Translocations();
    }

    public Insertions insertions() {
        return insertions;
    }

    public Deletions deletions() {
        return deletions;
    }

    public Translocations translocations() {
        return translocations;
    }

//    public Enhancers enhancers() {
//        return enhancers;
//    }

    public Inversions inversions() {
        return inversions;
    }

    public class Deletions {
        /**
         * Deletion upstream intergenic | GCK.
         * <p>
         * GCK:NM_000162 upstream, 200b deletion
         * chr7:44_189_901-44_190_100
         */
        public GenomicVariant gckUpstreamIntergenic_affectingEnhancer() {
            Contig chr7 = assembly.contigByName("7");
            return GenomicVariant.of(chr7, "gck_del_upstream_intergenic_enhancer", Strand.POSITIVE, CoordinateSystem.oneBased(), 44_189_900, 44_190_100, "N", "<DEL>", -200);
        }


        /**
         * Deletion upstream intergenic | GCK.
         * <p>
         * GCK:NM_000162 upstream, 1kb deletion
         * chr7:44_191_001-44_192_000
         */
        public GenomicVariant gckUpstreamIntergenic_NotAffectingEnhancer() {
            Contig chr7 = assembly.contigByName("7");
            return GenomicVariant.of(chr7, "gck_del_upstream_intergenic", Strand.POSITIVE, CoordinateSystem.oneBased(), 44_191_000, 44_192_000, "N", "<DEL>", -1_000);
        }

        /**
         * Deletion upstream intergenic | GCK.
         * <p>
         * GCK:NM_000162 upstream, 1kb deletion
         * chr7:44_191_001-44_192_000
         */
        public GenomicVariant gckUpstreamIntergenic_affectingPhenotypicallyNonrelevantEnhancer() {
            Contig chr7 = assembly.contigByName("7");
            return GenomicVariant.of(chr7, "gck_del_upstream_intergenic_phenotypically_nonrelevant_enhancer", Strand.POSITIVE, CoordinateSystem.oneBased(), 44_194_500, 44_195_500, "N", "<DEL>", -1_000);
        }

        /**
         * Single exon deletion
         * <p>
         * SURF2:NM_017503.5 deletion of exon 3, tx on (+) strand
         * chr9:133_357_501-133_358_000
         */
        public GenomicVariant surf2singleExon_exon3() {
            Contig chr9 = assembly.contigByName("9");
            return GenomicVariant.of(chr9, "single_exon_del", Strand.POSITIVE, CoordinateSystem.oneBased(), 133_357_500, 133_358_000, "N", "<DEL>", -500);
        }


        /**
         * Two exon deletion.
         * <p>
         * SURF1:NM_003172.4 deletion of exons 6 and 7, tx on (-) strand
         * chr9:133_352_301-133_352_900
         */
        public GenomicVariant surf1TwoExon_exons_6_and_7() {
            Contig chr9 = assembly.contigByName("9");
            return GenomicVariant.of(chr9, "two_exon_del", Strand.POSITIVE, CoordinateSystem.oneBased(), 133_352_300, 133_352_900, "N", "<DEL>", -600);
        }

        /**
         * Single exon deletion.
         * <p>
         * SURF1:NM_003172.4 deletion of the exon 2, tx on (-) strand
         * chr9:133_356_251-133_356_350
         */
        public GenomicVariant surf1SingleExon_exon2() {
            Contig chr9 = assembly.contigByName("9");
            return GenomicVariant.of(chr9, "surf1_exon2_del", Strand.POSITIVE, CoordinateSystem.oneBased(), 133_356_250, 133_356_350, "N", "<DEL>", -100);
        }

        /**
         * Deletion of one entire transcript and part of another.
         * <p>
         * SURF1:NM_003172.4 entirely deleted, SURF2:NM_017503.5 partially deleted
         * chr9:133_350_001-133_358_000
         */
        public GenomicVariant surf1Surf2oneEntireTranscriptAndPartOfAnother() {
            Contig chr9 = assembly.contigByName("9");
            return GenomicVariant.of(chr9, "entire_tx_del",Strand.POSITIVE, CoordinateSystem.oneBased(),  133_350_000, 133_358_000, "N", "<DEL>", -8_000);
        }


        /**
         * Deletion within an intron.
         * <p>
         * SURF2:NM_017503.5 700bp deletion within intron 3
         * chr9:133_359_001-133_359_700
         */
        public GenomicVariant surf2WithinAnIntron() {
            Contig chr9 = assembly.contigByName("9");
            return GenomicVariant.of(chr9, "del_within_intron", Strand.POSITIVE, CoordinateSystem.oneBased(), 133_359_000, 133_359_700, "N", "<DEL>", -700);
        }

        /**
         * ZBTB48, is forward strand. Test a variant in intron 1, i.e.,  between	6_580_137-6_580_540
         */
        public GenomicVariant zbtb48intron1() {
            Contig chr1 = assembly.contigByName("1");
            return GenomicVariant.of(chr1, "del_within_intron", Strand.POSITIVE, CoordinateSystem.oneBased(), 6_580_300, 6_580_400, "N", "<DEL>", -100);
        }

        /**
         * Deletion in 5UTR.
         * <p>
         * SURF2:NM_017503.5 20bp deletion in 5UTR
         * chr9:133_356_561-133_356_580
         */
        public GenomicVariant surf2In5UTR() {
            Contig chr9 = assembly.contigByName("9");
            return GenomicVariant.of(chr9, "del_in_5utr", Strand.POSITIVE, CoordinateSystem.oneBased(), 133_356_560, 133_356_580, "N", "<DEL>", -20);
        }

        /**
         * Deletion in 3UTR.
         * <p>
         * SURF1:NM_003172.4 100bp deletion in 3UTR
         * chr9:133_351_801-133_351_900
         */
        public GenomicVariant surf1In3UTR() {
            Contig chr9 = assembly.contigByName("9");
            return GenomicVariant.of(chr9, "del_in_3utr", Strand.POSITIVE, CoordinateSystem.oneBased(), 133_351_800, 133_351_900, "N", "<DEL>", -100);
        }

        /**
         * Deletion downstream intergenic.
         * <p>
         * SURF1:NM_003172.4 downstream, 10kb deletion
         * chr9:133_300_001-133_310_000
         */
        public GenomicVariant surf1DownstreamIntergenic() {
            Contig chr9 = assembly.contigByName("9");
            return GenomicVariant.of(chr9, "del_downstream_intergenic", Strand.POSITIVE, CoordinateSystem.oneBased(), 133_300_000, 133_310_000, "N", "<DEL>", -10_000);
        }

        /**
         * Deletion upstream intergenic.
         * <p>
         * hg38 chr15:48,408,306-48,645,849 Size: 237,544 Total Exon Count: 66 Strand: -
         * upstream, 10kb deletion
         * chr15:48_655_000-48_665_000
         */
        public GenomicVariant brca2UpstreamIntergenic() {
            Contig chr15 = assembly.contigByName("15");
            return GenomicVariant.of(chr15, "del_upstream_intergenic", Strand.POSITIVE, CoordinateSystem.oneBased(), 48_655_000, 48_665_000, "N", "<DEL>", -10_000);
        }
    }

    public class Insertions {
        /**
         * Insertion in 5'UTR.
         * <p>
         * SURF2:NM_017503.5 10bp insertion in 5UTR
         * chr9:133_356_571-133_356_571
         */
        public GenomicVariant surf2InsertionIn5UTR() {
            Contig chr9 = assembly.contigByName("9");
            return GenomicVariant.of(chr9, "ins_5utr",Strand.POSITIVE, CoordinateSystem.oneBased(),  133_356_571, 133_356_572, "N", "<INS>", 10);
        }

        /**
         * Insertion in 3'UTR
         * <p>
         * SURF1:NM_003172.4 10bp insertion in 3UTR
         * chr9:133_351_851-133_351_851
         */
        public GenomicVariant surf1InsertionIn3UTR() {
            Contig chr9 = assembly.contigByName("9");
            return GenomicVariant.of(chr9, "ins_3utr", Strand.POSITIVE, CoordinateSystem.oneBased(), 133_351_851, 133_351_852, "N", "<INS>", 10);
        }

        /**
         * Insertion in exon.
         * <p>
         * SURF2:NM_017503.5 10bp insertion in exon 4
         * chr9:133_360_001-133_360_001
         */
        public GenomicVariant surf2Exon4() {
            Contig chr9 = assembly.contigByName("9");
            return GenomicVariant.of(chr9, "ins_3utr", Strand.POSITIVE, CoordinateSystem.oneBased(), 133_360_001, 133_360_002, "N", "<INS>", 10);
        }


        /**
         * Insertion in intron.
         * <p>
         * SURF2:NM_017503.5 10bp insertion in intron 3
         * chr9:133_359_001-133_359_001
         */
        public GenomicVariant surf2Intron3() {
            Contig chr9 = assembly.contigByName("9");
            return GenomicVariant.of(chr9, "ins_intron", Strand.POSITIVE, CoordinateSystem.oneBased(), 133_359_001, 133_359_002, "N", "<INS>", 10);
        }


        /**
         * Insertion in GCK enhancer.
         * <p>
         * GCK:NM_000162 upstream, 200b insertion
         * chr7:44_190_025-44_190_026
         */
        public GenomicVariant gckRelevantEnhancer() {
            int inserted = 200;
            Contig chr7 = assembly.contigByName("7");
            return GenomicVariant.of(chr7, "ins_intron",Strand.POSITIVE, CoordinateSystem.oneBased(),  44_190_025, 44_190_026, "N", "<INS>", inserted);
        }

        /**
         * Insertion in GCK enhancer.
         * <p>
         * GCK:NM_000162 upstream, 200b insertion
         * chr7:44_195_025-44_195_026
         */
        public GenomicVariant gckNonRelevantEnhancer() {
            int inserted = 200;
            Contig chr7 = assembly.contigByName("7");
            return GenomicVariant.of(chr7, "ins_intron", Strand.POSITIVE, CoordinateSystem.oneBased(), 44_195_025, 44_195_026, "N", "<INS>", inserted);
        }

        /**
         * Insertion in GCK intergenic region.
         * <p>
         * GCK:NM_000162 upstream, 200b insertion
         * chr7:44_196_025-44_196_026
         */
        public GenomicVariant gckIntergenic() {
            int inserted = 200;
            Contig chr7 = assembly.contigByName("7");
            return GenomicVariant.of(chr7, "ins_intron", Strand.POSITIVE, CoordinateSystem.oneBased(), 44_196_025, 44_196_026, "N", "<INS>", inserted);
        }

    }

//    public class Enhancers {
//        public Enhancer enhancer90kbUpstreamOfFBN1() {
//            Contig chr15 = assembly.contigByName("15");
//            int fbn1Tss = 48_646_788;
//            int enhancerBegin = fbn1Tss + 90_000;
//            int enhancerEnd = enhancerBegin + 300;
//            double tau = 0.8;
//            TermId skeletonId = TermId.of("UBERON:0004288");
//
//            return Enhancer.of(chr15, Strand.POSITIVE, CoordinateSystem.ZERO_BASED, Position.of(enhancerBegin), Position.of(enhancerEnd), tau, skeletonId, "skeleton");
//        }
//    }

    public class Inversions {

        public GenomicVariant gckIntronic() {
            Contig chr7 = assembly.contigByName("7");
            int begin = 44_178_001;
            int end = 44_180_000;

            return makeInversion(chr7, begin, end);
        }

        /**
         * FBN1 is NM_000138.4  , chr15:48408306-48645788  (-)
         * Here, we want a 100bp inversion that is 50bp upstream of the TSS in the promoter
         *
         * @return Inversion 48bp upstream of FBN1 TSS
         */
        public GenomicVariant fbn1PromoterInversion() {
            Contig chr15 = assembly.contigByName("15");
            int begin = 48_645_838;
            int end = 48_645_938;

            return makeInversion(chr15, begin, end);
        }

        /**
         * FBN1 is NM_000138.4  , chr15:48408306-48645788  (-)
         * Here, we want a 100bp inversion that is 25000bp upstream of the TSS in the promoter
         * This should be a LOW impact
         *
         * @return Inversion 25000bp upstream of FBN1 TSS
         */
        public GenomicVariant fbn1UpstreamInversion() {
            Contig chr15 = assembly.contigByName("15");
            int TSS = 48_645_788;
            int begin = TSS + 25_000;
            int end = begin + 300;

            return makeInversion(chr15, begin, end);
        }

        /**
         * Simulate the case where there is an inversion of the entire gene. The gene
         * itself is not disrupted, but there is an enhancer that was 90kb upstream of
         * the gene that is now more distant from the promoter -- a possible regulatory
         * mutation.
         * FBN1 is NM_000138.4  , chr15:48408306-48645788  (-)
         *
         * @return
         */
        public GenomicVariant fbn1WholeGeneEnhancerAt90kb() {
            Contig chr15 = assembly.contigByName("15");
            int begin = 48_407_306;
            int end = 48_646_788;

            return makeInversion(chr15, begin, end);
        }


        /**
         * Inversion that disrupts the sequence of this enhancer
         * chr20	51642723	51642826	0.557366	UBERON:0000955	brain	HP:0012443	Abnormality of brain morphology
         */
        public GenomicVariant brainEnhancerDisruptedByInversion() {
            Contig chr20 = assembly.contigByName("20");
            int begin = 51_642_780;
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
        public GenomicVariant gckExonic() {
            Contig chr7 = assembly.contigByName("7");
            int begin = 44_153_401;
            int end = 44_153_600;

            return makeInversion(chr7, begin, end);
        }

        private GenomicVariant makeInversion(Contig contig, int begin, int end) {
            return GenomicVariant.of(contig, "inversion", Strand.POSITIVE, CoordinateSystem.oneBased(), begin, end, "N", "<INV>", 0);
        }

    }

    public class Translocations {

        /**
         * Translocation where one CDS is disrupted and the other is not
         * <p>
         * left mate, SURF2:NM_017503.5 intron 3 (disrupted CDS)
         * chr9:133_359_000 (+)
         * right mate, upstream from BRCA2 (not disrupted)
         * chr13:32_300_000 (+)
         */
        public GenomicBreakendVariant translocationWhereOneCdsIsDisruptedAndTheOtherIsNot() {
            Contig chr9 = assembly.contigByName("9");
            GenomicBreakend left = GenomicBreakend.of(chr9, "tra_l", Strand.POSITIVE, CoordinateSystem.oneBased(), 133_359_001, 133_359_000);
            Contig chr13 = assembly.contigByName("13");
            GenomicBreakend right = GenomicBreakend.of(chr13, "tra_r", Strand.POSITIVE, CoordinateSystem.oneBased(), 32_300_001, 32_300_000);

            return GenomicBreakendVariant.of("translocation_where_one_cds_is_disrupted_and_the_other_is_not", left, right, "G", "");
        }

    }


}
