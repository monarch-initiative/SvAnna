package org.jax.svanna.io;

import org.jax.svanna.test.TestVariants;
import org.monarchinitiative.svart.assembly.GenomicAssemblies;
import org.monarchinitiative.svart.assembly.GenomicAssembly;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import xyz.ielis.silent.genes.model.GeneIdentifier;
import xyz.ielis.silent.genes.model.Identifier;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Configuration
public class TestDataConfig {

    @Bean
    public GenomicAssembly genomicAssembly() throws Exception {
        return GenomicAssemblies.GRCh38p13();
    }

    @Bean
    public TestVariants testVariants(GenomicAssembly genomicAssembly) {
        return new TestVariants(genomicAssembly);
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
