package org.monarchinitiative.svanna.core.service;

import org.monarchinitiative.svanna.core.TestContig;
import org.monarchinitiative.svanna.core.TestDataConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.svart.*;
import org.monarchinitiative.svart.assembly.GenomicAssembly;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.monarchinitiative.sgenes.model.Gene;

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

    @ParameterizedTest
    @CsvSource({
            "HGNC:11474, SURF1,  4919",
            "HGNC:11475, SURF2,  4609",
            "HGNC:5024,  HNF4A, 77146",
    })
    public void bySymbol(String termId, String symbol, int length) {
        List<Gene> genes = geneService.byHgncId(TermId.of(termId));
        assertThat(genes.isEmpty(), equalTo(false));

        Gene gene = genes.get(0);

        assertThat(gene.symbol(), equalTo(symbol));
        assertThat(gene.location().length(), equalTo(length));
    }

    @ParameterizedTest
    @CsvSource({
            "1,  23290000, 23291000, ''", // between ZBTB48 and ZNF436 genes
            "9,  133356484, 133356548, SURF1",
            "9,  133357000, 133358000, SURF2",
            "9,  133356550, 133356551, 'SURF1,SURF2'",
            "9,  133356540, 133356540, SURF1", // interval with length 0
            "15,  48550000,  48550100,  FBN1",
    })
    public void overlappingGenes(String contigName, int start, int end, String names) {
        Contig contig = genomicAssembly.contigByName(contigName);
        GenomicRegion region = GenomicRegion.of(contig, Strand.POSITIVE, CoordinateSystem.zeroBased(), start, end);
        QueryResult<Gene> genes = geneService.overlappingGenes(region);

        Set<String> actual = genes.overlapping().stream().map(Gene::symbol).collect(Collectors.toUnmodifiableSet());
        String[] expected = names.split(",");
        if (expected.length == 1 && expected[0].equals(""))
            assertThat(actual, hasSize(0));
        else
            assertThat(actual, hasItems(expected));
    }

    @Test
    public void overlappingGenes_unknownContig() {
        GenomicRegion region = GenomicRegion.of(TestContig.of(200, 100), Strand.POSITIVE, CoordinateSystem.zeroBased(), 50, 60);
        assertThat(geneService.overlappingGenes(region).isEmpty(), is(true));
    }
}
