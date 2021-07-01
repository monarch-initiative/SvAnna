package org.jax.svanna.ingest.parse.population;

import org.jax.svanna.core.landscape.PopulationVariant;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.svart.GenomicAssemblies;
import org.monarchinitiative.svart.GenomicAssembly;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

public class HgSvc2VcfParserTest {

    private static final Path testFile = Paths.get(HgSvc2VcfParserTest.class.getResource("/variants/freeze3.sv.alt.250lines.vcf.gz").getPath());
    private static final GenomicAssembly genomicAssembly = GenomicAssemblies.GRCh38p13();

    @Test
    public void parseToList() throws Exception {
        HgSvc2VcfParser instance = new HgSvc2VcfParser(genomicAssembly, testFile);
        List<? extends PopulationVariant> variants = instance.parseToList();

        assertThat(variants, hasSize(43));
    }

}