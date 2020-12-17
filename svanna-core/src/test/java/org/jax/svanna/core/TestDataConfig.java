package org.jax.svanna.core;

import de.charite.compbio.jannovar.data.JannovarData;
import de.charite.compbio.jannovar.data.JannovarDataSerializer;
import org.jax.svanna.core.hpo.GeneWithId;
import org.jax.svanna.core.overlap.Overlapper;
import org.jax.svanna.core.overlap.SvAnnOverlapper;
import org.jax.svanna.core.reference.GenomicAssemblyProvider;
import org.jax.svanna.core.reference.TranscriptService;
import org.jax.svanna.core.reference.transcripts.JannovarTranscriptService;
import org.jax.svanna.test.TestVariants;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.variant.api.GenomicAssembly;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Configuration
public class TestDataConfig {

    private static final Path GENOME_ASSEMBLY_REPORT_PATH = Paths.get("src/test/resources/GCA_000001405.28_GRCh38.p13_assembly_report.txt");

    private static final Path JANNOVAR_DATA = Paths.get("src/test/resources/hg38_refseq_small.ser");

    @Bean
    public GenomicAssembly genomicAssembly() throws Exception {
        return GenomicAssemblyProvider.fromAssemblyReport(GENOME_ASSEMBLY_REPORT_PATH);
    }

    /**
     * Small Jannovar cache that contains RefSeq transcripts of the following genes:
     * <ul>
     *     <li><em>SURF1</em></li>
     *     <li><em>SURF2</em></li>
     *     <li><em>FBN1</em></li>
     *     <li><em>ZNF436</em></li>
     *     <li><em>ZBTB48</em></li>
     *     <li><em>HNF4A</em></li>
     *     <li><em>GCK</em></li>
     *     <li><em>BRCA2</em></li>
     *     <li><em>COL4A5</em></li> (on <code>chrX</code>)
     *     <li><em>SRY</em></li> (on <code>chrY</code>)
     * </ul>
     */
    @Bean
    public JannovarData jannovarData() throws Exception {
        return new JannovarDataSerializer(JANNOVAR_DATA.toString()).load();
    }

    @Bean
    public TestVariants testVariants(GenomicAssembly genomicAssembly) {
        return new TestVariants(genomicAssembly);
    }

    @Bean
    public TranscriptService transcriptService(GenomicAssembly assembly, JannovarData jannovarData) {
        return JannovarTranscriptService.of(assembly, jannovarData);
    }

    @Bean
    public Overlapper overlapper(TranscriptService transcriptService) {
        return new SvAnnOverlapper(transcriptService.getChromosomeMap());
    }

    @Bean
    public Map<String, GeneWithId> geneWithIdMap() {
        GeneWithId surf1 = new GeneWithId("SURF1", TermId.of("NCBIGene:6834"));
        GeneWithId surf2 = new GeneWithId("SURF2", TermId.of("NCBIGene:6835"));
        GeneWithId fbn1 = new GeneWithId("FBN1", TermId.of("NCBIGene:2200"));
        GeneWithId znf436 = new GeneWithId("ZNF436", TermId.of("NCBIGene:80818"));
        GeneWithId zbtb48 = new GeneWithId("ZBTB48", TermId.of("NCBIGene:3104"));
        GeneWithId hnf4a = new GeneWithId("HNF4A", TermId.of("NCBIGene:3172"));
        GeneWithId gck = new GeneWithId("GCK", TermId.of("NCBIGene:2645"));
        GeneWithId brca2 = new GeneWithId("BRCA2", TermId.of("NCBIGene:675"));
        GeneWithId col4a5 = new GeneWithId("COL4A5", TermId.of("NCBIGene:1287"));
        GeneWithId sry = new GeneWithId("SRY", TermId.of("NCBIGene:6736"));

        return Stream.of(
                new AbstractMap.SimpleImmutableEntry<>("SURF1", surf1),
                new AbstractMap.SimpleImmutableEntry<>("SURF2", surf2),
                new AbstractMap.SimpleImmutableEntry<>("FBN1", fbn1),
                new AbstractMap.SimpleImmutableEntry<>("ZNF436", znf436),
                new AbstractMap.SimpleImmutableEntry<>("ZBTB48", zbtb48),
                new AbstractMap.SimpleImmutableEntry<>("HNF4A", hnf4a),
                new AbstractMap.SimpleImmutableEntry<>("GCK", gck),
                new AbstractMap.SimpleImmutableEntry<>("BRCA2", brca2),
                new AbstractMap.SimpleImmutableEntry<>("COL4A5", col4a5),
                new AbstractMap.SimpleImmutableEntry<>("SRY", sry)
        )
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
