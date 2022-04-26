package org.monarchinitiative.svanna.cli.writer.html.svg;

import org.monarchinitiative.svanna.cli.TestDataConfig;
import org.monarchinitiative.svanna.core.service.GeneService;
import org.monarchinitiative.svanna.model.landscape.enhancer.Enhancer;
import org.monarchinitiative.svanna.test.TestVariants;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.svart.GenomicVariant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.monarchinitiative.sgenes.model.Gene;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(classes = TestDataConfig.class)
public class InversionSvgGeneratorTest {

    @Autowired
    public TestVariants testVariants;

    @Autowired
    public GeneService geneService;

    @Test
    public void testWriteSvg() {
        GenomicVariant variant = testVariants.inversions().gckExonic();
        List<Gene> genes = geneService.byHgncId(TermId.of("HGNC:4195"));
        List<Enhancer> enhancerList = List.of();
        SvSvgGenerator gen = new InversionSvgGenerator(variant, genes, enhancerList, List.of(), List.of(TestData.gckHaploinsufficiency()));
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
