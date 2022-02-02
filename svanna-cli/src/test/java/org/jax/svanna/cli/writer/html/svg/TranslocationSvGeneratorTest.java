package org.jax.svanna.cli.writer.html.svg;

import org.jax.svanna.cli.TestDataConfig;
import org.jax.svanna.core.overlap.GeneOverlap;
import org.jax.svanna.core.overlap.GeneOverlapper;
import org.jax.svanna.model.landscape.enhancer.Enhancer;
import org.jax.svanna.test.TestVariants;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.svart.GenomicBreakendVariant;
import org.monarchinitiative.svart.GenomicVariant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import xyz.ielis.silent.genes.model.Gene;

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
    public GeneOverlapper geneOverlapper;

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
        GenomicVariant variant = testVariants.translocations().translocationWhereOneCdsIsDisruptedAndTheOtherIsNot();
        List<Gene> genes = geneOverlapper.getOverlaps(variant).stream().map(GeneOverlap::gene).collect(Collectors.toList());
        List<Enhancer> enhancerList = List.of();
        TranslocationSvgGenerator gen = new TranslocationSvgGenerator(variant, ((GenomicBreakendVariant) variant), genes, enhancerList, List.of());
        String svg = gen.getSvg();
        assertNotNull(svg);
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
