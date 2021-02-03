package org.jax.svanna.enhancer.fantom;

import org.jax.svanna.core.exception.SvAnnRuntimeException;
import org.jax.svanna.hpomap.HpoMapping;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
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
    /** Path to Human.sample_name2library_id.txt file. */
    private final String fantomHumanSamplesPath;
    /** Key, a label such as lung adenocarcinoma cell line:PC-14. Value, the sample id., e.g., CNhs10726. */
     private final Map<String, FantomSample> idToFantomSampleMap;
    /**
     * parse the FANTOM sample name to library id file.
     * @param path Path to Human.sample_name2library_id.txt file.
     */
    public FantomSampleParser(String path, Map<TermId, HpoMapping> hpoMap) {
        SupplementParser supplementParser = new SupplementParser(hpoMap);
        Map<String, HpoMapping> fantomSampleToHpoMap = supplementParser.getFantomSampleToHpoMappingMap();
        this.fantomHumanSamplesPath = path;
        this.idToFantomSampleMap = new HashMap<>();
        int found = 0;
        int notfound = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(this.fantomHumanSamplesPath))) {
            String line;
            // note this file has no header
            while((line = br.readLine()) != null) {
                String[] fields = line.split("\t");
                if (fields.length != 2) {
                    throw new SvAnnRuntimeException("Malformed FANTOM Sample line with "
                            + fields.length + " fields: " + line);
                }
                //String label = fields[0]; - not needed
                String sampleId = fields[1];
                if (fantomSampleToHpoMap.containsKey(sampleId)) {
                    HpoMapping hmapping = fantomSampleToHpoMap.get(sampleId);
                    FantomSample fsample = new FantomSample(hmapping.getOtherOntologyTermId(),
                            hmapping.getOtherOntologyLabel(),
                            hmapping.getHpoTermId(),
                            hmapping.getHpoLabel(),
                            sampleId);
                    this.idToFantomSampleMap.put(sampleId, fsample);
                    found++;
                } else {
                    notfound++;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.err.printf("[INFO] We identified %d FANTOM samples and could not find an ontology term for %d.\n ", found, notfound);
        System.out.printf("[INFO] We got %d FANTOM samples.\n", this.idToFantomSampleMap.size());
    }

    public Map<String, FantomSample> getIdToFantomSampleMap() {
        return idToFantomSampleMap;
    }
}
