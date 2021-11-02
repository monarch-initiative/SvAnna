package org.jax.svanna.cli.writer.html.svg;

import org.jax.svanna.cli.TestDataConfig;
import org.jax.svanna.core.overlap.GeneOverlap;
import org.jax.svanna.core.overlap.GeneOverlapper;
import org.jax.svanna.model.gene.Gene;
import org.jax.svanna.model.landscape.enhancer.Enhancer;
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
    public GeneOverlapper geneOverlapper;

    @Test
    public void testWriteSvg() {
        Variant variant = testVariants.inversions().gckExonic();
        List<Gene> genes = geneOverlapper.getOverlaps(variant).stream().map(GeneOverlap::gene).collect(Collectors.toList());
        List<Enhancer> enhancerList = List.of();
        SvSvgGenerator gen = new InversionSvgGenerator(variant, genes, enhancerList, List.of());
        String svg = gen.getSvg();
        assertNotNull(svg);
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
