package org.jax.svanna.ingest.parse.enhancer;

import org.jax.svanna.ingest.hpomap.HpoMapping;
import org.jax.svanna.ingest.hpomap.HpoTissueMapParser;
import org.jax.svanna.ingest.parse.enhancer.fantom.FantomEnhancerParser;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.svart.GenomicAssemblies;
import org.monarchinitiative.svart.GenomicAssembly;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public abstract class AbstractEnhancerParserTest {

    private static final Path ENHANCER_MAP_PATH = Paths.get(FantomEnhancerParser.class.getResource("/hpo_enhancer_map.csv").getPath());
    protected static final GenomicAssembly GRCh38p13 = GenomicAssemblies.GRCh38p13();

    protected static Map<TermId, HpoMapping> UBERON_TO_HPO;
    static {
        HpoTissueMapParser hpoTissueMapParser;
        try {
            hpoTissueMapParser = new HpoTissueMapParser(ENHANCER_MAP_PATH.toFile());
            UBERON_TO_HPO = hpoTissueMapParser.getOtherToHpoMap();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
