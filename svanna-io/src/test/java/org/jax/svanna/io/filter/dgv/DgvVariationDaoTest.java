package org.jax.svanna.io.filter.dgv;

import org.jax.svanna.core.reference.PopulationVariant;
import org.jax.svanna.io.TestDataConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.svart.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = TestDataConfig.class)
public class DgvVariationDaoTest {

    private static final Path TEST_DGV_FILE = Paths.get("src/test/resources/org/jax/svanna/io/filter/dgv/GRCh38_hg38_variants_2020-02-25.5000.txt.gz");

    @Autowired
    public GenomicAssembly genomicAssembly;

    private DgvVariantDao featureSource;

    @BeforeEach
    public void setUp() throws Exception {
        featureSource = new DgvVariantDao(genomicAssembly, TEST_DGV_FILE);
    }

    @AfterEach
    public void tearDown() throws Exception {
        featureSource.close();
    }

    @Test
    public void getOverlappingFeatures() {
        int start = 180_900, end = 181_000;
        GenomicRegion query = GenomicRegion.of(genomicAssembly.contigByName("1"), Strand.POSITIVE, CoordinateSystem.zeroBased(),
                Position.of(start), Position.of(end));
        List<PopulationVariant> features = featureSource.getOverlapping(query);

        assertThat(features, hasSize(5));
        assertTrue(features.stream().allMatch(f -> f.start() <= end && f.end() >= start));

        // test one particular feature
        PopulationVariant feature = features.get(3);
        assertThat(feature.contigName(), equalTo("1"));
        assertThat(feature.start(), equalTo(180_918));
        assertThat(feature.end(), equalTo(180_991));
        assertThat(feature.coordinateSystem(), equalTo(CoordinateSystem.oneBased()));
        assertThat(feature.strand(), equalTo(Strand.POSITIVE));
        assertThat(feature.variantType(), equalTo(VariantType.DEL));
        assertThat(feature.alleleFrequency(), equalTo(71.428573F));
    }

    @Test
    public void getOverlappingFeatures_returnsEmptyWhenAskingForEntryOnUnknownChromosome() {
        int start = 180_900, end = 181_000;
        GenomicRegion query = GenomicRegion.of(genomicAssembly.contigByName("2"), Strand.POSITIVE, CoordinateSystem.zeroBased(),
                Position.of(start), Position.of(end));
        List<PopulationVariant> features = featureSource.getOverlapping(query);

        assertThat(features, empty());
    }
}