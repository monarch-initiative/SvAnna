package org.monarchinitiative.svanna.cli.writer.html.svg;

import org.monarchinitiative.svanna.cli.TestDataConfig;
import org.monarchinitiative.svanna.core.overlap.GeneOverlap;
import org.monarchinitiative.svanna.core.overlap.GeneOverlapper;
import org.monarchinitiative.svanna.model.landscape.enhancer.Enhancer;
import org.monarchinitiative.svanna.model.landscape.repeat.RepetitiveRegion;
import org.monarchinitiative.svanna.test.TestVariants;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.svart.GenomicBreakendVariant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.monarchinitiative.sgenes.model.Gene;

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
    public void testWriteSvg() {
        GenomicBreakendVariant variant = testVariants.translocations().translocationWhereOneCdsIsDisruptedAndTheOtherIsNot();
        List<Gene> genes = geneOverlapper.getOverlaps(variant).stream().map(GeneOverlap::gene).collect(Collectors.toList());
        List<Enhancer> enhancerList = List.of();
        List<RepetitiveRegion> repeats = List.of();
        TranslocationSvgGenerator gen = new TranslocationSvgGenerator(variant, genes, enhancerList, repeats, List.of(TestData.surf2Haploinsufficiency()));
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
