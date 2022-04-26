package org.monarchinitiative.svanna.ingest.parse.tad;

import org.monarchinitiative.svanna.model.landscape.tad.TadBoundary;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.svart.CoordinateSystem;
import org.monarchinitiative.svart.assembly.GenomicAssemblies;
import org.monarchinitiative.svart.assembly.GenomicAssembly;
import org.monarchinitiative.svart.Strand;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class McArthur2021TadBoundariesParserTest {

    private static final Path chainFile = Paths.get("src/test/resources/hg19ToHg38.over.chain.gz");

    private static final GenomicAssembly genomicAssembly = GenomicAssemblies.GRCh38p13();

    @Test
    public void parse() throws Exception {
        List<? extends TadBoundary> records;
        Path tadsPath = Paths.get("src/test/resources/tad/emcarthur-TAD-stability-heritability.3records.bed");
        try (InputStream is = Files.newInputStream(tadsPath)) {
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