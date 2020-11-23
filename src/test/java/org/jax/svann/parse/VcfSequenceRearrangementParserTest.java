package org.jax.svann.parse;

import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFCodec;
import htsjdk.variant.vcf.VCFFileReader;
import htsjdk.variant.vcf.VCFHeaderVersion;
import org.jax.svann.ToyCoordinateTestBase;
import org.jax.svann.reference.*;
import org.jax.svann.reference.genome.GenomeAssemblyProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;

public class VcfSequenceRearrangementParserTest extends ToyCoordinateTestBase {

    private static final VCFCodec VCF_CODEC = new VCFCodec();
    private static final Path SV_EXAMPLE_PATH = Paths.get("src/test/resources/sv_example.vcf");
    private static BreakendAssembler<StructuralVariant> ASSEMBLER;
    private VcfSequenceRearrangementParser parser;

    @BeforeAll
    public static void beforeAll() {
        ASSEMBLER = new BreakendAssemblerImpl();
        try (VCFFileReader reader = new VCFFileReader(SV_EXAMPLE_PATH, false)) {
            VCF_CODEC.setVCFHeader(reader.getFileHeader(), VCFHeaderVersion.VCF4_3);
        }
    }

    @BeforeEach
    public void setUp() {
        parser = new VcfSequenceRearrangementParser(TOY_ASSEMBLY, ASSEMBLER);
    }

    @Test
    public void parseFile() {
        VcfSequenceRearrangementParser parser = new VcfSequenceRearrangementParser(GenomeAssemblyProvider.getGrch38Assembly(), ASSEMBLER);
        List<StructuralVariant> rearrangements = parser.parseFile(SV_EXAMPLE_PATH);

        // we expect to see 6 rearrangement when things are ready
        assertThat(rearrangements, hasSize(6));

        StructuralVariant deletion = rearrangements.get(0);
        Adjacency adjacency = deletion.getAdjacencies().get(0);
        assertThat(adjacency.depthOfCoverage(), is(11));
        assertThat(deletion.zygosity(), is(Zygosity.HETEROZYGOUS));

        StructuralVariant insertion = rearrangements.get(1);
        Adjacency left = insertion.getAdjacencies().get(0);
        Adjacency right = insertion.getAdjacencies().get(1);
        assertThat(left.depthOfCoverage(), is(-1));
        assertThat(right.depthOfCoverage(), is(-1));
        assertThat(insertion.zygosity(), is(Zygosity.HOMOZYGOUS));

        StructuralVariant duplication = rearrangements.get(2);
        adjacency = duplication.getAdjacencies().get(0);
        assertThat(adjacency.depthOfCoverage(), is(5));
        assertThat(duplication.zygosity(), is(Zygosity.UNKNOWN));

        StructuralVariant breakend = rearrangements.get(3);
        assertThat(breakend.zygosity(), is(Zygosity.UNKNOWN));
    }

    /*
     *              TEST SYMBOLIC RECORD CONVERSION METHODS
     */

    @Test
    public void makeDeletionAdjacency() {
        String line = "ctg2\t11\tDEL0\tT\t<DEL>\t6\tPASS\tSVTYPE=DEL;END=20\tGT\t0/1";
        VariantContext vc = VCF_CODEC.decode(line);

        Optional<Adjacency> adjacencyOpt = parser.makeDeletionAdjacency(vc);
        assertThat(adjacencyOpt.isPresent(), is(true));

        Adjacency adjacency = adjacencyOpt.get();

        Breakend left = adjacency.getStart();
        assertThat(left.getId(), is("DEL0"));
        assertThat(left.getPosition(), is(10));
        assertThat(left.getStrand(), is(Strand.FWD));

        Breakend right = adjacency.getEnd();
        assertThat(right.getId(), is("DEL0"));
        assertThat(right.getPosition(), is(21));
        assertThat(right.getStrand(), is(Strand.FWD));
    }

    @Test
    public void makeDuplicationAdjacency() {
        String line = "ctg1\t11\tDUP0\tT\t<DUP>\t6\tPASS\tSVTYPE=DUP;END=19;CIPOS=-2,2;CIEND=-1,1\tGT\t0/1";
        VariantContext vc = VCF_CODEC.decode(line);

        Optional<Adjacency> adjacencyOpt = parser.makeDuplicationAdjacency(vc);
        assertThat(adjacencyOpt.isPresent(), is(true));

        Adjacency adjacency = adjacencyOpt.get();

        Breakend left = adjacency.getStart();
        assertThat(left.getId(), is("DUP0"));
        assertThat(left.getPosition(), is(19));
        assertThat(left.getCi(), is(ConfidenceInterval.of(-1, 1)));
        assertThat(left.getStrand(), is(Strand.FWD));

        Breakend right = adjacency.getEnd();
        assertThat(right.getId(), is("DUP0"));
        assertThat(right.getPosition(), is(11));
        assertThat(right.getCi(), is(ConfidenceInterval.of(-2, 2)));
        assertThat(right.getStrand(), is(Strand.FWD));
    }

    @Test
    public void makeInsertionAdjacencies() {
        String line = "ctg1\t15\tINS0\tT\t<INS>\t6\tPASS\tSVTYPE=INS;END=15;SVLEN=10\tGT\t0/1";
        VariantContext vc = VCF_CODEC.decode(line);

        List<? extends Adjacency> adjacencies = parser.makeInsertionAdjacencies(vc);
        assertThat(adjacencies, hasSize(2));

        Adjacency alpha = adjacencies.get(0);
        Adjacency beta = adjacencies.get(1);

        Breakend alphaLeft = alpha.getStart();
        assertThat(alphaLeft.getId(), is("INS0"));
        assertThat(alphaLeft.getPosition(), is(15));
        assertThat(alphaLeft.getStrand(), is(Strand.FWD));

        Breakend alphaRight = alpha.getEnd();
        assertThat(alphaRight.getId(), is("INS0"));
        assertThat(alphaRight.getPosition(), is(1));
        assertThat(alphaRight.getStrand(), is(Strand.FWD));

        Breakend betaLeft = beta.getStart();
        assertThat(betaLeft.getId(), is("INS0"));
        assertThat(betaLeft.getPosition(), is(10));
        assertThat(betaLeft.getStrand(), is(Strand.FWD));

        Breakend betaRight = beta.getEnd();
        assertThat(betaRight.getId(), is("INS0"));
        assertThat(betaRight.getPosition(), is(16));
        assertThat(betaRight.getStrand(), is(Strand.FWD));
    }

    @Test
    public void makeInversionAdjacencies() {
        String line = "ctg1\t11\tINV0\tT\t<INV>\t6\tPASS\tSVTYPE=INV;END=19\tGT\t0/1";
        VariantContext vc = VCF_CODEC.decode(line);

        List<? extends Adjacency> adjacencies = parser.makeInversionAdjacencies(vc);
        assertThat(adjacencies, hasSize(2));

        Adjacency alpha = adjacencies.get(0);
        Adjacency beta = adjacencies.get(1);

        Breakend alphaLeft = alpha.getStart();
        assertThat(alphaLeft.getId(), is("INV0"));
        assertThat(alphaLeft.getPosition(), is(10));
        assertThat(alphaLeft.getStrand(), is(Strand.FWD));

        Breakend alphaRight = alpha.getEnd();
        assertThat(alphaRight.getId(), is("INV0"));
        assertThat(alphaRight.getPosition(), is(12));
        assertThat(alphaRight.getStrand(), is(Strand.REV));

        Breakend betaLeft = beta.getStart();
        assertThat(betaLeft.getId(), is("INV0"));
        assertThat(betaLeft.getPosition(), is(20));
        assertThat(betaLeft.getStrand(), is(Strand.REV));

        Breakend betaRight = beta.getEnd();
        assertThat(betaRight.getId(), is("INV0"));
        assertThat(betaRight.getPosition(), is(20));
        assertThat(betaRight.getStrand(), is(Strand.FWD));
    }

    @Test
    @Disabled // only run this manually
    public void breakendTest() {
        Path vcfPath = Paths.get("src/test/resources/sv_example_breakend_resolution.vcf");
        VcfSequenceRearrangementParser parser = new VcfSequenceRearrangementParser(GenomeAssemblyProvider.getGrch38Assembly(), ASSEMBLER);
        List<StructuralVariant> variants = parser.parseFile(vcfPath);

        variants.forEach(System.err::println);
    }
}