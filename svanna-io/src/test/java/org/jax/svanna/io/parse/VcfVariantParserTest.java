package org.jax.svanna.io.parse;

import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFCodec;
import htsjdk.variant.vcf.VCFFileReader;
import htsjdk.variant.vcf.VCFHeaderVersion;
import org.jax.svanna.io.TestDataConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.variant.api.GenomicAssembly;
import org.monarchinitiative.variant.api.Variant;
import org.monarchinitiative.variant.api.impl.BreakendVariant;
import org.monarchinitiative.variant.api.impl.SequenceVariant;
import org.monarchinitiative.variant.api.impl.SymbolicVariant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@SpringBootTest(classes = {TestDataConfig.class})
public class VcfVariantParserTest {

    private static final VCFCodec VCF_CODEC = new VCFCodec();
    private static final boolean REQUIRE_INDEX = false;
    private static final Path SV_EXAMPLE_PATH = Paths.get("src/test/resources/sv_example.vcf");

    @Autowired
    public GenomicAssembly genomicAssembly;
    private VcfVariantParser parser;

    @BeforeAll
    public static void beforeAll() {
        try (VCFFileReader reader = new VCFFileReader(SV_EXAMPLE_PATH, REQUIRE_INDEX)) {
            VCF_CODEC.setVCFHeader(reader.getFileHeader(), VCFHeaderVersion.VCF4_3);
        }
    }

    @BeforeEach
    public void setUp() {
        parser = new VcfVariantParser(genomicAssembly, REQUIRE_INDEX);
    }

    @Test
    public void createVariantList() throws Exception {
        List<Variant> variants = parser.createVariantAlleleList(SV_EXAMPLE_PATH);

        assertThat(variants, hasSize(12));

        Set<BreakendVariant> translocations = variants.stream()
                .filter(v -> v instanceof BreakendVariant)
                .map(v -> ((BreakendVariant) v))
                .collect(Collectors.toSet());
        assertThat(translocations.stream()
                        .map(Variant::id)
                        .collect(Collectors.toSet()),
                hasItems("bnd_W", "bnd_V", "bnd_U", "bnd_X", "bnd_Y", "bnd_Z"));
        assertThat(translocations.stream()
                        .map(BreakendVariant::eventId)
                        .collect(Collectors.toSet()),
                hasItems("tra1", "tra2", "tra3"));

        Set<SymbolicVariant> symbolicVariants = variants.stream()
                .filter(v -> v instanceof SymbolicVariant)
                .map(v -> ((SymbolicVariant) v))
                .collect(Collectors.toSet());
        assertThat(symbolicVariants.stream()
                        .map(Variant::id)
                        .collect(Collectors.toSet()),
                hasItems("ins0", "del0", "dup0"));

        Set<SequenceVariant> sequenceVariants = variants.stream()
                .filter(v -> v instanceof SequenceVariant)
                .map(v -> ((SequenceVariant) v))
                .collect(Collectors.toSet());
        assertThat(sequenceVariants.stream()
                        .map(SequenceVariant::id)
                        .collect(Collectors.toSet()),
                hasItems("rs6054257", "microsat1"));

//        variants.forEach(System.err::println);
    }

    @Test
    public void toVariants_multiallelicBreakendVariant() {
        String line = "2\t321681\tbnd_W\tG\tG]17:198982],C\t6\tPASS\tSVTYPE=BND;MATEID=bnd_Y;EVENT=tra1\tGT\t./.";
        VariantContext vc = VCF_CODEC.decode(line);
        Collection<Variant> variants = parser.toVariants().apply(vc);

        assertThat(variants, is(empty()));
    }

    @Test
    public void toVariants_multiallelicSymbolicVariant() {
        String line = "2\t321682\tdel0\tT\t<DEL>,C\t6\tPASS\tSVTYPE=DEL;END=321887;SVLEN=-205;CIPOS=-56,20;CIEND=-10,62\tGT:GQ:DP\t0/1:12:11";
        VariantContext vc = VCF_CODEC.decode(line);
        Collection<Variant> variants = parser.toVariants().apply(vc);

        assertThat(variants, is(empty()));
    }

    @Test
    public void toVariants_symbolic_unknownContig() {
        String line = "bacon\t12665100\tdup0\tA\t<DUP>\t14\tPASS\tSVTYPE=DUP;END=12686200;SVLEN=21100;CIPOS=-500,500;CIEND=-500,500;DP=5\tGT:GQ:CN:CNQ\t./.:0:3:16.2";
        VariantContext vc = VCF_CODEC.decode(line);
        Collection<Variant> variants = parser.toVariants().apply(vc);

        assertThat(variants, is(empty()));
    }

    @Test
    public void toVariants_sequence_unknownContig() {
        String line = "bacon\t14370\trs6054257\tG\tA\t29\tPASS\tDP=14;AF=0.5;DB\tGT:GQ:DP\t1/1:43:5";
        VariantContext vc = VCF_CODEC.decode(line);
        Collection<Variant> variants = parser.toVariants().apply(vc);

        assertThat(variants, is(empty()));
    }
}