package org.jax.svanna.ingest.parse.enhancer.fantom;

import org.jax.svanna.core.SvAnnaRuntimeException;
import org.jax.svanna.ingest.hpomap.HpoMapping;
import org.jax.svanna.ingest.parse.enhancer.AnnotatedTissue;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;


/**
 * Parse the sample to id file from
 * https://fantom.gsc.riken.jp/5/datafiles/latest/extra/Enhancers/Human.sample_name2library_id.txt
 * A typical line in this file is like this
 * <pre>
 *     substantia nigra, newborn, donor10223	CNhs14076
 * </pre>
 */
public class FantomSampleParser {
    private static final Logger LOGGER = LoggerFactory.getLogger(FantomSampleParser.class);
    /**
     * Path to Human.sample_name2library_id.txt file.
     */
    private final Path fantomHumanSamplesPath;
    /**
     * Key, a label such as lung adenocarcinoma cell line:PC-14. Value, the sample id., e.g., CNhs10726.
     */
    private final Map<String, AnnotatedTissue> idToFantomSampleMap;

    /**
     * parse the FANTOM sample name to library id file.
     * @param path Path to Human.sample_name2library_id.txt file.
     */
    public FantomSampleParser(Path path, Map<TermId, HpoMapping> hpoMap) {
        SupplementParser supplementParser = new SupplementParser(hpoMap);
        Map<String, HpoMapping> fantomSampleToHpoMap = supplementParser.getFantomSampleToHpoMappingMap();
        this.fantomHumanSamplesPath = path;
        this.idToFantomSampleMap = new HashMap<>();
        int found = 0;
        int notfound = 0;
        try (BufferedReader br = Files.newBufferedReader(fantomHumanSamplesPath)) {
            String line;
            // note this file has no header
            while ((line = br.readLine()) != null) {
                String[] fields = line.split("\t");
                if (fields.length != 2) {
                    throw new SvAnnaRuntimeException("Malformed FANTOM Sample line with "
                            + fields.length + " fields: " + line);
                }
                //String label = fields[0]; - not needed
                String sampleId = fields[1];
                if (fantomSampleToHpoMap.containsKey(sampleId)) {
                    HpoMapping hmapping = fantomSampleToHpoMap.get(sampleId);
                    AnnotatedTissue atissue = new AnnotatedTissue(hmapping.getOtherOntologyTermId(), hmapping.getOtherOntologyLabel(),
                            hmapping.getHpoTermId(), hmapping.getHpoLabel());
                    this.idToFantomSampleMap.put(sampleId, atissue);
                    found++;
                } else {
                    notfound++;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        LOGGER.info("We identified {} FANTOM samples and could not find an ontology term for {}", found, notfound);
        LOGGER.info("We got {} FANTOM samples", idToFantomSampleMap.size());
    }

    public Map<String, AnnotatedTissue> getIdToFantomSampleMap() {
        return idToFantomSampleMap;
    }
}
