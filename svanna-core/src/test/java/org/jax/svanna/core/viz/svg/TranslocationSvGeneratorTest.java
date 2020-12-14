package org.jax.svanna.core.viz.svg;

/**
 *  /**
 *      * Translocation where one CDS is disrupted and the other is not
 *      * <p>
 *      * left mate, SURF2:NM_017503.5 intron 3 (disrupted CDS)
 *      * chr9:133_359_000 (+)
 *      * right mate, upstream from BRCA2 (not disrupted)
 *      * chr13:32_300_000 (+)
 *      */

import org.jax.svanna.core.TestDataConfig;
import org.jax.svanna.core.overlap.Overlap;
import org.jax.svanna.core.reference.Enhancer;
import org.jax.svanna.core.reference.Transcript;
import org.jax.svanna.test.TestVariants;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.variant.api.Breakended;
import org.monarchinitiative.variant.api.Variant;
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
    public org.jax.svanna.core.overlap.Overlapper overlapper;


    @Test
    public void testWriteSvg() {
        Variant variant = testVariants.translocations().translocationWhereOneCdsIsDisruptedAndTheOtherIsNot();
        List<Transcript> transcriptModels = overlapper.getOverlapList(variant).stream()
                .map(Overlap::getTranscriptModel).collect(Collectors.toList());
        List<Enhancer> enhancerList = List.of();
        TranslocationSvgGenerator gen = new TranslocationSvgGenerator(variant, ((Breakended) variant), transcriptModels, enhancerList);
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
