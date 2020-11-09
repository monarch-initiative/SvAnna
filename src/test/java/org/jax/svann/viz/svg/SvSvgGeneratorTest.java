package org.jax.svann.viz.svg;

import org.jax.svann.TestBase;
import org.jax.svann.genomicreg.Enhancer;
import org.jax.svann.genomicreg.TestCoordinatePair;
import org.jax.svann.genomicreg.TestGenomicPosition;
import org.jax.svann.reference.CoordinatePair;
import org.jax.svann.reference.genome.Contig;
import org.jax.svann.reference.transcripts.JannovarTranscriptService;
import org.jax.svann.reference.transcripts.SvAnnTxModel;
import org.jax.svann.reference.transcripts.TranscriptService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import static org.jax.svann.reference.Strand.FWD;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class SvSvgGeneratorTest extends TestBase {

    private static SvSvgGenerator generator;

    private static TranscriptService TX_SERVICE;


    @BeforeAll
    public static void init() {
        TX_SERVICE = JannovarTranscriptService.of(GENOME_ASSEMBLY, JANNOVAR_DATA);
    }

    @BeforeEach
    public void setUp() {
        Set<SvAnnTxModel> surf1list = TX_SERVICE.getTxBySymbolMap().get("SURF1");
        List<Enhancer> enhancers = List.of();
        // Create a genome interval that takes our exon 5 of 9
        Contig chr9 = GENOME_ASSEMBLY.getContigByName("chr9").orElseThrow();
        int start = 133_353_500;
        int end = 133_354_500;
        CoordinatePair cpair = TestCoordinatePair.of(new TestGenomicPosition(chr9, start, FWD),
                new TestGenomicPosition(chr9, end, FWD));
        String deletion = "Deletion, ch9:133_353_500-133_354_500";
        generator = new DeletionSvgGenerator(List.copyOf(surf1list), enhancers, List.of(cpair));
    }

    @Test
    public void testWriteSvg() {
        assertNotNull(generator);
        String svg = generator.getSvg();
        assertNotNull(svg);
        System.out.println(svg);
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
