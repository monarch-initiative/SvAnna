package org.jax.svanna.cli.writer.html.svg;

import org.jax.svanna.cli.TestDataConfig;
import org.jax.svanna.core.landscape.Enhancer;
import org.jax.svanna.core.overlap.Overlap;
import org.jax.svanna.core.overlap.Overlapper;
import org.jax.svanna.core.reference.Transcript;
import org.jax.svanna.test.TestVariants;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.svart.BreakendVariant;
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
public class TranslocationSvGeneratorTest {

    @Autowired
    public TestVariants testVariants;

    @Autowired
    public Overlapper overlapper;

    /**
     * Translocation where one CDS is disrupted and the other is not
     * <p>
     * left mate, SURF2:NM_017503.5 intron 3 (disrupted CDS)
     * chr9:133_359_000 (+)
     * right mate, upstream from BRCA2 (not disrupted)
     * chr13:32_300_000 (+)
     */
    @Test
    @Disabled
    public void testWriteSvg() {
        Variant variant = testVariants.translocations().translocationWhereOneCdsIsDisruptedAndTheOtherIsNot();
        List<Transcript> transcriptModels = overlapper.getOverlaps(variant).stream()
                .map(Overlap::getTranscriptModel).collect(Collectors.toList());
        List<Enhancer> enhancerList = List.of();
        TranslocationSvgGenerator gen = new TranslocationSvgGenerator(variant, ((BreakendVariant) variant), transcriptModels, enhancerList);
        String svg = gen.getSvg();
        assertNotNull(svg);
        System.out.println(svg);
        try {
            String path = "target/translocation.svg";
            BufferedWriter writer = new BufferedWriter(new FileWriter(path));
            writer.write(svg);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}