package org.jax.svanna.cli.writer.html.svg;

import org.jax.svanna.cli.TestDataConfig;
import org.jax.svanna.core.service.GeneService;
import org.jax.svanna.test.TestVariants;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

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
        SvSvgGenerator generator = new DeletionSvgGenerator(testVariants.deletions().gckUpstreamIntergenic_affectingEnhancer(), List.of(geneService.bySymbol("GCK")),
                List.of(), List.of()
        );
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
