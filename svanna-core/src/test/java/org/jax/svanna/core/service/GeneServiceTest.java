package org.jax.svanna.core.service;

import org.jax.svanna.core.TestContig;
import org.jax.svanna.core.TestDataConfig;
import org.jax.svanna.model.gene.Gene;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.monarchinitiative.svart.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@SpringBootTest(classes = TestDataConfig.class)
public class GeneServiceTest {

    @Autowired
    public GeneService geneService;
    @Autowired
    private GenomicAssembly genomicAssembly;

    @Test
    public void getChromosomeMap() {
        assertThat(geneService.getChromosomeMap().keySet(), hasItems(1, 7, 9, 13, 15, 20, 23, 24));
    }

    @ParameterizedTest
    @CsvSource({
            "SURF1, 4681",
            "HNF4A, 77045",
    })
    public void bySymbol(String symbol, int length) {
        Gene gene = geneService.bySymbol(symbol);

        assertThat(gene.geneSymbol(), equalTo(symbol));
        assertThat(gene.length(), equalTo(length));
    }

    @ParameterizedTest
    @CsvSource({
            "9,  133356485, 133356544, ''", // right between the SURF1 and SURF2 genes
            "9,  133356484, 133356544, SURF1",
            "9,  133356485, 133356545, SURF2",
            "9,  133356484, 133356545, 'SURF1,SURF2'",
            "9,  133356480, 133356480, SURF1", // interval with length 0
            "15,  48550000,  48550100,  FBN1",
    })
    public void overlappingGenes(String contigName, int start, int end, String names) {
        Contig contig = genomicAssembly.contigByName(contigName);
        GenomicRegion region = GenomicRegion.of(contig, Strand.POSITIVE, CoordinateSystem.zeroBased(), start, end);
        List<Gene> genes = geneService.overlappingGenes(region);

        Set<String> actual = genes.stream().map(Gene::geneSymbol).collect(Collectors.toUnmodifiableSet());
        String[] expected = names.split(",");
        if (expected.length == 1 && expected[0].equals(""))
            assertThat(actual, hasSize(0));
        else
            assertThat(actual, hasItems(expected));
    }

    @Test
    public void overlappingGenes_unknownContig() {
        GenomicRegion region = GenomicRegion.of(TestContig.of(200, 100), Strand.POSITIVE, CoordinateSystem.zeroBased(), 50, 60);
        assertThat(geneService.overlappingGenes(region), is(empty()));
    }
}