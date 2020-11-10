package org.jax.svann;

import de.charite.compbio.jannovar.data.JannovarData;
import de.charite.compbio.jannovar.data.JannovarDataSerializer;
import de.charite.compbio.jannovar.data.SerializationException;
import org.jax.svann.hpo.GeneWithId;
import org.jax.svann.reference.genome.GenomeAssembly;
import org.jax.svann.reference.genome.GenomeAssemblyProvider;
import org.jax.svann.reference.transcripts.JannovarTranscriptService;
import org.jax.svann.reference.transcripts.TranscriptService;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class TestBase {

    /**
     * Full GRCh38.p13 assembly.
     */
    protected static final GenomeAssembly GENOME_ASSEMBLY = GenomeAssemblyProvider.getGrch38Assembly();

    private static final Path JANNOVAR_PATH = Paths.get("src/test/resources/hg38_refseq_small.ser");
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
    protected static final JannovarData JANNOVAR_DATA = getJannovarData();

    protected static final Map<String, GeneWithId> GENE_WITH_ID_MAP = getGeneMap();

    protected static final TranscriptService TX_SERVICE = JannovarTranscriptService.of(GENOME_ASSEMBLY, JANNOVAR_DATA);

    private static JannovarData getJannovarData() {
        try {
            return new JannovarDataSerializer(JANNOVAR_PATH.toString()).load();
        } catch (SerializationException e) {
            // should not happen since the file is present
            throw new RuntimeException(e);
        }
    }

    private static Map<String, GeneWithId>  getGeneMap() {
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
