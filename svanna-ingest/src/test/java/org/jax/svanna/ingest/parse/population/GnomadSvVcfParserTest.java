package org.jax.svanna.ingest.parse.population;

import org.jax.svanna.core.reference.PopulationVariant;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.svart.GenomicAssemblies;
import org.monarchinitiative.svart.GenomicAssembly;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

public class GnomadSvVcfParserTest {

    private static final Path testFile = Paths.get(GnomadSVFileParser.class.getResource("/variants/gnomad_v2.1_sv.sites.200lines.vcf.gz").getPath());
    private static final Path chainFile = Paths.get(GnomadSVFileParser.class.getResource("/liftover/hg19ToHg38.over.chain.gz").getPath());

    private static final GenomicAssembly genomicAssembly = GenomicAssemblies.GRCh38p13();


    @Test
    public void parseToList() throws Exception {
        GnomadSvVcfParser instance = new GnomadSvVcfParser(genomicAssembly, testFile, chainFile);
        List<? extends PopulationVariant> variants = instance.parseToList();

        assertThat(variants, hasSize(33));
    }
}