package org.jax.svanna.ingest.parse.population;

import org.jax.svanna.model.landscape.variant.PopulationVariant;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.svart.assembly.GenomicAssemblies;
import org.monarchinitiative.svart.assembly.GenomicAssembly;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

public class HgSvc2VcfParserTest {

    private static final Path testFile = Paths.get("src/test/resources/population/freeze3.sv.alt.250lines.vcf.gz");
    private static final GenomicAssembly genomicAssembly = GenomicAssemblies.GRCh38p13();

    @Test
    public void parseToList() throws Exception {
        HgSvc2VcfParser instance = new HgSvc2VcfParser(genomicAssembly, testFile);
        List<? extends PopulationVariant> variants = instance.parseToList();

        assertThat(variants, hasSize(43));
    }

}