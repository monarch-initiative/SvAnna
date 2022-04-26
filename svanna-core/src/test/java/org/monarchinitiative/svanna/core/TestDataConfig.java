package org.monarchinitiative.svanna.core;

import org.monarchinitiative.svanna.core.service.ConstantGeneDosageDataService;
import org.monarchinitiative.svanna.core.service.GeneDosageDataService;
import org.monarchinitiative.svanna.core.service.GeneService;
import org.monarchinitiative.svanna.test.TestVariants;
import org.monarchinitiative.svart.assembly.GenomicAssemblies;
import org.monarchinitiative.svart.assembly.GenomicAssembly;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.monarchinitiative.sgenes.model.GeneIdentifier;
import org.monarchinitiative.sgenes.model.Identifier;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
     * The file is prepared by `org.jax.svanna.ingest.MakeSmallGencodeFileTest.makeSmallGencodeFile()`.
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
    public GeneDosageDataService geneDosageDataService(GeneService geneService) {
        return new ConstantGeneDosageDataService(geneService);
    }

    @Bean
    public Map<String, GeneIdentifier> geneWithIdMap() {
        GeneIdentifier surf1 = GeneIdentifier.of("NCBIGene:6834", "SURF1", null, null);
        GeneIdentifier surf2 = GeneIdentifier.of("NCBIGene:6835", "SURF2", null, null);
        GeneIdentifier fbn1 = GeneIdentifier.of("NCBIGene:2200", "FBN1", null, null);
        GeneIdentifier znf436 =  GeneIdentifier.of("NCBIGene:80818", "ZNF436", null, null);
        GeneIdentifier zbtb48 =  GeneIdentifier.of("NCBIGene:3104", "ZBTB48", null, null);
        GeneIdentifier hnf4a =  GeneIdentifier.of("NCBIGene:3172", "HNF4A", null, null);
        GeneIdentifier gck =  GeneIdentifier.of("NCBIGene:2645", "GCK", null, null);
        GeneIdentifier brca2 =  GeneIdentifier.of("NCBIGene:675", "BRCA2", null, null);
        GeneIdentifier col4a5 = GeneIdentifier.of("NCBIGene:1287", "COL4A5", null, null);
        GeneIdentifier sry = GeneIdentifier.of("NCBIGene:6736", "SRY", null, null);

        return Stream.of(surf1, surf2, fbn1, znf436, zbtb48, hnf4a, gck, brca2, col4a5, sry)
                .collect(Collectors.toMap(Identifier::symbol, Function.identity()));
    }
}
