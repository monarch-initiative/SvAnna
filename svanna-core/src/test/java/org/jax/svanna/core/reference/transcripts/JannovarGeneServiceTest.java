package org.jax.svanna.core.reference.transcripts;

import de.charite.compbio.jannovar.data.JannovarData;
import de.charite.compbio.jannovar.data.JannovarDataSerializer;
import org.jax.svanna.core.reference.Gene;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.svart.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;

public class JannovarGeneServiceTest {

    private static final GenomicAssembly ASSEMBLY = GenomicAssemblies.GRCh38p13();
    private static final Path JANNOVAR_CACHE = Paths.get("/home/ielis/soft/jannovar/v0.35/hg38_refseq.ser");


    @Test
    @Disabled
    public void initialize() throws Exception {
        JannovarData jannovarData = new JannovarDataSerializer(JANNOVAR_CACHE.toString()).load();
        JannovarGeneService service = JannovarGeneService.of(ASSEMBLY, jannovarData);

        GenomicRegion region = GenomicRegion.of(ASSEMBLY.contigByName("9"), Strand.POSITIVE, CoordinateSystem.zeroBased(), 133_352_000, 133_366_000);
        List<Gene> genes = service.overlappingGenes(region);

        assertThat(genes, hasSize(3));
        assertThat(genes.stream().map(Gene::geneSymbol).collect(Collectors.toSet()), hasItems("SURF1", "SURF2", "SURF4"));
    }
}