package org.jax.svanna.ingest.enhancer.fantom;

import org.jax.svanna.ingest.enhancer.AnnotatedTissue;
import org.jax.svanna.ingest.enhancer.EnhancerParser;
import org.jax.svanna.ingest.hpomap.HpoMapping;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.svart.GenomicAssembly;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class FantomEnhancerParser implements EnhancerParser<FEnhancer> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FantomEnhancerParser.class);

    private final GenomicAssembly assembly;
    private final Path countsPath;
    private final Path samplesPath;
    private final Map<TermId, HpoMapping> uberonToHpoMap;

    public FantomEnhancerParser(GenomicAssembly assembly, Path countsPath, Path samplesPath, Map<TermId, HpoMapping> uberonToHpoMap) {
        this.assembly = assembly;
        this.countsPath = countsPath;
        this.samplesPath = samplesPath;
        this.uberonToHpoMap = uberonToHpoMap;
    }

    @Override
    public List<FEnhancer> parse() {
        FantomSampleParser sampleParser = new FantomSampleParser(samplesPath, uberonToHpoMap);
        Map<String, AnnotatedTissue> annotatedTissueMap = sampleParser.getIdToFantomSampleMap();
        FantomCountMatrixParser fantomCountMatrixParser = new FantomCountMatrixParser(assembly, countsPath, annotatedTissueMap);
        return fantomCountMatrixParser.getEnhancers();
    }

}
