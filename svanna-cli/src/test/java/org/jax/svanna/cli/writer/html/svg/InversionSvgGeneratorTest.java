package org.jax.svanna.cli.writer.html.svg;

import org.jax.svanna.cli.TestDataConfig;
import org.jax.svanna.core.landscape.Enhancer;
import org.jax.svanna.core.overlap.Overlap;
import org.jax.svanna.core.overlap.Overlapper;
import org.jax.svanna.core.reference.Transcript;
import org.jax.svanna.test.TestVariants;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.svart.Variant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(classes = TestDataConfig.class)
public class InversionSvgGeneratorTest {

    @Autowired
    public TestVariants testVariants;

    @Autowired
    public Overlapper overlapper;

    @Test
    public void testWriteSvg() {
        Variant variant = testVariants.inversions().gckExonic();
        List<Transcript> transcripts = overlapper.getOverlaps(variant).stream()
                .map(Overlap::getTranscriptModel)
                .collect(Collectors.toList());
        List<Enhancer> enhancerList = List.of();
        SvSvgGenerator gen = new InversionSvgGenerator(variant, transcripts, enhancerList);
        String svg = gen.getSvg();
        assertNotNull(svg);
        System.out.println(svg);
        try {
            String path = "target/inversion.svg";
            BufferedWriter writer = new BufferedWriter(new FileWriter(path));
            writer.write(svg);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
