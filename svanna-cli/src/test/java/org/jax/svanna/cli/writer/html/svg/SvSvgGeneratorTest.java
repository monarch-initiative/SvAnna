package org.jax.svanna.cli.writer.html.svg;

import org.jax.svanna.cli.TestDataConfig;
import org.jax.svanna.core.service.GeneService;
import org.jax.svanna.test.TestVariants;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import xyz.ielis.silent.genes.model.Gene;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(classes = TestDataConfig.class)
public class SvSvgGeneratorTest {

    @Autowired
    public GeneService geneService;

    @Autowired
    public TestVariants testVariants;

    @Test
    public void testWriteSvg() {
        //noinspection OptionalGetWithoutIsPresent
        List<Gene> genes = List.of(geneService.byHgncId(TermId.of("HGNC:4195")).get());
        SvSvgGenerator generator = new DeletionSvgGenerator(testVariants.deletions().gckUpstreamIntergenic_affectingEnhancer(), genes,
                List.of(), List.of());
        String svg = generator.getSvg();
        assertNotNull(svg);
        try {
            String path = "target/deletion.svg";
            BufferedWriter writer = new BufferedWriter(new FileWriter(path));
            writer.write(svg);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
