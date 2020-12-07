package org.jax.svanna.io.parse;

import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFCodec;
import htsjdk.variant.vcf.VCFFileReader;
import htsjdk.variant.vcf.VCFHeaderVersion;
import org.jax.svanna.io.TestDataConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.variant.api.Breakend;
import org.monarchinitiative.variant.api.Contig;
import org.monarchinitiative.variant.api.GenomicAssembly;
import org.monarchinitiative.variant.api.Strand;
import org.monarchinitiative.variant.api.impl.BreakendVariant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

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
        Optional<BreakendVariant> bndVarOpt = assembler.resolveBreakends(vc);

        assertThat(bndVarOpt.isPresent(), is(true));

        BreakendVariant bv = bndVarOpt.get();
        assertThat(bv.ref(), is("C"));
        assertThat(bv.alt(), is(""));

        Breakend left = bv.left();
        assertThat(left.id(), is("bnd_U"));
        assertThat(left.contigName(), is("13"));
        assertThat(left.position().pos(), is(123_456));
        assertThat(left.strand(), is(Strand.POSITIVE));


        Breakend right = bv.right();
        assertThat(right.id(), is("bnd_V"));
        assertThat(right.contigName(), is("2"));
        assertThat(right.position().pos(), is(321_681));
        assertThat(right.strand(), is(Strand.POSITIVE));
    }

    @Test
    public void resolveBreakends_PosToNeg() {
        String line = "2\t321681\tbnd_W\tG\tG]17:198982]\t6\tPASS\tSVTYPE=BND;MATEID=bnd_Y;EVENT=tra1\tGT\t./.";
        VariantContext vc = VCF_CODEC.decode(line);
        Optional<BreakendVariant> bndVarOpt = assembler.resolveBreakends(vc);

        assertThat(bndVarOpt.isPresent(), is(true));

        BreakendVariant bv = bndVarOpt.get();
        assertThat(bv.ref(), is("G"));
        assertThat(bv.alt(), is(""));

        Breakend left = bv.left();
        assertThat(left.id(), is("bnd_W"));
        assertThat(left.contigName(), is("2"));
        assertThat(left.position().pos(), is(321_681));
        assertThat(left.strand(), is(Strand.POSITIVE));

        Contig chr17 = genomicAssembly.contigByName("17");
        Breakend right = bv.right();
        assertThat(right.id(), is("bnd_Y"));
        assertThat(right.contigName(), is("17"));
        assertThat(right.position().pos(), is(chr17.length() - 198_982));
        assertThat(right.strand(), is(Strand.NEGATIVE));
    }

    @Test
    public void resolveBreakends_NegToPos() {
        String line = "13\t123457\tbnd_X\tA\t[17:198983[A\t6\tPASS\tSVTYPE=BND;MATEID=bnd_Z;EVENT=tra3\tGT\t./.";
        VariantContext vc = VCF_CODEC.decode(line);
        Optional<BreakendVariant> bndVarOpt = assembler.resolveBreakends(vc);

        assertThat(bndVarOpt.isPresent(), is(true));

        BreakendVariant bv = bndVarOpt.get();
        assertThat(bv.ref(), is("T"));
        assertThat(bv.alt(), is(""));

        Contig chr13 = genomicAssembly.contigByName("13");
        Breakend left = bv.left();
        assertThat(left.id(), is("bnd_X"));
        assertThat(left.contigName(), is("13"));
        assertThat(left.position().pos(), is(chr13.length() - 123_456));
        assertThat(left.strand(), is(Strand.NEGATIVE));


        Breakend right = bv.right();
        assertThat(right.id(), is("bnd_Z"));
        assertThat(right.contigName(), is("17"));
        assertThat(right.position().pos(), is(198_982));
        assertThat(right.strand(), is(Strand.POSITIVE));
    }

    @Test
    public void resolveBreakends_NegToNeg() {
        String line = "2\t321682\tbnd_V\tT\t]13:123456]T\t6\tPASS\tSVTYPE=BND;MATEID=bnd_U;EVENT=tra2\tGT\t./.";
        VariantContext vc = VCF_CODEC.decode(line);
        Optional<BreakendVariant> bndVarOpt = assembler.resolveBreakends(vc);

        assertThat(bndVarOpt.isPresent(), is(true));

        BreakendVariant bv = bndVarOpt.get();
        assertThat(bv.ref(), is("A"));
        assertThat(bv.alt(), is(""));

        Contig chr2 = genomicAssembly.contigByName("2");
        Breakend left = bv.left();
        assertThat(left.id(), is("bnd_V"));
        assertThat(left.contigName(), is("2"));
        assertThat(left.position().pos(), is(chr2.length() - 321_681));
        assertThat(left.strand(), is(Strand.NEGATIVE));


        Contig chr13 = genomicAssembly.contigByName("13");
        Breakend right = bv.right();
        assertThat(right.id(), is("bnd_U"));
        assertThat(right.contigName(), is("13"));
        assertThat(right.position().pos(), is(chr13.length() - 123_456));
        assertThat(right.strand(), is(Strand.NEGATIVE));
    }

    @Test
    public void resolveBreakendsWithInsertedSequence_NegToNeg() {
        String line = "2\t321682\tbnd_V\tT\t]13:123456]AGTNNNNNCAT\t6\tPASS\tSVTYPE=BND;MATEID=bnd_U;EVENT=tra2\tGT\t./.";
        VariantContext vc = VCF_CODEC.decode(line);
        Optional<BreakendVariant> bndVarOpt = assembler.resolveBreakends(vc);

        assertThat(bndVarOpt.isPresent(), is(true));

        BreakendVariant bv = bndVarOpt.get();
        assertThat(bv.ref(), is("A"));
        assertThat(bv.alt(), is("TGNNNNNACT"));

        Contig chr2 = genomicAssembly.contigByName("2");
        Breakend left = bv.left();
        assertThat(left.id(), is("bnd_V"));
        assertThat(left.contigName(), is("2"));
        assertThat(left.position().pos(), is(chr2.length() - 321_681));
        assertThat(left.strand(), is(Strand.NEGATIVE));


        Contig chr13 = genomicAssembly.contigByName("13");
        Breakend right = bv.right();
        assertThat(right.id(), is("bnd_U"));
        assertThat(right.contigName(), is("13"));
        assertThat(right.position().pos(), is(chr13.length() - 123_456));
        assertThat(right.strand(), is(Strand.NEGATIVE));
    }

    @Test
    public void resolveBreakendsWithInsertedSequence_PosToPos() {
        String line = "13\t123456\tbnd_U\tC\tCAGTNNNNNCA[2:321682[\t6\tPASS\tSVTYPE=BND;MATEID=bnd_V;EVENT=tra2\tGT\t./.";
        VariantContext vc = VCF_CODEC.decode(line);
        Optional<BreakendVariant> bndVarOpt = assembler.resolveBreakends(vc);

        assertThat(bndVarOpt.isPresent(), is(true));

        BreakendVariant bv = bndVarOpt.get();
        assertThat(bv.ref(), is("C"));
        assertThat(bv.alt(), is("AGTNNNNNCA"));

        Breakend left = bv.left();
        assertThat(left.id(), is("bnd_U"));
        assertThat(left.contigName(), is("13"));
        assertThat(left.position().pos(), is(123_456));
        assertThat(left.strand(), is(Strand.POSITIVE));


        Breakend right = bv.right();
        assertThat(right.id(), is("bnd_V"));
        assertThat(right.contigName(), is("2"));
        assertThat(right.position().pos(), is(321_681));
        assertThat(right.strand(), is(Strand.POSITIVE));
    }
}