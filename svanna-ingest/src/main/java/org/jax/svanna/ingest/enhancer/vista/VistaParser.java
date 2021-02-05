package org.jax.svanna.ingest.enhancer.vista;

import org.jax.svanna.core.exception.SvAnnRuntimeException;
import org.jax.svanna.ingest.enhancer.AnnotatedTissue;
import org.jax.svanna.ingest.enhancer.IngestedEnhancer;
import org.jax.svanna.ingest.hpomap.HpoMapping;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Parse the file {@code vista-hg38.bed}, which is created in the {@code scripts} subdirectory.
 * This file has the following format. Tissues can be an UBERON or Cell Ontology term
 *
 * name	chr	begin	end	tissues
 * element 1	chr16	86396481	86397120	neural tube[UBERON:0001049];presumptive hindbrain[UBERON:0007277];...
 * element 4	chr16	80338696	80339858	neural tube[UBERON:0001049];presumptive hindbrain[UBERON:0007277];...
 * element 12	chr16	78476711	78478047	presumptive hindbrain[UBERON:0007277];forebrain[UBERON:0001890]
 */
public class VistaParser {

    public final List<IngestedEnhancer> ingestedEnhancerList;

    public VistaParser(String vistaPath, Map<TermId, HpoMapping> uberonToHpoMap) {
        ingestedEnhancerList = new ArrayList<>();
        File f = new File(vistaPath);
        if (! f.isFile()) {
            throw new RuntimeException("Could not find VISTA file at " + vistaPath);
        }
        try (BufferedReader br = new BufferedReader(new FileReader(vistaPath))) {
            String line = br.readLine();// header
            if (! line.startsWith("name")) {
                throw new SvAnnRuntimeException("Malformed VISTA header: " + line);
            }
            while ((line=br.readLine()) != null) {
                String [] fields = line.split("\t");
                String name = fields[0];
                String chrom = fields[1];
                int begin = Integer.parseInt(fields[2]);
                int end = Integer.parseInt(fields[3]);
                // fields[4] is like neural tube[UBERON:0001049];presumptive hindbrain[UBERON:0007277];...
                String []tissueList =  fields[4].split(";");
                List<AnnotatedTissue> annotatedTissues = new ArrayList<>();
                for (String tissue : tissueList) {
                    int j = tissue.indexOf("[");
                    if (j<0) {
                        throw new SvAnnRuntimeException("Malformed tissue field:" + tissue);
                    }
                    String label = tissue.substring(0,j);
                    int k = tissue.indexOf("]");
                    if (k<0) {
                        throw new SvAnnRuntimeException("Malformed tissue field:" + tissue);
                    }
                    TermId uberon = TermId.of(tissue.substring(j+1,k));
                    if (! uberonToHpoMap.containsKey(uberon)) {
                        throw new SvAnnRuntimeException(uberon.getValue() + " not found");
                    }
                    HpoMapping hpoMapping = uberonToHpoMap.get(uberon);
                    // The following object just holds the UBERON/Cell Ontology term and the corresponding HPO term
                    AnnotatedTissue atissue = new AnnotatedTissue(uberon, label, hpoMapping.getHpoTermId(), hpoMapping.getHpoLabel());
                    annotatedTissues.add(atissue);
                }
               ingestedEnhancerList.add(new VistaEnhancer(name, chrom, begin, end, annotatedTissues));
            }
        }catch (IOException e) {
            e.printStackTrace();
        }
        System.out.printf("[INFO] We ingested %d VISTA enhancers.\n", ingestedEnhancerList.size());
    }

    public List<IngestedEnhancer> getEnhancers() {
        return ingestedEnhancerList;
    }
}
