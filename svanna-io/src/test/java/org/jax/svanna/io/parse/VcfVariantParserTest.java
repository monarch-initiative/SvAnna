package org.jax.svanna.io.parse;

import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFCodec;
import htsjdk.variant.vcf.VCFFileReader;
import htsjdk.variant.vcf.VCFHeaderVersion;
import org.jax.svanna.core.reference.SvannaVariant;
import org.jax.svanna.core.reference.Zygosity;
import org.jax.svanna.io.FullSvannaVariant;
import org.jax.svanna.io.TestDataConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.svart.*;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static java.util.stream.Collectors.toSet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@SpringBootTest(classes = TestDataConfig.class)
public class VcfVariantParserTest {

    private static final Path SV_EXAMPLE_PATH = Paths.get("src/test/resources/org/jax/svanna/io/parse/sv_example.vcf");
    private static final VCFCodec VCF_CODEC = new VCFCodec();

    @BeforeAll
    public static void beforeAll() {
        try (VCFFileReader reader = new VCFFileReader(SV_EXAMPLE_PATH, false)) {
            VCF_CODEC.setVCFHeader(reader.getFileHeader(), VCFHeaderVersion.VCF4_3);
        }
    }

    @Nested
    @DisplayName("Tests that use real-life VCF files and real genome assembly - GRCh38p13")
    public class RealLife {

        private final GenomicAssembly GRCh38p13 = GenomicAssemblies.GRCh38p13();


        @Test
        public void createVariantList() throws Exception {
            VcfVariantParser instance = new VcfVariantParser(GRCh38p13);

            List<? extends Variant> variants = instance.createVariantAlleleList(SV_EXAMPLE_PATH);

            assertThat(variants, hasSize(12));

            Set<BreakendVariant> translocations = variants.stream()
                    .filter(Variant::isBreakend)
                    .map(v -> ((BreakendVariant) v))
                    .collect(toSet());
            assertThat(translocations.stream()
                            .map(bnd -> bnd.left().id())
                            .collect(toSet()),
                    hasItems("bnd_W", "bnd_V", "bnd_U", "bnd_X", "bnd_Y", "bnd_Z"));
            assertThat(translocations.stream()
                            .map(BreakendVariant::eventId)
                            .collect(toSet()),
                    hasItems("tra1", "tra2", "tra3"));

            assertThat(variants.stream()
                            .filter(variant -> variant.isSymbolic() && !variant.isBreakend())
                            .map(Variant::id)
                            .collect(toSet()),
                    hasItems("ins0", "del0", "dup0"));

            assertThat(variants.stream()
                            .filter(v -> !v.isSymbolic())
                            .map(Variant::id)
                            .collect(toSet()),
                    hasItems("rs6054257", "microsat1"));
        }

        @Test
        public void createVariantList_Pbsv() throws Exception{
            VcfVariantParser instance = new VcfVariantParser(GRCh38p13);

            List<FullSvannaVariant> variants = instance.createVariantAlleleList(Paths.get("src/test/resources/org/jax/svanna/io/parse/pbsv.vcf"));

            assertThat(variants, hasSize(6));
            assertThat(variants.stream().map(Variant::variantType).collect(toSet()),
                    hasItems(VariantType.DEL, VariantType.INS, VariantType.BND, VariantType.INV, VariantType.DUP, VariantType.CNV));

            // check general fields for the first variant
            // CM000663.2	367610	pbsv.DEL.1	TATTCATGCACACATGTTCAC	T	.	PASS	SVTYPE=DEL;END=367630;SVLEN=-20	GT:AD:DP	1/1:0,2:2
            SvannaVariant del = variants.get(0);
            assertThat(del.contigName(), equalTo("1"));
            assertThat(del.start(), equalTo(367_611));
            assertThat(del.end(), equalTo(367_630));
            assertThat(del.id(), equalTo("pbsv.DEL.1"));
            assertThat(del.ref(), equalTo("ATTCATGCACACATGTTCAC"));
            assertThat(del.alt(), equalTo(""));
            assertThat(del.length(), equalTo(20));
            assertThat(del.changeLength(), equalTo(-20));
            assertThat(del.variantType(), equalTo(VariantType.DEL));
            assertThat(del.isSymbolic(), equalTo(false));
            assertThat(del.zygosity(), equalTo(Zygosity.HOMOZYGOUS));
            assertThat(del.minDepthOfCoverage(), equalTo(2));
            assertThat(del.numberOfRefReads(), equalTo(0));
            assertThat(del.numberOfAltReads(),equalTo(2));

            // now check breakended bits
            // CM000663.2	13054707	pbsv.BND.CM000663.2:13054707-CM000663.2:13256071	C	C]CM000663.2:13256071]	.	PASS	SVTYPE=BND;CIPOS=0,0;MATEID=pbsv.BND.CM000663.2:13256071-CM000663.2:13054707;MATEDIST=201364	GT:AD:DP	0/1:1,1:2
            SvannaVariant breakendVariant = variants.get(2);
            assertThat(breakendVariant.variantType(), equalTo(VariantType.BND));
            assertThat(breakendVariant.isBreakend(), equalTo(true));
            assertThat(breakendVariant.contig(), equalTo(GRCh38p13.contigByName("1")));
            assertThat(breakendVariant.strand(), equalTo(Strand.POSITIVE));
            assertThat(breakendVariant.coordinateSystem(), equalTo(CoordinateSystem.oneBased()));
            assertThat(breakendVariant.start(), equalTo(13_054_707));
            assertThat(breakendVariant.end(), equalTo(13_054_707));

            BreakendVariant breakended = (BreakendVariant) breakendVariant;
            Breakend left = breakended.left();
            assertThat(left.id(), equalTo("pbsv.BND.CM000663.2:13054707-CM000663.2:13256071"));
            assertThat(left.contig(), equalTo(GRCh38p13.contigByName("CM000663.2")));
            assertThat(left.strand(), equalTo(Strand.POSITIVE));
            assertThat(left.coordinateSystem(), equalTo(CoordinateSystem.oneBased()));
            assertThat(left.start(), equalTo(13_054_708));
            assertThat(left.end(), equalTo(13_054_707));

            Breakend right = breakended.right();
            assertThat(right.id(), equalTo("pbsv.BND.CM000663.2:13256071-CM000663.2:13054707"));
            assertThat(right.contig(), equalTo(GRCh38p13.contigByName("CM000663.2")));
            assertThat(right.strand(), equalTo(Strand.NEGATIVE));
            assertThat(right.coordinateSystem(), equalTo(CoordinateSystem.oneBased()));
            assertThat(right.startOnStrand(Strand.POSITIVE), equalTo(13_256_071));
            assertThat(right.endOnStrand(Strand.POSITIVE), equalTo(13_256_070));

            // cnv bits & the fact that this CNV fails the filters
            SvannaVariant cnv = variants.get(5);
            assertThat(cnv.variantType(), equalTo(VariantType.CNV));
            assertThat(cnv.copyNumber(), equalTo(4));
            assertThat(cnv.passedFilters(), equalTo(false));
        }

        @Test
        public void createVariantList_Svim() throws Exception {
            VcfVariantParser instance = new VcfVariantParser(GRCh38p13);

            List<FullSvannaVariant> variants = instance.createVariantAlleleList(Paths.get("src/test/resources/org/jax/svanna/io/parse/svim.vcf"));

            assertThat(variants, hasSize(6));
            assertThat(variants.stream().map(Variant::variantType).collect(toSet()),
                    hasItems(VariantType.DEL, VariantType.INS, VariantType.BND, VariantType.DUP, VariantType.INV, VariantType.DUP_TANDEM));

            // check general fields for the first variant
            // CM000663.2	180188	svim.DEL.1	N	<DEL>	4	hom_ref	SVTYPE=DEL;END=180393;SVLEN=-205;SUPPORT=4;STD_SPAN=9.76;STD_POS=8.86	GT:DP:AD	0/0:48:44,4
            SvannaVariant del = variants.get(0);
            assertThat(del.contigName(), equalTo("1"));
            assertThat(del.start(), equalTo(180_189));
            assertThat(del.end(), equalTo(180_393));
            assertThat(del.id(), equalTo("svim.DEL.1"));
            assertThat(del.ref(), equalTo(""));
            assertThat(del.alt(), equalTo("<DEL>"));
            assertThat(del.length(), equalTo(205));
            assertThat(del.changeLength(), equalTo(-205));
            assertThat(del.variantType(), equalTo(VariantType.DEL));
            assertThat(del.isSymbolic(), equalTo(true));
            assertThat(del.zygosity(), equalTo(Zygosity.HOMOZYGOUS));
            assertThat(del.minDepthOfCoverage(), equalTo(48));
            assertThat(del.numberOfRefReads(), equalTo(44));
            assertThat(del.numberOfAltReads(),equalTo(4));

            // now check breakended bits
            // CM000663.2	1177318	svim.BND.3	N	N[CM000666.2:182304220[	1	PASS	SVTYPE=BND;SUPPORT=1;STD_POS1=.;STD_POS2=.	GT:DP:AD	./.:.:.,.
            SvannaVariant breakendVariant = variants.get(2);
            assertThat(breakendVariant.variantType(), equalTo(VariantType.BND));
            assertThat(breakendVariant.isBreakend(), equalTo(true));
            assertThat(breakendVariant.contig(), equalTo(GRCh38p13.contigByName("CM000663.2")));
            assertThat(breakendVariant.strand(), equalTo(Strand.POSITIVE));
            assertThat(breakendVariant.coordinateSystem(), equalTo(CoordinateSystem.oneBased()));
            assertThat(breakendVariant.start(), equalTo(1_177_318));
            assertThat(breakendVariant.end(), equalTo(1_177_318));

            BreakendVariant breakended = (BreakendVariant) breakendVariant;
            Breakend left = breakended.left();
            assertThat(left.id(), equalTo("svim.BND.3"));
            assertThat(left.contig(), equalTo(GRCh38p13.contigByName("CM000663.2")));
            assertThat(left.strand(), equalTo(Strand.POSITIVE));
            assertThat(left.coordinateSystem(), equalTo(CoordinateSystem.oneBased()));
            assertThat(left.start(), equalTo(1_177_319));
            assertThat(left.end(), equalTo(1_177_318));

            Breakend right = breakended.right();
            assertThat(right.id(), equalTo(""));
            assertThat(right.contigName(), equalTo("4"));
            assertThat(left.strand(), equalTo(Strand.POSITIVE));
            assertThat(left.coordinateSystem(), equalTo(CoordinateSystem.oneBased()));
            assertThat(right.start(), equalTo(182_304_220));
            assertThat(right.end(), equalTo(182_304_219));
        }

        @Test
        public void createVariantList_Sniffles() throws Exception {
            VcfVariantParser instance = new VcfVariantParser(GRCh38p13);

            List<FullSvannaVariant> variants = instance.createVariantAlleleList(Paths.get("src/test/resources/org/jax/svanna/io/parse/sniffles.vcf"));

            assertThat(variants, hasSize(6));
            assertThat(variants.stream().map(Variant::variantType).collect(toSet()),
                    hasItems(VariantType.DEL, VariantType.DUP, VariantType.INV, VariantType.INS, VariantType.INS, VariantType.SYMBOLIC)); // INVDUP -> SYMBOLIC

            // check general fields for the first variant
            // CM000663.2	1366938	1	N	<DEL>	.	PASS	IMPRECISE;SVMETHOD=Snifflesv1.0.12;CHR2=CM000663.2;END=1367108;ZMW=9;STD_quant_start=11.333333;STD_quant_stop=10.000000;Kurtosis_quant_start=6.000000;Kurtosis_quant_stop=6.000000;SVTYPE=DEL;SUPTYPE=AL;SVLEN=-170;STRANDS=+-;STRANDS2=4,5,4,5;RE=9;REF_strand=0,0;Strandbias_pval=1;AF=1	GT:DR:DV	1/1:0:9
            SvannaVariant del = variants.get(0);
            assertThat(del.contigName(), equalTo("1"));
            assertThat(del.start(), equalTo(1_366_939));
            assertThat(del.end(), equalTo(1_367_108));
            assertThat(del.id(), equalTo("1"));
            assertThat(del.ref(), equalTo(""));
            assertThat(del.alt(), equalTo("<DEL>"));
            assertThat(del.length(), equalTo(170));
            assertThat(del.changeLength(), equalTo(-170));
            assertThat(del.variantType(), equalTo(VariantType.DEL));
            assertThat(del.isSymbolic(), equalTo(true));
            assertThat(del.zygosity(), equalTo(Zygosity.HOMOZYGOUS));
            assertThat(del.minDepthOfCoverage(), equalTo(9));
            assertThat(del.numberOfRefReads(), equalTo(0));
            assertThat(del.numberOfAltReads(),equalTo(9));

            // Check that bad INS coordinates in Sniffles are fixed
            SvannaVariant ins = variants.get(4);
            assertThat(ins.contigName(), equalTo("1"));
            assertThat(ins.strand(), equalTo(Strand.POSITIVE));
            assertThat(ins.coordinateSystem(), equalTo(CoordinateSystem.oneBased()));
            assertThat(ins.start(), equalTo(240_229_701));
            assertThat(ins.end(), equalTo(240_229_700));
        }


        @Test
        public void toVariants_multiallelicBreakendVariant() {
            VcfVariantParser instance = new VcfVariantParser(GRCh38p13);

            String line = "2\t321681\tbnd_W\tG\tG]17:198982],C\t6\tPASS\tSVTYPE=BND;MATEID=bnd_Y;EVENT=tra1\tGT\t./.";
            VariantContext vc = VCF_CODEC.decode(line);
            Optional<? extends Variant> vo = instance.toVariants().apply(vc);

            assertThat(vo.isPresent(), is(false));
        }

        @Test
        public void toVariants_multiallelicSymbolicVariant() {
            VcfVariantParser instance = new VcfVariantParser(GRCh38p13);

            String line = "2\t321682\tdel0\tT\t<DEL>,C\t6\tPASS\tSVTYPE=DEL;END=321887;SVLEN=-205;CIPOS=-56,20;CIEND=-10,62\tGT:GQ:DP\t0/1:12:11";
            VariantContext vc = VCF_CODEC.decode(line);
            Optional<? extends Variant> vo = instance.toVariants().apply(vc);

            assertThat(vo.isPresent(), is(false));
        }

        @Test
        public void toVariants_symbolic_unknownContig() {
            VcfVariantParser instance = new VcfVariantParser(GRCh38p13);

            String line = "bacon\t12665100\tdup0\tA\t<DUP>\t14\tPASS\tSVTYPE=DUP;END=12686200;SVLEN=21100;CIPOS=-500,500;CIEND=-500,500;DP=5\tGT:GQ:CN:CNQ\t./.:0:3:16.2";
            VariantContext vc = VCF_CODEC.decode(line);
            Optional<? extends Variant> variants = instance.toVariants().apply(vc);

            assertThat(variants.isPresent(), is(false));
        }

        @Test
        public void toVariants_sequence_unknownContig() {
            VcfVariantParser instance = new VcfVariantParser(GRCh38p13);

            String line = "bacon\t14370\trs6054257\tG\tA\t29\tPASS\tDP=14;AF=0.5;DB\tGT:GQ:DP\t1/1:43:5";
            VariantContext vc = VCF_CODEC.decode(line);
            Optional<? extends Variant> vo = instance.toVariants().apply(vc);

            assertThat(vo.isPresent(), is(false));
        }
    }

    @Nested
    @DisplayName("Test parsing code with easy-to-compute coordinates")
    public class ToyTests {

        @Test
        public void toVariants_symbolicDeletion() {
            GenomicAssembly assembly = testAssembly(List.of(TestContig.of(1, 10), TestContig.of(2, 20)));
            VcfVariantParser parser = new VcfVariantParser(assembly);

            String line = "1\t2\tdel0\tT\t<DEL>\t6\tPASS\tSVTYPE=DEL;END=7;SVLEN=-5;CIPOS=-1,2;CIEND=-2,1\tGT:GQ:DP:AD\t0/1:12:11:6,5";

            VariantContext vc = VCF_CODEC.decode(line);
            Optional<? extends SvannaVariant> vo = parser.toVariants().apply(vc);
            assertThat(vo.isPresent(), equalTo(true));

            SvannaVariant variant = vo.get();
            assertThat(variant.contigName(), equalTo("1"));
            assertThat(variant.start(), equalTo(3));
            assertThat(variant.coordinates().startConfidenceInterval(), equalTo(ConfidenceInterval.of(-1, 2)));
            assertThat(variant.end(), equalTo(7));
            assertThat(variant.coordinates().endConfidenceInterval(), equalTo(ConfidenceInterval.of(-2, 1)));

            assertThat(variant.id(), equalTo("del0"));
            assertThat(variant.strand(), equalTo(Strand.POSITIVE));
            assertThat(variant.coordinateSystem(), equalTo(CoordinateSystem.oneBased()));
            assertThat(variant.variantType(), equalTo(VariantType.DEL));

            assertThat(variant.length(), equalTo(5));
            assertThat(variant.changeLength(), equalTo(-5));

            assertThat(variant.ref(), equalTo(""));
            assertThat(variant.alt(), equalTo("<DEL>"));

            assertThat(variant.zygosity(), equalTo(Zygosity.HETEROZYGOUS));
            assertThat(variant.minDepthOfCoverage(), equalTo(11));
            assertThat(variant.numberOfRefReads(), equalTo(6));
            assertThat(variant.numberOfAltReads(), equalTo(5));
        }

        @Test
        public void toVariants_sequenceVariant() {
            GenomicAssembly assembly = testAssembly(List.of(TestContig.of(1, 10), TestContig.of(2, 20)));
            VcfVariantParser parser = new VcfVariantParser(assembly);

            String line = "1\t2\tdel1\tTTC\tTT\t6\tPASS\tAF=0.5\tGT:GQ:DP:AD\t0/1:12:11:6,5";
            VariantContext vc = VCF_CODEC.decode(line);
            Optional<? extends SvannaVariant> vo = parser.toVariants().apply(vc);

            assertThat(vo.isPresent(), equalTo(true));

            SvannaVariant variant = vo.get();
            assertThat(variant.contig(), equalTo(assembly.contigByName("1")));
            assertThat(variant.start(), equalTo(4));
            assertThat(variant.end(), equalTo(4));

            assertThat(variant.id(), equalTo("del1"));
            assertThat(variant.strand(), equalTo(Strand.POSITIVE));
            assertThat(variant.coordinateSystem(), equalTo(CoordinateSystem.oneBased()));
            assertThat(variant.variantType(), equalTo(VariantType.DEL));

            assertThat(variant.length(), equalTo(1));
            assertThat(variant.changeLength(), equalTo(-1));

            assertThat(variant.ref(), equalTo("C"));
            assertThat(variant.alt(), equalTo(""));

            assertThat(variant.zygosity(), equalTo(Zygosity.HETEROZYGOUS));
            assertThat(variant.minDepthOfCoverage(), equalTo(11));
            assertThat(variant.numberOfRefReads(), equalTo(6));
            assertThat(variant.numberOfAltReads(), equalTo(5));
        }

        @Test
        public void toVariants_breakendVariant() {
            GenomicAssembly assembly = testAssembly(List.of(TestContig.of(1, 10), TestContig.of(2, 20)));
            VcfVariantParser parser = new VcfVariantParser(assembly);

            String line = "1\t3\tbnd_V\tT\t]2:16]AAGT\t6\tPASS\tSVTYPE=BND;CIPOS=-1,2;CIEND=-2,1;MATEID=bnd_U;EVENT=tra2\tGT\t./.";
            VariantContext vc = VCF_CODEC.decode(line);
            Optional<? extends SvannaVariant> vo = parser.toVariants().apply(vc);

            // variant bits
            assertThat(vo.isPresent(), equalTo(true));

            SvannaVariant variant = vo.get();
            assertThat(variant.contig(), equalTo(assembly.contigByName("1")));
            assertThat(variant.start(), equalTo(7));
            assertThat(variant.coordinates().startConfidenceInterval(), equalTo(ConfidenceInterval.of(-2, 1)));
            assertThat(variant.end(), equalTo(7));
            assertThat(variant.coordinates().endConfidenceInterval(), equalTo(ConfidenceInterval.of(-2, 1)));

            assertThat(variant.id(), equalTo("bnd_V"));
            assertThat(variant.strand(), equalTo(Strand.NEGATIVE));
            assertThat(variant.coordinateSystem(), equalTo(CoordinateSystem.oneBased()));
            assertThat(variant.variantType(), equalTo(VariantType.BND));

            assertThat(variant.length(), equalTo(1));
            assertThat(variant.changeLength(), equalTo(3));

            assertThat(variant.ref(), equalTo("A"));
            assertThat(variant.alt(), equalTo("CTT"));

            assertThat(variant.zygosity(), equalTo(Zygosity.UNKNOWN));
            assertThat(variant.minDepthOfCoverage(), equalTo(-1));

            // breakend bits
            assertThat(variant.isBreakend(), equalTo(true));
            BreakendVariant bnd = (BreakendVariant) variant;
            assertThat(bnd.eventId(), equalTo("tra2"));

            Breakend left = bnd.left();
            assertThat(left.id(), equalTo("bnd_V"));
            assertThat(left.contig(), equalTo(assembly.contigByName("1")));
            assertThat(left.strand(), equalTo(Strand.NEGATIVE));
            assertThat(left.coordinateSystem(), equalTo(CoordinateSystem.FULLY_CLOSED));
            assertThat(left.start(), equalTo(8));
            assertThat(left.coordinates().startConfidenceInterval(), equalTo(ConfidenceInterval.of( -2, 1)));
            assertThat(left.end(), equalTo(7));
            assertThat(left.coordinates().endConfidenceInterval(), equalTo(ConfidenceInterval.of( -2, 1)));

            Breakend right = bnd.right();
            assertThat(right.id(), equalTo("bnd_U"));
            assertThat(right.contig(), equalTo(assembly.contigByName("2")));
            assertThat(left.strand(), equalTo(Strand.NEGATIVE));
            assertThat(left.coordinateSystem(), equalTo(CoordinateSystem.FULLY_CLOSED));
            assertThat(right.start(), equalTo(6));
            assertThat(right.coordinates().startConfidenceInterval(), equalTo(ConfidenceInterval.of(-1, 2)));
            assertThat(right.end(), equalTo(5));
            assertThat(right.coordinates().endConfidenceInterval(), equalTo(ConfidenceInterval.of( -1, 2)));
        }
    }

    private static GenomicAssembly testAssembly(List<Contig> contigs) {
        return GenomicAssembly.of("toy", "Wookie", "9999", "Han Solo", "2100-01-01",
                "GB1", "RS1", contigs);
    }
}