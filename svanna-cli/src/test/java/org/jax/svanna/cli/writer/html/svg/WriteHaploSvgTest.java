package org.jax.svanna.cli.writer.html.svg;


import org.jax.svanna.cli.TestDataConfig;
import org.jax.svanna.core.overlap.GeneOverlap;
import org.jax.svanna.core.overlap.GeneOverlapper;
import org.jax.svanna.model.landscape.dosage.Dosage;
import org.jax.svanna.model.landscape.dosage.DosageRegion;
import org.jax.svanna.model.landscape.dosage.DosageSensitivity;
import org.jax.svanna.model.landscape.dosage.DosageSensitivityEvidence;
import org.jax.svanna.model.landscape.enhancer.Enhancer;
import org.jax.svanna.model.landscape.repeat.RepetitiveRegion;
import org.jax.svanna.test.TestVariants;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.svart.CoordinateSystem;
import org.monarchinitiative.svart.GenomicRegion;
import org.monarchinitiative.svart.Strand;
import org.monarchinitiative.svart.Variant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import xyz.ielis.silent.genes.model.Gene;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Test writing an SVG with a triplo/haplo track.
 */

@SpringBootTest(classes = TestDataConfig.class)
public class WriteHaploSvgTest {

    @Autowired
    public TestVariants testVariants;

    @Autowired
    public GeneOverlapper geneOverlapper;

    @Test
    public void testWriteSvg() {
        Variant singeExonDel = testVariants.deletions().surf1SingleExon_exon2();
        List<Gene> genes = geneOverlapper.getOverlaps(singeExonDel).stream()
                .map(GeneOverlap::gene)
                .collect(Collectors.toUnmodifiableList());
        int min = genes.stream().mapToInt(g -> g.startOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased())).min().orElse(0);
        int max = genes.stream().mapToInt(g -> g.endOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased())).max().orElse(min+2);
        Dosage dosage = Dosage.of("id1", DosageSensitivity.HAPLOINSUFFICIENCY, DosageSensitivityEvidence.SUFFICIENT_EVIDENCE);
        GenomicRegion region = GenomicRegion.of(singeExonDel.contig(), Strand.POSITIVE, CoordinateSystem.zeroBased(), min, max);
        DosageRegion dosageRegion = DosageRegion.of(region, dosage);

        List<Enhancer> enhancerList = List.of();
        List<RepetitiveRegion> repetitives = List.of(); // empty
        List<DosageRegion> dosageRegions = List.of(dosageRegion); // empty
        SvSvgGenerator gen = new DeletionSvgGenerator(singeExonDel, genes, enhancerList, repetitives, dosageRegions);
        String svg = gen.getSvg();
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
