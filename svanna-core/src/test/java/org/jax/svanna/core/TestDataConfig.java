package org.jax.svanna.core;

import org.monarchinitiative.variant.api.GenomicAssembly;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class TestDataConfig {

    private static final Path GENOME_ASSEMBLY_REPORT_PATH = Paths.get("src/test/resources/GCA_000001405.28_GRCh38.p13_assembly_report.txt");

    private static final Path JANNOVAR_DATA = Paths.get("src/test/resources/hg38_refseq_small.ser");

    @Bean
    public GenomicAssembly genomicAssembly() throws Exception {
        return GenomicAssemblyProvider.fromAssemblyReport(GENOME_ASSEMBLY_REPORT_PATH);
    }

//    /**
//     * Small Jannovar cache that contains RefSeq transcripts of the following genes:
//     * <ul>
//     *     <li><em>SURF1</em></li>
//     *     <li><em>SURF2</em></li>
//     *     <li><em>FBN1</em></li>
//     *     <li><em>ZNF436</em></li>
//     *     <li><em>ZBTB48</em></li>
//     *     <li><em>HNF4A</em></li>
//     *     <li><em>GCK</em></li>
//     *     <li><em>BRCA2</em></li>
//     *     <li><em>COL4A5</em></li> (on <code>chrX</code>)
//     *     <li><em>SRY</em></li> (on <code>chrY</code>)
//     * </ul>
//     */
//    @Bean
//    public JannovarData jannovarData() throws Exception {
//        return new JannovarDataSerializer(JANNOVAR_DATA.toString()).load();
//    }
}
