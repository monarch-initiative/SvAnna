package org.jax.svanna.ingest.parse.population;

import org.jax.svanna.core.reference.PopulationVariant;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.svart.GenomicAssemblies;
import org.monarchinitiative.svart.GenomicAssembly;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class GnomadSVFileParserTest {

    private static final Path testFile = Paths.get(GnomadSVFileParser.class.getResource("/variants/variants_for_nstd166.5lines.csv.gz").getPath());

    private static final GenomicAssembly genomicAssembly = GenomicAssemblies.GRCh38p13();

    @Test
    public void parseToList() throws Exception {
        GnomadSVFileParser instance = new GnomadSVFileParser(genomicAssembly, testFile);
        List<PopulationVariant> variants = instance.parseToList();

        // TODO - finish or remove
//        variants.forEach(System.err::println);
//        assertThat(variants, hasSize(0));
    }
}