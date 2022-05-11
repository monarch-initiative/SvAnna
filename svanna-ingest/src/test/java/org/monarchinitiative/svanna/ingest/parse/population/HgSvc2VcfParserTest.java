package org.monarchinitiative.svanna.ingest.parse.population;

import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFCodec;
import htsjdk.variant.vcf.VCFFileReader;
import htsjdk.variant.vcf.VCFHeader;
import htsjdk.variant.vcf.VCFHeaderVersion;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.monarchinitiative.svanna.model.landscape.variant.PopulationVariant;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.svart.CoordinateSystem;
import org.monarchinitiative.svart.VariantType;
import org.monarchinitiative.svart.assembly.GenomicAssemblies;
import org.monarchinitiative.svart.assembly.GenomicAssembly;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class HgSvc2VcfParserTest {

    private static final Path TEST_FILE = Paths.get("src/test/resources/population/freeze3.sv.alt.250lines.vcf.gz");
    private static final GenomicAssembly genomicAssembly = GenomicAssemblies.GRCh38p13();
    private static final VCFCodec CODEC = new VCFCodec();

    private HgSvc2VcfParser instance;

    @BeforeAll
    public static void beforeAll() {
        try (VCFFileReader reader = new VCFFileReader(TEST_FILE, false)) {
            VCFHeader header = reader.getHeader();
            CODEC.setVCFHeader(header, VCFHeaderVersion.VCF4_2);
        }
    }

    @BeforeEach
    public void setUp() {
        instance = new HgSvc2VcfParser(genomicAssembly, TEST_FILE);
    }

    @Test
    public void parseToList() throws Exception {
        instance = new HgSvc2VcfParser(genomicAssembly, TEST_FILE);
        List<? extends PopulationVariant> variants = instance.parseToList();

        assertThat(variants, hasSize(43));
    }

    @Nested
    public class IndividualVcfLinesTest {

        @Test
        public void checkDeletion() {
            VariantContext vc = CODEC.decode("chr17\t1741319\t.\tGACAGTGAAGGTGGGAAGGGGCCTGATGCAGGGAGTGAGGCGCAAGACAGTGAGGGTGGGAAGGAGCCTGATGCAGGGAGTGAGGCGCAGGACAGTGAGGGTGGGAAGGAGCCTGATGCAGGGAGTGAGGAGCAGGACAGTGAAGGTGGGAAGGAGCCTGATGCAGGGAGTGAGGAGCCGGACAGTGAGGGTGGGAAGGAGCCTGATGCAGGGAGTGAGGAGCTGGACAGTGAGGATGGGAAGGAGCCTGATGCAGGGAGTGAGGAGCAGGACAGTGAGGGTGGGAAGTGGCCTGATGCAGGGAGTGAGGCGCAGGACAGTGAGGGTGGGAAGGAGCCTGATGCAGGGAGTGAGGAGCAGGACAGTGAGGGTGGGAGGCGGCCTGATGCAGGGAGTGAGGAGCCGGACAGTGAGGGTGGGAGGCGGCCTGATGCAGGGAGTGAGGAGCAGGACAGTGAGGGTGGGAAGTGGCCCGATGCAGGGAGTGAGGCGCAGGACAGTGAGGGTGGGAAGGAGCCTGATGCAGGGAGTGAGGAGCCGGACAGTGAGGGTGGGAAGGAGCCTGATGCAGGGAGTGAGGAGCCGGACAGTGAGGGTGGGAAGGAGCCTGATGCAGGGAGTGAGGAGCAGGTGTGAGGGCGGGAAGCGGCCTGATGCAGGGAGTGAGGAGCAGGAC\tG\t.\t.\tVARTYPE=SV;SVTYPE=DEL;SVLEN=-675;ID=chr17-1741320-DEL-675;LEAD_SAMPLE=HG00731;TIG_REGION=cluster16_000011F:348093-348093;TIG_STRAND=+\tGT\t0|1\t0|0\t0|0\t0|0\t0|0\t0|0\t0|0\t0|0\t0|0\t0|0\t0|0\t0|0\t0|0\t0|0\t0|0\t0|0\t0|0\t0|0\t1|1\t0|0\t0|0\t0|1\t0|0\t0|0\t0|0\t0|0\t0|0\t0|1\t0|0\t0|1\t0|0\t0|0");
            Optional<? extends PopulationVariant> pvo = instance.toPopulationVariant().apply(vc);

            assertThat(pvo.isPresent(), equalTo(true));
            PopulationVariant pv = pvo.get();
            assertThat(pv.id(), equalTo("chr17-1741320-DEL-675"));
            assertThat(pv.contigName(), equalTo("17"));
            assertThat(pv.startWithCoordinateSystem(CoordinateSystem.oneBased()), equalTo(1_741_320));
            assertThat(pv.endWithCoordinateSystem(CoordinateSystem.oneBased()), equalTo(1_741_994));
            assertThat(pv.coordinates().length(), equalTo(675));
            assertThat(pv.variantType(), equalTo(VariantType.DEL));
            assertThat((double) pv.alleleFrequency(), closeTo(6*100/64., 1E-5));
        }

        @Test
        public void checkDeletionBla() {
            VariantContext vc = CODEC.decode("chr17\t1740554\t.\tGACAGTGAGGGTGGGAAGGAGCCTGATGCAGGGAGTGAGGCGCAGGACAGTGAGGGTGGGAGGCGGCCTGATGCAGGGAGTGAGGCGCAGGACAGTGAGGGTGGGAAGGAGCCTGATGCAGGGAGTGAGGAGCAGGACAGTCAGGGTGGGAGGCGGCCTGATGCAGGGAGTGAGGCGCAGGACAGTGAGGGTGGGAAGGAGCCTGATGCAGGGAGTGAGGCGCAGGACAGTGAGGGTGGGAAGGAGCCTGATGCAGGGAGTGAGGCGCAGGACAGTGAGGGTGGGAAGTGGCCTGATGCAGGGAGTGAGGCGCAGGACAGTGAGGGTGGGAAGTGGCCTGATGCAGGGAGTGAGGCGCAGGACAGTGAGGGTGGGAAGGGGCCTGATGCAGGGAGTGAGGCGCAGGACAGTGAGGGTGGGAAGGGGCCTGATGCAGGGAGTGAGGAGCCGGACAGTGAGGGTGGGAAGGAGCCTGATGCAGGGAGTGAGGCGCAGGACAGTGAGGGTGGGAAGGGGCCTGATGCAGGGAGTGAGGCGCAGGACAGTGAGGGTGGGAAGGGGCCTGATGCAGGGAGTGAGGAGCCGGACACTGAGGGTGGGAAGGAGCCTGATGCAGGGAGTGAGGCGCAGGACAGTGAGGGTGGGAAGGGGCCTGATGCAGGGAGTGAGGCGCAGGACAGTGAGGGTGGGAAGGGGCCTGATGCAGGGAGTGAGGAGCCGGACAGTGAGGGTGGGAAGGAGCCTGATGCAGGGAGTGAGGCGCAGGACAGTGAAGGTGGGAAGGGGCCTGATGCAGGGAGTGAGGCGCAAGACAGTGAGGGTGGGAAGGAGCCTGATGCAGGGAGTGAGGCGCAGGACAGTGAGGGTGGGAAGGAGCCTGATGCAGGGAGTGAGGAGCAGGACAGTGAAGGTGGGAAGGAGCCTGATGCAGGGAGTGAGGAGCCGGACAGTGAGGGTGGGAAGGAGCCTGATGCAGGGAGTGAGGAGCTGGACAGTGAGGATGGGAAGGAGCCTGATGCAGGGAGTGAGGAGCAGGACAGTGAGGGTGGGAAGTGGCCTGATGCAGGGAGTGAGGCGCAGGACAGTGAGGGTGGGAAGGAGCCTGATGCAGGGAGTGAGGAGCAGGACAGTGAGGGTGGGAGGCGGCCTGATGCAGGGAGTGAGGAGCCGGACAGTGAGGGTGGGAGGCGGCCTGATGCAGGGAGTGAGGAGCAGGACAGTGAGGGTGGGAAGTGGCCCGATGCAGGGAGTGAGGCGCAGGACAGTGAGGGTGGGAAGGAGCCTGATGCAGGGAGTGAGGAGCCGGACAGTGAGGGTGGGAAGGAGCCTGATGCAGGGAGTGAGGAGCCGGACAGTGAGGGTGGGAAGGAGCCTGATGCAGGGAGTGAGGAGCAGGTGTGAGGGCGGGAAGCGGCCTGATGCAGGGAGTGAGGAGCAGGAC\tG\t.\t.\tVARTYPE=SV;SVTYPE=DEL;SVLEN=-1440;ID=chr17-1740555-DEL-1440;LEAD_SAMPLE=NA19238;TIG_REGION=cluster22_000010F:851590-851590;TIG_STRAND=+\tGT\t0|0\t0|0\t0|0\t0|0\t1|0\t0|1\t0|0\t1|0\t0|0\t0|0\t0|1\t0|0\t0|0\t0|0\t0|0\t1|0\t0|0\t0|0\t0|0\t0|0\t0|0\t0|0\t0|0\t0|0\t0|0\t0|0\t0|0\t0|0\t0|0\t0|0\t0|0\t0|0");
            Optional<? extends PopulationVariant> pvo = instance.toPopulationVariant().apply(vc);

            assertThat(pvo.isPresent(), equalTo(true));
            PopulationVariant pv = pvo.get();
            assertThat(pv.id(), equalTo("chr17-1740555-DEL-1440"));
            assertThat(pv.contigName(), equalTo("17"));
            assertThat(pv.startWithCoordinateSystem(CoordinateSystem.oneBased()), equalTo(1_740_555));
            assertThat(pv.endWithCoordinateSystem(CoordinateSystem.oneBased()), equalTo(1_741_994));
            assertThat(pv.coordinates().length(), equalTo(1440));
            assertThat(pv.variantType(), equalTo(VariantType.DEL));
            assertThat((double) pv.alleleFrequency(), closeTo(5*100/64., 1E-5));
        }

        @Test
        public void checkInsertion() {
            VariantContext vc = CODEC.decode("chr1\t10627\t.\tA\tAAGGCGCGCCGCGCCGGCGCAGGCGCAGAGAGGCGCGCCGCGCCGGCGCAGGCGCAGAG\t.\t.\tVARTYPE=SV;SVTYPE=INS;SVLEN=58;ID=chr1-10628-INS-58;LEAD_SAMPLE=HG03371;TIG_REGION=cluster10_contig_296:70644-70701;TIG_STRAND=-\tGT\t.|.\t0|.\t.|.\t.|.\t0|.\t.|.\t.|.\t.|0\t.|0\t0|.\t0|.\t0|.\t0|0\t.|.\t1|1\t.|.\t0|0\t.|.\t.|.\t.|.\t0|.\t0|.\t.|.\t0|0\t0|.\t0|.\t.|.\t.|.\t0|0\t.|.\t.|.\t.|.");
            Optional<? extends PopulationVariant> pvo = instance.toPopulationVariant().apply(vc);

            assertThat(pvo.isPresent(), equalTo(true));
            PopulationVariant pv = pvo.get();
            assertThat(pv.id(), equalTo("chr1-10628-INS-58"));
            assertThat(pv.contigName(), equalTo("1"));
            assertThat(pv.startWithCoordinateSystem(CoordinateSystem.zeroBased()), equalTo(10_627));
            assertThat(pv.endWithCoordinateSystem(CoordinateSystem.zeroBased()), equalTo(10_627));
            assertThat(pv.coordinates().length(), equalTo(0));
            assertThat(pv.variantType(), equalTo(VariantType.INS));
            assertThat((double) pv.alleleFrequency(), closeTo(2*100/21., 1E-5));
        }
    }

}