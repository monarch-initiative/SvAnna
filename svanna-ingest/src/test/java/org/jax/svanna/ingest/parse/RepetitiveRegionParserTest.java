package org.jax.svanna.ingest.parse;

import org.jax.svanna.model.landscape.repeat.RepetitiveRegion;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.svart.assembly.GenomicAssemblies;

import java.nio.file.Path;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

public class RepetitiveRegionParserTest {

    private static final Path testFilePath = Path.of(RepetitiveRegionParserTest.class.getResource("hg38.fa.out.10lines.gz").getPath());

    @Test
    public void parse() throws Exception {
        RepetitiveRegionParser instance = new RepetitiveRegionParser(GenomicAssemblies.GRCh38p13(), testFilePath);
        List<? extends RepetitiveRegion> regions = instance.parseToList();

        assertThat(regions, hasSize(7));
    }
}