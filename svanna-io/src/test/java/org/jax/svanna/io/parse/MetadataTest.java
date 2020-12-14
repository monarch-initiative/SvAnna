package org.jax.svanna.io.parse;

import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFCodec;
import htsjdk.variant.vcf.VCFFileReader;
import htsjdk.variant.vcf.VCFHeaderVersion;
import org.jax.svanna.core.reference.VariantMetadata;
import org.jax.svanna.core.reference.Zygosity;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class MetadataTest {

    private static final VCFCodec VCF_CODEC = new VCFCodec();
    private static final boolean REQUIRE_INDEX = false;
    private static final Path SV_EXAMPLE_PATH = Paths.get("src/test/resources/org/jax/svanna/io/parse/sv_example.vcf");

    @BeforeAll
    public static void beforeAll() {
        try (VCFFileReader reader = new VCFFileReader(SV_EXAMPLE_PATH, REQUIRE_INDEX)) {
            VCF_CODEC.setVCFHeader(reader.getFileHeader(), VCFHeaderVersion.VCF4_3);
        }
    }

    @Test
    public void parseGenotypeData() {
        String line = "1\t10\trs123\tG\tA\t29\tPASS\tAF=0.5;DB\tGT:GQ:DP:AD\t0/1:12:11:6,5";
        VariantContext vc = VCF_CODEC.decode(line);

        Metadata metadata = Metadata.parseGenotypeData(0, vc.getGenotypes());
        assertThat(metadata.zygosity(), equalTo(Zygosity.HETEROZYGOUS));
        assertThat(metadata.dp(), equalTo(11));
        assertThat(metadata.refReads(), equalTo(6));
        assertThat(metadata.altReads(), equalTo(5));
    }

    @Test
    public void parseGenotypeData_badSampleIdx() {
        String line = "1\t10\trs123\tG\tA\t29\tPASS\tAF=0.5;DB\tGT:GQ:DP:AD\t0/1:12:11:6,5";
        VariantContext vc = VCF_CODEC.decode(line);

        Metadata metadata = Metadata.parseGenotypeData(1, vc.getGenotypes());
        assertThat(metadata, equalTo(Metadata.missing()));
    }

    @Test
    public void parseGenotypeData_missingDepth() {
        String line = "1\t10\trs123\tG\tA\t29\tPASS\tAF=0.5;DB\tGT:GQ:AD\t0/1:12:6,5";
        VariantContext vc = VCF_CODEC.decode(line);

        Metadata metadata = Metadata.parseGenotypeData(0, vc.getGenotypes());
        assertThat(metadata.zygosity(), equalTo(Zygosity.HETEROZYGOUS));
        assertThat(metadata.dp(), equalTo(VariantMetadata.MISSING_DEPTH_PLACEHOLDER));
        assertThat(metadata.refReads(), equalTo(6));
        assertThat(metadata.altReads(), equalTo(5));
    }

    @Test
    public void parseGenotypeData_breakend() {
        String line = "2\t321681\tbnd_W\tG\tG]17:198982]\t6\tPASS\tSVTYPE=BND;MATEID=bnd_Y;EVENT=tra1\tGT:DP:AD\t1/1:13:1,12";
        VariantContext vc = VCF_CODEC.decode(line);

        Metadata metadata = Metadata.parseGenotypeData(0, vc.getGenotypes());
        assertThat(metadata.zygosity(), equalTo(Zygosity.HOMOZYGOUS));
        assertThat(metadata.dp(), equalTo(13));
        assertThat(metadata.refReads(), equalTo(1));
        assertThat(metadata.altReads(), equalTo(12));
    }
}