package org.jax.svanna.io.parse;

import org.jax.svanna.io.TestDataConfig;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.svart.*;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

@SpringBootTest(classes = TestDataConfig.class)
public class MergedVariantParserTest {

    private static final Path SV_EXAMPLE_PATH = Paths.get("src/test/resources/org/jax/svanna/io/parse/sv_small_merged.bed");

    private final GenomicAssembly GRCh38p13= GenomicAssemblies.GRCh38p13();

    @Test
    public void createVariantAlleleList() throws Exception {
        MergedVariantParser instance = new MergedVariantParser(GRCh38p13);

        List<Variant> alleles = instance.createVariantAlleleList(SV_EXAMPLE_PATH);

        assertThat(alleles, hasSize(3));

        Variant deletion = alleles.get(0);
        assertThat(deletion.contig(), equalTo(GRCh38p13.contigByName("4")));
        assertThat(deletion.id(), equalTo("pb(pbsv.DEL.30582);sn(96836);sv(svim.DEL.24308)"));
        assertThat(deletion.strand(), equalTo(Strand.POSITIVE));
        assertThat(deletion.coordinateSystem(), equalTo(CoordinateSystem.zeroBased()));
        assertThat(deletion.startPosition(), equalTo(Position.of(87_615_924)));
        assertThat(deletion.endPosition(), equalTo(Position.of(87_616_059)));

        assertThat(deletion.ref(), equalTo(""));
        assertThat(deletion.alt(), equalTo("<DEL>"));
        assertThat(deletion.variantType(), equalTo(VariantType.DEL));
        assertThat(deletion.length(), equalTo(87_616_059-87_615_924));
        assertThat(deletion.changeLength(), equalTo(87_615_924 - 87_616_059));

        Variant duplication = alleles.get(1);
        assertThat(duplication.contigName(), equalTo("4"));
        assertThat(duplication.startPosition().pos(), equalTo(89_180_724));
        assertThat(duplication.startPosition().isPrecise(), equalTo(true));
        assertThat(duplication.endPosition().pos(), equalTo(89_186_234));
        assertThat(duplication.endPosition().isPrecise(), equalTo(true));
        assertThat(duplication.id(), equalTo("pb(pbsv.SPLIT.DUP.30605);sn(96896);sv(svim.DUP_TANDEM.6669)"));
        assertThat(duplication.ref(), equalTo(""));
        assertThat(duplication.alt(), equalTo("<DUP>"));
        assertThat(duplication.variantType(), equalTo(VariantType.DUP));
        assertThat(duplication.length(), equalTo(89_186_234 - 89_180_724));
        assertThat(duplication.changeLength(), equalTo(89_186_234 - 89_180_724));

        Variant inversion = alleles.get(2);
        assertThat(inversion.contigName(), equalTo("4"));
        assertThat(inversion.startPosition().pos(), equalTo(95_391_950));
        assertThat(inversion.startPosition().isPrecise(), equalTo(true));
        assertThat(inversion.endPosition().pos(), equalTo(95_392_557));
        assertThat(inversion.endPosition().isPrecise(), equalTo(true));
        assertThat(inversion.id(), equalTo("sn(97077);sv(svim.INV.1900)"));
        assertThat(inversion.ref(), equalTo(""));
        assertThat(inversion.alt(), equalTo("<INV>"));
        assertThat(inversion.variantType(), equalTo(VariantType.INV));
        assertThat(inversion.changeLength(), equalTo(0));
    }
}