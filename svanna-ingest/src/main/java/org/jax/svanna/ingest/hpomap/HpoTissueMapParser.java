package org.jax.svanna.ingest.hpomap;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.jax.svanna.core.SvAnnaRuntimeException;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.io.*;
import java.util.List;
import java.util.Map;

/**
 * For some kinds of analysis, we want to associate HPO terms with terms from other
 * ontologies such as UBERON and CL. Currently, we are interested in mapping
 * enhancers from FANTOM5 to corresponding HPO terms. The map for this is
 * contained in main/resources/hpo_enhancer_map.csv
 * There are four columns and no header
 * The columns are
 * other.ontology.id,other.ontology.label,hpo.id,hpo.label
 * Column rows start with #
 * This class implements a parser that can be used with other maps and as a convenience,
 * it implements a version that parsers the above-mentioned enhancer map file.
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
    ImmutableList.Builder<HpoMapping> builder = new ImmutableList.Builder<>();
    BufferedReader br = new BufferedReader(new InputStreamReader(is));
    String line;
    while ((line = br.readLine()) != null) {
      if (line.startsWith("#") || line.isEmpty())
        continue;

      String[] fields = line.split(",");
      if (fields.length != 4) {
        throw new SvAnnaRuntimeException("Malformed mapping file line: " + line);
      }
      TermId tid = TermId.of(fields[0]);
      String label = fields[1];
      TermId hpoId = TermId.of(fields[2]);
      String hpoLabel = fields[3];
      HpoMapping hmapping = new HpoMapping(tid, label, hpoId, hpoLabel);
      builder.add(hmapping);
    }

    return builder.build();
  }

  public Map<TermId, HpoMapping> getOtherToHpoMap() {
    ImmutableMap.Builder<TermId, HpoMapping> builder = new ImmutableMap.Builder<>();
    for (HpoMapping hmap : this.hpoMappingList) {
      builder.put(hmap.getOtherOntologyTermId(), hmap);
    }
    return builder.build();
  }

  public List<HpoMapping> getHpoMappingList() {
    return hpoMappingList;
  }


}
