package org.jax.svanna.core.parse;

import org.jax.svanna.core.TestDataConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.variant.api.GenomicAssembly;
import org.monarchinitiative.variant.api.Variant;
import org.monarchinitiative.variant.api.VariantType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

@SpringBootTest(classes = {TestDataConfig.class})
public class MergedVariantParserTest {

    private static final Path SV_EXAMPLE_PATH = Paths.get("src/test/resources/sv_small_merged.bed");

    @Autowired
    public GenomicAssembly genomicAssembly;

    private MergedVariantParser parser;

    @BeforeEach
    public void setUp() {
        parser = new MergedVariantParser(genomicAssembly);
    }

    @Test
    public void createVariantAlleleList() throws Exception {
        List<Variant> alleles = parser.createVariantAlleleList(SV_EXAMPLE_PATH);

        assertThat(alleles, hasSize(3));

        Variant deletion = alleles.get(0);
        assertThat(deletion.contigName(), is("4"));
        assertThat(deletion.startPosition().zeroBasedPos(), is(87_615_924));
        assertThat(deletion.startPosition().isPrecise(), is(true));
        assertThat(deletion.endPosition().oneBasedPos(), is(87_616_059));
        assertThat(deletion.endPosition().isPrecise(), is(true));
        assertThat(deletion.id(), is("pb(pbsv.DEL.30582);sn(96836);sv(svim.DEL.24308)"));
        assertThat(deletion.ref(), is("N"));
        assertThat(deletion.alt(), is("<DEL>"));
        assertThat(deletion.variantType(), is(VariantType.DEL));

        Variant duplication = alleles.get(1);
        assertThat(duplication.contigName(), is("4"));
        assertThat(duplication.startPosition().zeroBasedPos(), is(89_180_724));
        assertThat(duplication.startPosition().isPrecise(), is(true));
        assertThat(duplication.endPosition().oneBasedPos(), is(89_186_234));
        assertThat(duplication.endPosition().isPrecise(), is(true));
        assertThat(duplication.id(), is("pb(pbsv.SPLIT.DUP.30605);sn(96896);sv(svim.DUP_TANDEM.6669)"));
        assertThat(duplication.ref(), is("N"));
        assertThat(duplication.alt(), is("<DUP>"));
        assertThat(duplication.variantType(), is(VariantType.DUP));

        Variant inversion = alleles.get(2);
        assertThat(inversion.contigName(), is("4"));
        assertThat(inversion.startPosition().zeroBasedPos(), is(95_391_950));
        assertThat(inversion.startPosition().isPrecise(), is(true));
        assertThat(inversion.endPosition().oneBasedPos(), is(95_392_557));
        assertThat(inversion.endPosition().isPrecise(), is(true));
        assertThat(inversion.id(), is("sn(97077);sv(svim.INV.1900)"));
        assertThat(inversion.ref(), is("N"));
        assertThat(inversion.alt(), is("<INV>"));
        assertThat(inversion.variantType(), is(VariantType.INV));

//        alleles.forEach(System.err::println);
    }
}