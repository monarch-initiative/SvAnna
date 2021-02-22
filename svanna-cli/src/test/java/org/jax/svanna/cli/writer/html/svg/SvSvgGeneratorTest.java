package org.jax.svanna.cli.writer.html.svg;

import org.jax.svanna.cli.TestDataConfig;
import org.jax.svanna.core.reference.TranscriptService;
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
    public TranscriptService transcriptService;

    @Autowired
    public TestVariants testVariants;

    @Test
    public void testWriteSvg() {
        SvSvgGenerator generator = new DeletionSvgGenerator(testVariants.deletions().gckUpstreamIntergenic_affectingEnhancer(), List.copyOf(transcriptService.getTxBySymbolMap().get("GCK")),
                List.of()
        );
        String svg = generator.getSvg();
        assertNotNull(svg);
        System.out.println(svg);
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
