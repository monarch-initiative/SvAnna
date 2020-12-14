package org.jax.svanna.io.parse;

import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFCodec;
import htsjdk.variant.vcf.VCFFileReader;
import htsjdk.variant.vcf.VCFHeaderVersion;
import org.jax.svanna.io.TestDataConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.variant.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

@SpringBootTest(classes = TestDataConfig.class)
public class BreakendAssemblerTest {

    private static final VCFCodec VCF_CODEC = new VCFCodec();
    private static final boolean REQUIRE_INDEX = false;
    private static final Path VCF_EXAMPLE = Paths.get("src/test/resources/sv_example.vcf");

    @Autowired
    private GenomicAssembly genomicAssembly;

    private BreakendAssembler assembler;

    @BeforeAll
    public static void beforeAll() {
        try (VCFFileReader reader = new VCFFileReader(VCF_EXAMPLE, REQUIRE_INDEX)) {
            VCF_CODEC.setVCFHeader(reader.getFileHeader(), VCFHeaderVersion.VCF4_3);
        }
    }

    @BeforeEach
    public void setUp() {
        assembler = new BreakendAssembler(genomicAssembly);
    }

    @Test
    public void resolveBreakends_PosToPos() {
        String line = "13\t123456\tbnd_U\tC\tC[2:321682[\t6\tPASS\tSVTYPE=BND;MATEID=bnd_V;EVENT=tra2\tGT\t./.";
        VariantContext vc = VCF_CODEC.decode(line);
        Optional<? extends Variant> bndVarOpt = assembler.resolveBreakends(vc);

        assertThat(bndVarOpt.isPresent(), equalTo(true));

        Variant variant = bndVarOpt.get();
        assertThat(variant.ref(), equalTo("C"));
        assertThat(variant.alt(), equalTo(""));

        assertThat(variant instanceof Breakended, equalTo(true));

        Breakended bv = (Breakended) variant;
        Breakend left = bv.left();
        assertThat(left.id(), equalTo("bnd_U"));
        assertThat(left.contigName(), equalTo("13"));
        assertThat(left.position().pos(), equalTo(123_456));
        assertThat(left.strand(), equalTo(Strand.POSITIVE));


        Breakend right = bv.right();
        assertThat(right.id(), equalTo("bnd_V"));
        assertThat(right.contigName(), equalTo("2"));
        assertThat(right.position().pos(), equalTo(321_681));
        assertThat(right.strand(), equalTo(Strand.POSITIVE));
    }

    @Test
    public void resolveBreakends_PosToNeg() {
        String line = "2\t321681\tbnd_W\tG\tG]17:198982]\t6\tPASS\tSVTYPE=BND;MATEID=bnd_Y;EVENT=tra1\tGT\t./.";
        VariantContext vc = VCF_CODEC.decode(line);
        Optional<? extends Variant> bndVarOpt = assembler.resolveBreakends(vc);

        assertThat(bndVarOpt.isPresent(), equalTo(true));

        Variant variant = bndVarOpt.get();
        assertThat(variant.ref(), equalTo("G"));
        assertThat(variant.alt(), equalTo(""));

        assertThat(variant instanceof Breakended, equalTo(true));
        Breakended bv = (Breakended) variant;
        Breakend left = bv.left();
        assertThat(left.id(), equalTo("bnd_W"));
        assertThat(left.contigName(), equalTo("2"));
        assertThat(left.position().pos(), equalTo(321_681));
        assertThat(left.strand(), equalTo(Strand.POSITIVE));

        Contig chr17 = genomicAssembly.contigByName("17");
        Breakend right = bv.right();
        assertThat(right.id(), equalTo("bnd_Y"));
        assertThat(right.contigName(), equalTo("17"));
        assertThat(right.position().pos(), equalTo(chr17.length() - 198_982));
        assertThat(right.strand(), equalTo(Strand.NEGATIVE));
    }

    @Test
    public void resolveBreakends_NegToPos() {
        String line = "13\t123457\tbnd_X\tA\t[17:198983[A\t6\tPASS\tSVTYPE=BND;MATEID=bnd_Z;EVENT=tra3\tGT\t./.";
        VariantContext vc = VCF_CODEC.decode(line);
        Optional<? extends Variant> bndVarOpt = assembler.resolveBreakends(vc);

        assertThat(bndVarOpt.isPresent(), equalTo(true));

        Variant variant = bndVarOpt.get();
        assertThat(variant.ref(), equalTo("T"));
        assertThat(variant.alt(), equalTo(""));

        assertThat(variant instanceof Breakended, equalTo(true));
        Breakended bv = (Breakended) variant;
        Contig chr13 = genomicAssembly.contigByName("13");
        Breakend left = bv.left();
        assertThat(left.id(), equalTo("bnd_X"));
        assertThat(left.contigName(), equalTo("13"));
        assertThat(left.position().pos(), equalTo(chr13.length() - 123_456));
        assertThat(left.strand(), equalTo(Strand.NEGATIVE));


        Breakend right = bv.right();
        assertThat(right.id(), equalTo("bnd_Z"));
        assertThat(right.contigName(), equalTo("17"));
        assertThat(right.position().pos(), equalTo(198_982));
        assertThat(right.strand(), equalTo(Strand.POSITIVE));
    }

    @Test
    public void resolveBreakends_NegToNeg() {
        String line = "2\t321682\tbnd_V\tT\t]13:123456]T\t6\tPASS\tSVTYPE=BND;MATEID=bnd_U;EVENT=tra2\tGT\t./.";
        VariantContext vc = VCF_CODEC.decode(line);
        Optional<?extends Variant> bndVarOpt = assembler.resolveBreakends(vc);

        assertThat(bndVarOpt.isPresent(), equalTo(true));

        Variant variant = bndVarOpt.get();
        assertThat(variant.ref(), equalTo("A"));
        assertThat(variant.alt(), equalTo(""));

        assertThat(variant instanceof Breakended, equalTo(true));
        Breakended bv = (Breakended) variant;
        Contig chr2 = genomicAssembly.contigByName("2");
        Breakend left = bv.left();
        assertThat(left.id(), equalTo("bnd_V"));
        assertThat(left.contigName(), equalTo("2"));
        assertThat(left.position().pos(), equalTo(chr2.length() - 321_681));
        assertThat(left.strand(), equalTo(Strand.NEGATIVE));


        Contig chr13 = genomicAssembly.contigByName("13");
        Breakend right = bv.right();
        assertThat(right.id(), equalTo("bnd_U"));
        assertThat(right.contigName(), equalTo("13"));
        assertThat(right.position().pos(), equalTo(chr13.length() - 123_456));
        assertThat(right.strand(), equalTo(Strand.NEGATIVE));
    }

    @Test
    public void resolveBreakendsWithInsertedSequence_NegToNeg() {
        String line = "2\t321682\tbnd_V\tT\t]13:123456]AGTNNNNNCAT\t6\tPASS\tSVTYPE=BND;MATEID=bnd_U;EVENT=tra2\tGT\t./.";
        VariantContext vc = VCF_CODEC.decode(line);
        Optional<? extends Variant> bndVarOpt = assembler.resolveBreakends(vc);

        assertThat(bndVarOpt.isPresent(), equalTo(true));

        Variant variant = bndVarOpt.get();
        assertThat(variant.ref(), equalTo("A"));
        assertThat(variant.alt(), equalTo("TGNNNNNACT"));

        assertThat(variant instanceof Breakended, equalTo(true));
        Breakended bv = (Breakended) variant;
        Contig chr2 = genomicAssembly.contigByName("2");
        Breakend left = bv.left();
        assertThat(left.id(), equalTo("bnd_V"));
        assertThat(left.contigName(), equalTo("2"));
        assertThat(left.position().pos(), equalTo(chr2.length() - 321_681));
        assertThat(left.strand(), equalTo(Strand.NEGATIVE));


        Contig chr13 = genomicAssembly.contigByName("13");
        Breakend right = bv.right();
        assertThat(right.id(), equalTo("bnd_U"));
        assertThat(right.contigName(), equalTo("13"));
        assertThat(right.position().pos(), equalTo(chr13.length() - 123_456));
        assertThat(right.strand(), equalTo(Strand.NEGATIVE));
    }

    @Test
    public void resolveBreakendsWithInsertedSequence_PosToPos() {
        String line = "13\t123456\tbnd_U\tC\tCAGTNNNNNCA[2:321682[\t6\tPASS\tSVTYPE=BND;MATEID=bnd_V;EVENT=tra2\tGT\t./.";
        VariantContext vc = VCF_CODEC.decode(line);
        Optional<? extends Variant> bndVarOpt = assembler.resolveBreakends(vc);

        assertThat(bndVarOpt.isPresent(), equalTo(true));

        Variant variant = bndVarOpt.get();
        assertThat(variant.ref(), equalTo("C"));
        assertThat(variant.alt(), equalTo("AGTNNNNNCA"));

        assertThat(variant instanceof Breakended, equalTo(true));
        Breakended bv = (Breakended) variant;
        Breakend left = bv.left();
        assertThat(left.id(), equalTo("bnd_U"));
        assertThat(left.contigName(), equalTo("13"));
        assertThat(left.position().pos(), equalTo(123_456));
        assertThat(left.strand(), equalTo(Strand.POSITIVE));


        Breakend right = bv.right();
        assertThat(right.id(), equalTo("bnd_V"));
        assertThat(right.contigName(), equalTo("2"));
        assertThat(right.position().pos(), equalTo(321_681));
        assertThat(right.strand(), equalTo(Strand.POSITIVE));
    }
}