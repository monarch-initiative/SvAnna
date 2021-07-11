package org.jax.svanna.core.reference.transcripts;

import de.charite.compbio.jannovar.data.JannovarData;
import org.jax.svanna.core.TestDataConfig;
import org.jax.svanna.core.reference.Gene;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.svart.CoordinateSystem;
import org.monarchinitiative.svart.GenomicAssembly;
import org.monarchinitiative.svart.GenomicRegion;
import org.monarchinitiative.svart.Strand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;

@SpringBootTest(classes = TestDataConfig.class)
public class JannovarGeneServiceTest {

    @Autowired
    public GenomicAssembly genomicAssembly;

    @Autowired
    public JannovarData jannovarData;

    @Test
    public void initialize() {
        JannovarGeneService geneService = JannovarGeneService.of(genomicAssembly, jannovarData);
        GenomicRegion region = GenomicRegion.of(genomicAssembly.contigByName("9"), Strand.POSITIVE, CoordinateSystem.zeroBased(), 133_356_484, 133_356_545);

        List<Gene> genes = geneService.overlappingGenes(region);

        assertThat(genes, hasSize(2));
        assertThat(genes.stream().map(Gene::geneSymbol).collect(Collectors.toSet()), hasItems("SURF1", "SURF2"));
    }
}