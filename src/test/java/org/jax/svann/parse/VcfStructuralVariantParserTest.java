package org.jax.svann.parse;

import org.jax.svann.reference.genome.GenomeAssembly;
import org.jax.svann.reference.genome.GenomeAssemblyProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.Matchers.*;

public class VcfStructuralVariantParserTest {

    static Path EXAMPLE_VCF = Paths.get("src/test/resources/sv_example.vcf");

    static GenomeAssembly ASSEMBLY = GenomeAssemblyProvider.getGrch38Assembly();

    private VcfStructuralVariantParser parser;

    @BeforeEach
    public void setUp() {
        parser = new VcfStructuralVariantParser(ASSEMBLY);
    }

    @Test
    public void parse() throws Exception {
        final ParseResult result = parser.parseFile(EXAMPLE_VCF);

        assertThat(result.getAnns(), hasSize(3));
        assertThat(result.getBreakends(), hasSize(6));

    }
}