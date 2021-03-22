package org.jax.svanna.ingest.parse.population;

import org.jax.svanna.core.landscape.PopulationVariant;
import org.jax.svanna.core.landscape.PopulationVariantOrigin;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.svart.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class GnomadSvVcfParserTest {

    private static final Path testFile = Paths.get(GnomadSVFileParser.class.getResource("/variants/gnomad_v2.1_sv.sites.200lines.vcf.gz").getPath());
    private static final Path chainFile = Paths.get(GnomadSVFileParser.class.getResource("/liftover/hg19ToHg38.over.chain.gz").getPath());

    private static final GenomicAssembly genomicAssembly = GenomicAssemblies.GRCh38p13();
    private static final double ERROR = 1E-5;


    @Test
    public void parseToList() throws Exception {
        GnomadSvVcfParser instance = new GnomadSvVcfParser(genomicAssembly, testFile, chainFile);
        List<? extends PopulationVariant> variants = instance.parseToList();

        assertThat(variants, hasSize(12));

        PopulationVariant first = variants.get(0);
        assertThat(first.contigId(), equalTo(1));
        assertThat(first.strand(), equalTo(Strand.POSITIVE));
        assertThat(first.coordinateSystem(), equalTo(CoordinateSystem.oneBased()));
        assertThat(first.start(), equalTo(54666));
        assertThat(first.end(), equalTo(54666));
        assertThat(first.variantType(), equalTo(VariantType.INS));

        assertThat(first.id(), equalTo("gnomAD-SV_v2.1_INS_1_1"));
        assertThat((double) first.alleleFrequency(), closeTo(.013400, ERROR));
        assertThat(first.origin(), equalTo(PopulationVariantOrigin.GNOMAD_SV));


        PopulationVariant last = variants.get(11);
        assertThat(last.contigId(), equalTo(1));
        assertThat(last.strand(), equalTo(Strand.POSITIVE));
        assertThat(last.coordinateSystem(), equalTo(CoordinateSystem.oneBased()));
        assertThat(last.start(), equalTo(417588));
        assertThat(last.end(), equalTo(455337));
        assertThat(last.variantType(), equalTo(VariantType.DEL));

        assertThat(last.id(), equalTo("gnomAD-SV_v2.1_DEL_1_11"));
        assertThat((double) last.alleleFrequency(), closeTo(25.66600, ERROR));
        assertThat(last.origin(), equalTo(PopulationVariantOrigin.GNOMAD_SV));
    }
}