package org.jax.svanna.cli;

import org.jax.svanna.core.overlap.GeneOverlapper;
import org.jax.svanna.core.service.GeneService;
import org.jax.svanna.io.service.SilentGenesGeneService;
import org.jax.svanna.test.TestVariants;
import org.monarchinitiative.svart.GenomicAssemblies;
import org.monarchinitiative.svart.GenomicAssembly;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class TestDataConfig {

    /**
     * Small transcript file that contains GENCODE transcripts of the following genes:
     * <ul>
     *     <li><em>SURF1</em></li>
     *     <li><em>SURF2</em></li>
     *     <li><em>FBN1</em></li>
     *     <li><em>ZNF436</em></li>
     *     <li><em>ZBTB48</em></li>
     *     <li><em>HNF4A</em></li>
     *     <li><em>GCK</em></li>
     *     <li><em>BRCA2</em></li>
     *     <li><em>COL4A5</em></li> (on <code>chrX</code>)
     *     <li><em>SRY</em></li> (on <code>chrY</code>)
     * </ul>
     */
    private static final Path SILENT_GENE_DATA = Paths.get("src/test/resources/gencode.10genes.v38.basic.annotation.json.gz");

    @Bean
    public GenomicAssembly genomicAssembly() {
        return GenomicAssemblies.GRCh38p13();
    }

    @Bean
    public TestVariants testVariants(GenomicAssembly genomicAssembly) {
        return new TestVariants(genomicAssembly);
    }

    @Bean
    public GeneService geneService(GenomicAssembly assembly) throws IOException {
        return SilentGenesGeneService.of(assembly, SILENT_GENE_DATA);
    }

    @Bean
    public GeneOverlapper geneOverlapper(GeneService geneService) {
        return GeneOverlapper.of(geneService);
    }

}
