package org.jax.svann.parse;

import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFCodec;
import htsjdk.variant.vcf.VCFFileReader;
import htsjdk.variant.vcf.VCFHeaderVersion;
import org.jax.svann.ToyCoordinateTestBase;
import org.jax.svann.reference.Adjacency;
import org.jax.svann.reference.Breakend;
import org.jax.svann.reference.Position;
import org.jax.svann.reference.Strand;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;

public class VcfStructuralRearrangementParserTest extends ToyCoordinateTestBase {

    private static final VCFCodec VCF_CODEC = new VCFCodec();
    private static BreakendAssembler ASSEMBLER;
    private static final Path VCF_HEADER = Paths.get("src/test/resources/sv_example.vcf");
    private VcfStructuralRearrangementParser parser;

    @BeforeAll
    public static void beforeAll() {
        ASSEMBLER = new BreakendAssembler();
        try (VCFFileReader reader = new VCFFileReader(VCF_HEADER, false)) {
            VCF_CODEC.setVCFHeader(reader.getFileHeader(), VCFHeaderVersion.VCF4_3);
        }
    }

    @BeforeEach
    public void setUp() {
        parser = new VcfStructuralRearrangementParser(TOY_ASSEMBLY, ASSEMBLER);
    }

    @Test
    public void makeInversionAdjacencies() {
        String line = "ctg1\t11\tINV0\tT\t<INV>\t6\tPASS\tSVTYPE=INV;END=19\t";
        VariantContext vc = VCF_CODEC.decode(line);

        List<? extends Adjacency> adjacencies = parser.makeInversionAdjacencies(vc);
        assertThat(adjacencies, hasSize(2));

        Adjacency alpha = adjacencies.get(0);
        Adjacency beta = adjacencies.get(1);

        Breakend alphaLeft = alpha.getLeft();
        assertThat(alphaLeft.getId(), is("INV0"));
        assertThat(alphaLeft.getBeginPosition(), is(Position.precise(10)));
        assertThat(alphaLeft.getStrand(), is(Strand.FWD));

        Breakend alphaRight = alpha.getRight();
        assertThat(alphaRight.getId(), is("INV0"));
        assertThat(alphaRight.getBeginPosition(), is(Position.precise(12)));
        assertThat(alphaRight.getStrand(), is(Strand.REV));

        Breakend betaLeft = beta.getLeft();
        assertThat(betaLeft.getId(), is("INV0"));
        assertThat(betaLeft.getBeginPosition(), is(Position.precise(20)));
        assertThat(betaLeft.getStrand(), is(Strand.REV));

        Breakend betaRight = beta.getRight();
        assertThat(betaRight.getId(), is("INV0"));
        assertThat(betaRight.getBeginPosition(), is(Position.precise(20)));
        assertThat(betaRight.getStrand(), is(Strand.FWD));
    }
}