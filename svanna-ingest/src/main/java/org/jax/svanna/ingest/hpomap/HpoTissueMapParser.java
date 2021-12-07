package org.jax.svanna.ingest.hpomap;

import org.jax.svanna.core.SvAnnaRuntimeException;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * For some kinds of analysis, we want to associate HPO terms with terms from other
 * ontologies such as UBERON and CL. Currently, we are interested in mapping
 * enhancers from FANTOM5 to corresponding HPO terms. The map for this is
 * contained in main/resources/uberon_tissue_to_hpo_top_level.csv
 * There are four columns and header. The header starts with #
 * The columns are
 * other.ontology.id,other.ontology.label,hpo.id,hpo.label
 * This class implements a parser that can be used with other maps and as a convenience,
 * it implements a version that parsers the above-mentioned enhancer map file.
 *
 * @author Peter N. Robinson
 */
public class HpoTissueMapParser {

    private final List<HpoMapping> hpoMappingList;

    public HpoTissueMapParser(InputStream is) throws IOException {
        hpoMappingList = readMappings(is);
    }

    public HpoTissueMapParser(File path) throws IOException {
        try (InputStream is = new FileInputStream(path)) {
            hpoMappingList = readMappings(is);
        }
    }

    private static List<HpoMapping> readMappings(InputStream is) throws IOException {
        List<HpoMapping> mappings = new ArrayList<>(200);
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String line;
        while ((line = br.readLine()) != null) {
            if (line.startsWith("#") || line.isEmpty())
                continue;

            String[] fields = line.split(",");
            if (fields.length != 4) {
                throw new SvAnnaRuntimeException("Malformed mapping file line: " + line);
            }
            TermId otherTermId = TermId.of(fields[0]);
            String otherLabel = fields[1];
            TermId hpoId = TermId.of(fields[2]);
            String hpoLabel = fields[3];
            HpoMapping hmapping = HpoMapping.of(otherTermId, otherLabel, hpoId, hpoLabel);
            mappings.add(hmapping);
        }

        return List.copyOf(mappings);
    }

    public Map<TermId, HpoMapping> getOtherToHpoMap() {
        return hpoMappingList.stream()
                .collect(Collectors.toUnmodifiableMap(
                        HpoMapping::getOtherOntologyTermId,
                        Function.identity()));
    }

    public List<HpoMapping> getHpoMappingList() {
        return hpoMappingList;
    }


}
