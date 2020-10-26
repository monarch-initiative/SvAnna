package org.jax.svann.overlap;

import de.charite.compbio.jannovar.data.JannovarData;
import de.charite.compbio.jannovar.data.JannovarDataSerializer;
import de.charite.compbio.jannovar.data.SerializationException;
import org.jax.svann.reference.genome.GenomeAssembly;
import org.jax.svann.reference.genome.GenomeAssemblyProvider;

import java.nio.file.Path;
import java.nio.file.Paths;

public abstract class TestBase {

    /**
     * Full GRCh38.p13 assembly.
     */
    protected static final GenomeAssembly GENOME_ASSEMBLY = GenomeAssemblyProvider.getGrch38Assembly();

    private static final Path JANNOVAR_PATH = Paths.get("src/test/resources/hg38_ucsc_small.ser");
    /**
     * Small Jannovar cache that contains UCSC transcripts of the following genes:
     * <ul>
     *     <li><em>SURF1</em></li>
     *     <li><em>SURF2</em></li>
     *     <li><em>FBN1</em></li>
     *     <li><em>ZNF436</em></li>
     *     <li><em>ZBTB38</em></li>
     *     <li><em>HNF4A</em></li>
     *     <li><em>GCK</em></li>
     *     <li><em>COL4A5</em></li>
     *     <li><em>BRCA2</em></li>
     * </ul>
     */
    protected static final JannovarData JANNOVAR_DATA = getJannovarData();

    private static JannovarData getJannovarData() {
        try {
            return new JannovarDataSerializer(JANNOVAR_PATH.toString()).load();
        } catch (SerializationException e) {
            // should not happen since the file is present
            throw new RuntimeException(e);
        }
    }

}
