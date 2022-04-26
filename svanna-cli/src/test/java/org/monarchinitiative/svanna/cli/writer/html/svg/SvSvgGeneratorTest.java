package org.monarchinitiative.svanna.cli.writer.html.svg;

import org.monarchinitiative.svanna.cli.TestDataConfig;
import org.monarchinitiative.svanna.core.service.GeneService;
import org.monarchinitiative.svanna.test.TestVariants;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.monarchinitiative.sgenes.model.Gene;

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
        List<Gene> genes = geneService.byHgncId(TermId.of("HGNC:4195"));
        SvSvgGenerator generator = new DeletionSvgGenerator(testVariants.deletions().gckUpstreamIntergenic_affectingEnhancer(),
                genes,
                List.of(),
                List.of(),
                List.of(TestData.gckHaploinsufficiency()));
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
