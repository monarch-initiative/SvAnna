package org.jax.svanna.io;

import org.jax.svanna.model.gene.GeneIdentifier;
import org.jax.svanna.test.TestVariants;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.svart.GenomicAssemblies;
import org.monarchinitiative.svart.GenomicAssembly;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.AbstractMap;
import java.util.Map;
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
        GeneIdentifier surf1 = GeneIdentifier.of("SURF1", TermId.of("NCBIGene:6834"));
        GeneIdentifier surf2 =  GeneIdentifier.of("SURF2", TermId.of("NCBIGene:6835"));
        GeneIdentifier fbn1 =  GeneIdentifier.of("FBN1", TermId.of("NCBIGene:2200"));
        GeneIdentifier znf436 =  GeneIdentifier.of("ZNF436", TermId.of("NCBIGene:80818"));
        GeneIdentifier zbtb48 =  GeneIdentifier.of("ZBTB48", TermId.of("NCBIGene:3104"));
        GeneIdentifier hnf4a =  GeneIdentifier.of("HNF4A", TermId.of("NCBIGene:3172"));
        GeneIdentifier gck =  GeneIdentifier.of("GCK", TermId.of("NCBIGene:2645"));
        GeneIdentifier brca2 =  GeneIdentifier.of("BRCA2", TermId.of("NCBIGene:675"));
        GeneIdentifier col4a5 = GeneIdentifier.of("COL4A5", TermId.of("NCBIGene:1287"));
        GeneIdentifier sry = GeneIdentifier.of("SRY", TermId.of("NCBIGene:6736"));

        return Stream.of(
                new AbstractMap.SimpleImmutableEntry<>("SURF1", surf1),
                new AbstractMap.SimpleImmutableEntry<>("SURF2", surf2),
                new AbstractMap.SimpleImmutableEntry<>("FBN1", fbn1),
                new AbstractMap.SimpleImmutableEntry<>("ZNF436", znf436),
                new AbstractMap.SimpleImmutableEntry<>("ZBTB48", zbtb48),
                new AbstractMap.SimpleImmutableEntry<>("HNF4A", hnf4a),
                new AbstractMap.SimpleImmutableEntry<>("GCK", gck),
                new AbstractMap.SimpleImmutableEntry<>("BRCA2", brca2),
                new AbstractMap.SimpleImmutableEntry<>("COL4A5", col4a5),
                new AbstractMap.SimpleImmutableEntry<>("SRY", sry)
        )
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
