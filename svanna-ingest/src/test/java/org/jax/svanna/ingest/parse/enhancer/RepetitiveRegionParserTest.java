package org.jax.svanna.ingest.parse.enhancer;

import org.jax.svanna.core.landscape.RepetitiveRegion;
import org.jax.svanna.ingest.parse.RepetitiveRegionParser;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.svart.GenomicAssemblies;

import java.nio.file.Path;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

public class RepetitiveRegionParserTest {

    private static final Path testFilePath = Path.of(RepetitiveRegionParserTest.class.getResource("/repeats/hg38.fa.out.10lines.gz").getPath());

    @Test
    public void parse() throws Exception {
        RepetitiveRegionParser instance = new RepetitiveRegionParser(GenomicAssemblies.GRCh38p13(), testFilePath);
        List<? extends RepetitiveRegion> regions = instance.parseToList();

        assertThat(regions, hasSize(7));
    }
}