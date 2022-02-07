package org.jax.svanna.ingest.parse.enhancer.fantom;

import org.jax.svanna.ingest.parse.enhancer.AbstractEnhancerParserTest;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.svart.CoordinateSystem;
import org.monarchinitiative.svart.Strand;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class FantomEnhancerParserTest extends AbstractEnhancerParserTest {

    @Test
    public void parse() throws Exception {
        Path countsPath = Paths.get(FantomEnhancerParserTest.class.getResource("F5.hg38.enhancers.expression.matrix.3lines.tsv.gz").getPath());
        Path samplesPath = Paths.get(FantomEnhancerParserTest.class.getResource("Human.sample_name2library_id.txt").getPath());
        FantomEnhancerParser instance = new FantomEnhancerParser(GRCh38p13, countsPath, samplesPath, UBERON_TO_HPO);
        List<? extends FEnhancer> enhancers = instance.parseToList();

        assertThat(enhancers, hasSize(2));
        FEnhancer first = enhancers.get(0);
        assertThat(first.contig(), equalTo(GRCh38p13.contigByName("chr10")));
        assertThat(first.strand(), equalTo(Strand.POSITIVE));
        assertThat(first.coordinateSystem(), equalTo(CoordinateSystem.oneBased()));
        assertThat(first.start(), equalTo(100_006_233));
        assertThat(first.end(), equalTo(100_006_603));
        assertThat(first.id(), equalTo("F5:chr10:100006233-100006603"));

        assertThat(first.isDevelopmental(), equalTo(false));
        assertThat(first.tau(), closeTo(.676366922577466, 1E-15));
        assertThat(first.totalReadCounts(), closeTo(.050698, 1E-6));
    }
}