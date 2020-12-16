package org.jax.svanna.io.filter.dgv;

import org.jax.svanna.core.filter.SVFeatureOrigin;
import org.jax.svanna.io.TestDataConfig;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.variant.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(classes = TestDataConfig.class)
public class DgvFeatureTest {

    @Autowired
    public GenomicAssembly genomicAssembly;



    @Test
    public void properties() {
        // nsv3324042      1       60901   71500   CNV     duplication     Audano_et_al_2019       30661756        Sequencing                      nssv14478394,nssv14489204,nssv14484668  M               14      3
        //       0               ""      HG00268,HG04217,NA19434
        Contig chr1 = genomicAssembly.contigByName("1");
        DgvFeature feature = DgvFeature.of(chr1, Strand.POSITIVE, CoordinateSystem.ONE_BASED, 60_901, 71_500, "nsv3324042", VariantType.DUP, 3.f / 14);
        assertThat(feature.contigName(), equalTo("1"));
        assertThat(feature.start(), equalTo(60_901));
        assertThat(feature.end(), equalTo(71_500));
        assertThat(feature.coordinateSystem(), equalTo(CoordinateSystem.ONE_BASED));
        assertThat(feature.strand(), equalTo(Strand.POSITIVE));
        assertThat(feature.variantType(), equalTo(VariantType.DUP));
        assertThat(feature.getOrigin(), equalTo(SVFeatureOrigin.DGV));
        assertThat(feature.accession(), equalTo("nsv3324042"));
        assertThat(feature.frequency(), equalTo(0.21428572F));
    }

    @Test
    public void failsWhenFrequencyOutOfBounds() {
        Contig chr1 = genomicAssembly.contigByName("1");
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> DgvFeature.of(chr1, Strand.POSITIVE, CoordinateSystem.ONE_BASED, 1, 2, "nsv3324042", VariantType.DUP, 1.01f));
        assertThat(e.getMessage(), equalTo("Frequency must be in range [0,1]: 1.01"));
    }
}