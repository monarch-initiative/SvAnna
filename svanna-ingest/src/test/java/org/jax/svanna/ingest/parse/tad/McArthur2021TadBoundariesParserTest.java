package org.jax.svanna.ingest.parse.tad;

import org.jax.svanna.ingest.parse.population.GnomadSVFileParser;
import org.jax.svanna.model.landscape.tad.TadBoundary;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.svart.CoordinateSystem;
import org.monarchinitiative.svart.GenomicAssemblies;
import org.monarchinitiative.svart.GenomicAssembly;
import org.monarchinitiative.svart.Strand;

import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class McArthur2021TadBoundariesParserTest {

    private static final Path chainFile = Paths.get(GnomadSVFileParser.class.getResource("/liftover/hg19ToHg38.over.chain.gz").getPath());

    private static final GenomicAssembly genomicAssembly = GenomicAssemblies.GRCh38p13();

    @Test
    public void parse() throws Exception {
        List<? extends TadBoundary> records;
        try (InputStream is = McArthur2021TadBoundariesParserTest.class.getResourceAsStream("/tads/emcarthur-TAD-stability-heritability.3records.bed")) {
            McArthur2021TadBoundariesParser instance = new McArthur2021TadBoundariesParser(genomicAssembly, is, chainFile);
            records = instance.parseToList();
        }
        assertThat(records, hasSize(3));

        TadBoundary first = records.get(0);
        assertThat(first.id(), equalTo("chr1:600000-700000"));
        assertThat((double) first.stability(), closeTo(0.65444404, 1E-8));

        assertThat(first.contig(), equalTo(genomicAssembly.contigByName("chr1")));
        assertThat(first.strand(), equalTo(Strand.POSITIVE));
        assertThat(first.coordinateSystem(), equalTo(CoordinateSystem.zeroBased()));
        assertThat(first.start(), equalTo(664_620));
        assertThat(first.end(), equalTo(764_620));
    }
}