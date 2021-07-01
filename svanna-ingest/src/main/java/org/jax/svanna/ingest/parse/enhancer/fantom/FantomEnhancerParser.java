package org.jax.svanna.ingest.parse.enhancer.fantom;

import org.jax.svanna.ingest.hpomap.HpoMapping;
import org.jax.svanna.ingest.parse.IngestRecordParser;
import org.jax.svanna.ingest.parse.enhancer.AnnotatedTissue;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.svart.GenomicAssembly;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Stream;

public class FantomEnhancerParser implements IngestRecordParser<FEnhancer> {

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
    public Stream<? extends FEnhancer> parse() throws IOException {
        FantomSampleParser sampleParser = new FantomSampleParser(samplesPath, uberonToHpoMap);
        Map<String, AnnotatedTissue> annotatedTissueMap = sampleParser.getIdToFantomSampleMap();
        FantomCountMatrixParser fantomCountMatrixParser = new FantomCountMatrixParser(assembly, countsPath, annotatedTissueMap);
        return fantomCountMatrixParser.getEnhancers().stream(); // not the most efficient way but OK for now
    }

}
