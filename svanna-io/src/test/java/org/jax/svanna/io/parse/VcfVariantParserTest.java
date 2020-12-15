package org.jax.svanna.io.parse;

import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFCodec;
import htsjdk.variant.vcf.VCFFileReader;
import htsjdk.variant.vcf.VCFHeaderVersion;
import org.jax.svanna.core.reference.SvannaVariant;
import org.jax.svanna.core.reference.Zygosity;
import org.jax.svanna.io.TestDataConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.variant.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@SpringBootTest(classes = {TestDataConfig.class})
public class VcfVariantParserTest {

    private static final VCFCodec VCF_CODEC = new VCFCodec();
    private static final boolean REQUIRE_INDEX = false;
    private static final Path SV_EXAMPLE_PATH = Paths.get("src/test/resources/org/jax/svanna/io/parse/sv_example.vcf");

    @Autowired
    public GenomicAssembly assembly;

    private VcfVariantParser parser;

    @BeforeAll
    public static void beforeAll() {
        try (VCFFileReader reader = new VCFFileReader(SV_EXAMPLE_PATH, REQUIRE_INDEX)) {
            VCF_CODEC.setVCFHeader(reader.getFileHeader(), VCFHeaderVersion.VCF4_3);
        }
    }

    @BeforeEach
    public void setUp() {
        parser = new VcfVariantParser(assembly, REQUIRE_INDEX);
    }

    @Test
    public void createVariantList() throws Exception {
        List<? extends Variant> variants = parser.createVariantAlleleList(SV_EXAMPLE_PATH);

        assertThat(variants, hasSize(12));

        Set<Breakended> translocations = variants.stream()
                .filter(v -> v instanceof Breakended)
                .map(v -> ((Breakended) v))
                .collect(Collectors.toSet());
        assertThat(translocations.stream()
                        .map(bnd -> bnd.left().id())
                        .collect(Collectors.toSet()),
                hasItems("bnd_W", "bnd_V", "bnd_U", "bnd_X", "bnd_Y", "bnd_Z"));
        assertThat(translocations.stream()
                        .map(Breakended::eventId)
                        .collect(Collectors.toSet()),
                hasItems("tra1", "tra2", "tra3"));

        assertThat(variants.stream()
                        .filter(variant -> variant.isSymbolic() && !(variant instanceof Breakended))
                        .map(Variant::id)
                        .collect(Collectors.toSet()),
                hasItems("ins0", "del0", "dup0"));

        assertThat(variants.stream()
                        .filter(v -> !v.isSymbolic())
                        .map(Variant::id)
                        .collect(Collectors.toSet()),
                hasItems("rs6054257", "microsat1"));
    }

    @Test
    public void createVariantList_Pbsv() throws Exception{
        List<SvannaVariant> variants = parser.createVariantAlleleList(Paths.get("src/test/resources/org/jax/svanna/io/parse/pbsv.vcf"));

        assertThat(variants.size(), equalTo(6));

        // check general fields for the first variant
        SvannaVariant del = variants.get(0);
        assertThat(del.contigName(), equalTo("1"));
        assertThat(del.start(), equalTo(367_610));
        assertThat(del.end(), equalTo(367_630));
        assertThat(del.id(), equalTo("pbsv.DEL.1"));
        assertThat(del.ref(), equalTo("TATTCATGCACACATGTTCAC"));
        assertThat(del.alt(), equalTo("T"));
        assertThat(del.length(), equalTo(21));
        assertThat(del.refLength(), equalTo(21));
        assertThat(del.changeLength(), equalTo(-20));
        assertThat(del.variantType(), equalTo(VariantType.DEL));
        assertThat(del.isSymbolic(), equalTo(false));
        assertThat(del.zygosity(), equalTo(Zygosity.HOMOZYGOUS));
        assertThat(del.minDepthOfCoverage(), equalTo(2));
        assertThat(del.numberOfRefReads(), equalTo(0));
        assertThat(del.numberOfAltReads(),equalTo(2));

        // now check breakended bits
        SvannaVariant breakendVariant = variants.get(2);
        assertThat(breakendVariant.variantType(), equalTo(VariantType.BND));
        assertThat(breakendVariant instanceof Breakended, equalTo(true));
        Breakended breakended = (Breakended) breakendVariant;
        Breakend left = breakended.left();
        assertThat(left.contigName(), equalTo("1"));
        assertThat(left.id(), equalTo("pbsv.BND.CM000663.2:13054707-CM000663.2:13256071"));
        assertThat(left.pos(), equalTo(13_054_707));
        assertThat(left.strand(), equalTo(Strand.POSITIVE));

        Breakend right = breakended.right();
        assertThat(right.contigName(), equalTo("1"));
        assertThat(right.id(), equalTo("pbsv.BND.CM000663.2:13256071-CM000663.2:13054707"));
        assertThat(right.pos(), equalTo(assembly.contigByName("1").length() - 13_256_071));
        assertThat(right.strand(), equalTo(Strand.NEGATIVE));

        // cnv bits
        SvannaVariant cnv = variants.get(5);
        assertThat(cnv.variantType(), equalTo(VariantType.CNV));
        assertThat(cnv.copyNumber(), equalTo(4));
    }

    @Test
    public void createVariantList_Svim() throws Exception {
        List<SvannaVariant> variants = parser.createVariantAlleleList(Paths.get("src/test/resources/org/jax/svanna/io/parse/svim.vcf"));

//        assertThat(variants.size(), equalTo(6)); TODO - handle the missing MATEID for SVIM BNDs
        assertThat(variants.size(), equalTo(5));

        // check general fields for the first variant
        SvannaVariant del = variants.get(0);
        assertThat(del.contigName(), equalTo("1"));
        assertThat(del.start(), equalTo(180_188));
        assertThat(del.end(), equalTo(180_393));
        assertThat(del.id(), equalTo("svim.DEL.1"));
        assertThat(del.ref(), equalTo("N"));
        assertThat(del.alt(), equalTo("<DEL>"));
        assertThat(del.length(), equalTo(206));
        assertThat(del.refLength(), equalTo(206));
        assertThat(del.changeLength(), equalTo(-205));
        assertThat(del.variantType(), equalTo(VariantType.DEL));
        assertThat(del.isSymbolic(), equalTo(true));
        assertThat(del.zygosity(), equalTo(Zygosity.HOMOZYGOUS));
        assertThat(del.minDepthOfCoverage(), equalTo(48));
        assertThat(del.numberOfRefReads(), equalTo(44));
        assertThat(del.numberOfAltReads(),equalTo(4));
    }

    @Test
    @Disabled
    public void createVariantList_Sniffles() throws Exception {
        List<SvannaVariant> variants = parser.createVariantAlleleList(Paths.get("src/test/resources/org/jax/svanna/io/parse/sniffles.vcf"));
        variants.forEach(System.err::println);
    }

    @Test
    public void toVariants() {
        String line = "2\t321682\tdel0\tT\t<DEL>\t6\tPASS\tSVTYPE=DEL;END=321887;SVLEN=-205;CIPOS=-56,20;CIEND=-10,62\tGT:GQ:DP:AD\t0/1:12:11:6,5";
        VariantContext vc = VCF_CODEC.decode(line);
        Optional<? extends SvannaVariant> vo = parser.toVariants().apply(vc);

        assertThat(vo.isPresent(), equalTo(true));
        SvannaVariant variant = vo.get();
        assertThat(variant.contigName(), equalTo("2"));
        assertThat(variant.startPosition(), equalTo(Position.of(321_682, -56, 20)));
        assertThat(variant.endPosition(), equalTo(Position.of(321_887, -10, 62)));

        assertThat(variant.id(), equalTo("del0"));
        assertThat(variant.strand(), equalTo(Strand.POSITIVE));
        assertThat(variant.coordinateSystem(), equalTo(CoordinateSystem.ONE_BASED));
        assertThat(variant.variantType(), equalTo(VariantType.DEL));

        assertThat(variant.length(), equalTo(206));
        assertThat(variant.refLength(), equalTo(206));
        assertThat(variant.changeLength(), equalTo(-205));

        assertThat(variant.ref(), equalTo("T"));
        assertThat(variant.alt(), equalTo("<DEL>"));

        assertThat(variant.zygosity(), equalTo(Zygosity.HETEROZYGOUS));
        assertThat(variant.minDepthOfCoverage(), equalTo(11));
        assertThat(variant.numberOfRefReads(), equalTo(6));
        assertThat(variant.numberOfAltReads(), equalTo(5));
    }

    @Test
    public void toVariants_breakendVariant() {
        String line = "2\t321682\tbnd_V\tT\t]13:123456]T\t6\tPASS\tSVTYPE=BND;MATEID=bnd_U;EVENT=tra2\tGT\t./.";
        VariantContext vc = VCF_CODEC.decode(line);
        Optional<? extends SvannaVariant> vo = parser.toVariants().apply(vc);

        Contig chr2 = assembly.contigByName("2");
        Contig chr13 = assembly.contigByName("13");
        Position expPosition = Position.of(321_682).invert(chr2, CoordinateSystem.ONE_BASED);

        // variant bits
        assertThat(vo.isPresent(), equalTo(true));
        SvannaVariant variant = vo.get();
        assertThat(variant.contigName(), equalTo("2"));
        assertThat(variant.startPosition(), equalTo(expPosition));
        assertThat(variant.endPosition(), equalTo(expPosition));

        assertThat(variant.id(), equalTo("bnd_V"));
        assertThat(variant.strand(), equalTo(Strand.NEGATIVE));
        assertThat(variant.coordinateSystem(), equalTo(CoordinateSystem.ZERO_BASED));
        assertThat(variant.variantType(), equalTo(VariantType.BND));

        assertThat(variant.length(), equalTo(0));
        assertThat(variant.refLength(), equalTo(1));
        assertThat(variant.changeLength(), equalTo(0));

        assertThat(variant.ref(), equalTo("A"));
        assertThat(variant.alt(), equalTo(""));

        assertThat(variant.zygosity(), equalTo(Zygosity.UNKNOWN));
        assertThat(variant.minDepthOfCoverage(), equalTo(-1));

        // breakend bits
        assertThat(variant instanceof Breakended, equalTo(true));
        Breakended bnd = (Breakended) variant;
        assertThat(bnd.eventId(), equalTo("tra2"));
        Breakend left = bnd.left();
        assertThat(left.id(), equalTo("bnd_V"));
        assertThat(left.contigName(), equalTo("2"));
        assertThat(left.position(), equalTo(expPosition));

        Breakend right = bnd.right();
        assertThat(right.id(), equalTo("bnd_U"));
        assertThat(right.contigName(), equalTo("13"));
        assertThat(right.position(), equalTo(Position.of(123_456).invert(chr13, CoordinateSystem.ZERO_BASED)));
    }

    @Test
    public void toVariants_multiallelicBreakendVariant() {
        String line = "2\t321681\tbnd_W\tG\tG]17:198982],C\t6\tPASS\tSVTYPE=BND;MATEID=bnd_Y;EVENT=tra1\tGT\t./.";
        VariantContext vc = VCF_CODEC.decode(line);
        Optional<? extends Variant> vo = parser.toVariants().apply(vc);

        assertThat(vo.isPresent(), is(false));
    }

    @Test
    public void toVariants_multiallelicSymbolicVariant() {
        String line = "2\t321682\tdel0\tT\t<DEL>,C\t6\tPASS\tSVTYPE=DEL;END=321887;SVLEN=-205;CIPOS=-56,20;CIEND=-10,62\tGT:GQ:DP\t0/1:12:11";
        VariantContext vc = VCF_CODEC.decode(line);
        Optional<? extends Variant> vo = parser.toVariants().apply(vc);

        assertThat(vo.isPresent(), is(false));
    }

    @Test
    public void toVariants_symbolic_unknownContig() {
        String line = "bacon\t12665100\tdup0\tA\t<DUP>\t14\tPASS\tSVTYPE=DUP;END=12686200;SVLEN=21100;CIPOS=-500,500;CIEND=-500,500;DP=5\tGT:GQ:CN:CNQ\t./.:0:3:16.2";
        VariantContext vc = VCF_CODEC.decode(line);
        Optional<? extends Variant> variants = parser.toVariants().apply(vc);

        assertThat(variants.isPresent(), is(false));
    }

    @Test
    public void toVariants_sequence_unknownContig() {
        String line = "bacon\t14370\trs6054257\tG\tA\t29\tPASS\tDP=14;AF=0.5;DB\tGT:GQ:DP\t1/1:43:5";
        VariantContext vc = VCF_CODEC.decode(line);
        Optional<? extends Variant> vo = parser.toVariants().apply(vc);

        assertThat(vo.isPresent(), is(false));
    }
}