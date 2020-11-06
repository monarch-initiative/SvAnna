package org.jax.svann.viz.svg;

import de.charite.compbio.jannovar.data.ReferenceDictionary;
import de.charite.compbio.jannovar.reference.GenomeInterval;
import de.charite.compbio.jannovar.reference.PositionType;
import de.charite.compbio.jannovar.reference.Strand;
import de.charite.compbio.jannovar.reference.TranscriptModel;
import org.jax.svann.TestBase;
import org.jax.svann.genomicreg.Enhancer;
import org.jax.svann.genomicreg.TestCoordinatePair;
import org.jax.svann.genomicreg.TestGenomicPosition;
import org.jax.svann.reference.*;
import org.jax.svann.reference.genome.Contig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import static org.jax.svann.reference.Strand.FWD;

public class SvSvgGeneratorTest extends TestBase {

    private static SvSvgGenerator generator;



    @BeforeAll
    public static void init() {
        Collection<TranscriptModel> surf1list = JANNOVAR_DATA.getTmByGeneSymbol().asMap().get("SURF1");
        ReferenceDictionary rd = JANNOVAR_DATA.getRefDict();
        List<Enhancer> enhancers = List.of();
        // Create a genome interval that takes our exon 5 of 9
        Contig chr9 = GENOME_ASSEMBLY.getContigByName("chr9").get();
        int start = 133_353_500;
        int end = 133_354_500;
        GenomeInterval gi = new GenomeInterval(rd, Strand.FWD, chr9.getId(), start, end, PositionType.ONE_BASED);
        CoordinatePair cpair = TestCoordinatePair.of(new TestGenomicPosition(chr9, start, FWD),
                new TestGenomicPosition(chr9, end, FWD));
        ArrayList<CoordinatePair> cpairlist = new ArrayList<>();
        cpairlist.add(cpair);
        String deletion = "Deletion, ch9:133_353_500-133_354_500";
        generator = new DeletionSvgGenerator(new ArrayList<>(surf1list), enhancers, cpairlist);
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
