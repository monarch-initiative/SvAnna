package org.jax.svanna.ingest.parse.population;

import org.jax.svanna.model.landscape.variant.PopulationVariant;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.svart.*;
import org.monarchinitiative.svart.assembly.GenomicAssemblies;
import org.monarchinitiative.svart.assembly.GenomicAssembly;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class DbsnpVcfParserTest {

    private static final double ERROR = 5e-6;

    private static final Path testFile = Paths.get(HgSvc2VcfParserTest.class.getResource("00-common_all.100lines.vcf.gz").getPath());
    private static final GenomicAssembly assembly = GenomicAssemblies.GRCh38p13();

    @Test
    public void parseToList() throws Exception {
        AbstractVcfIngestRecordParser instance = new DbsnpVcfParser(assembly, testFile);
        List<? extends PopulationVariant> variants = instance.parseToList();

        assertThat(variants, hasSize(6));

        Map<String, ? extends List<? extends PopulationVariant>> variantsById = variants.stream().collect(Collectors.groupingBy(PopulationVariant::id));
        PopulationVariant del = variantsById.get("rs376342519").get(0);
        assertThat(del.contigName(), equalTo("1"));
        assertThat(del.strand(), equalTo(Strand.POSITIVE));
        assertThat(del.coordinateSystem(), equalTo(CoordinateSystem.oneBased()));
        assertThat(del.start(), equalTo(10617));
        assertThat(del.end(), equalTo(10637));
        assertThat(del.variantType(), equalTo(VariantType.DEL));
        assertThat((double) del.alleleFrequency(), closeTo(99.3, ERROR));


        List<? extends PopulationVariant> insertions = variantsById.get("rs557514207");
        assertThat(insertions, hasSize(2));

        PopulationVariant minor = insertions.get(0);
        assertThat(minor.contigName(), equalTo("1"));
        assertThat(minor.strand(), equalTo(Strand.POSITIVE));
        assertThat(minor.coordinateSystem(), equalTo(CoordinateSystem.oneBased()));
        assertThat(minor.start(), equalTo(15904));
        assertThat(minor.end(), equalTo(15903));
        assertThat(((double) minor.alleleFrequency()), closeTo(0.01, ERROR));

        PopulationVariant major = insertions.get(1);
        assertThat(major.contigName(), equalTo("1"));
        assertThat(major.strand(), equalTo(Strand.POSITIVE));
        assertThat(major.coordinateSystem(), equalTo(CoordinateSystem.oneBased()));
        assertThat(major.start(), equalTo(15904));
        assertThat(major.end(), equalTo(15903));
        assertThat(((double) major.alleleFrequency()), closeTo(44.11000061035156, ERROR));
    }

}