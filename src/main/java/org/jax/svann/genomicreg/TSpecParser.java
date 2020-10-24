package org.jax.svann.genomicreg;

import org.jax.svann.except.SvAnnRuntimeException;
import org.jax.svann.reference.genome.Contig;
import org.jax.svann.reference.genome.GenomeAssembly;
import org.jax.svann.reference.genome.GenomeAssemblyProvider;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * This class is intended to parse files from the tspec repository
 * https://github.com/pnrobinson/tspec. The file represents FANTOM5 enhancer and has information
 * about tissue specificity
 * <pre>
 * chrom	start	end	tau	HPO.id	HPO.label
 * chr10	100006233	100006603	0.288152	HP:0025015	Abnormal vascular morphology
 * chr10	100008181	100008444	0.328839	HP:0025015	Abnormal vascular morphology
 * chr10	100014348	100014634	0.728857	HP:0000777	Abnormality of the thymus
 * chr10	100020065	100020562	0.498244	HP:0001627	Abnormal heart morphology
 * (...)
 * </pre>
 * tau reflects the degree of tissue specificity, where 0 means unspecific (ubiquitously expressed across tissues/cells)
 * and 1 means specific (exclusively expressed in one tissue/cell).
 */
public class TSpecParser {
    private static final Logger LOGGER = LoggerFactory.getLogger(TSpecParser.class);
    /** key: an HPO id, e.g., HP:0001234; value: corresponding label. TODO may not be needed here, it is more for output*/
    private final Map<TermId, String> id2labelMap;
    /** Key: An HPO id; value: List of {@link Enhancer} objects annotated to the HPO id. */
    private final Map<TermId, List<Enhancer>> id2enhancerMap;
    /**
     * For now, the enhancer files are provided only as hg38. TODO allow as parameter to CTOR
     */
    private final GenomeAssembly assembly = GenomeAssemblyProvider.getGrch38Assembly();

    public TSpecParser(String path) {
        id2labelMap = new HashMap<>();
        id2enhancerMap = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            br.readLine(); // skip header
            while ((line = br.readLine()) != null) {
                String [] fields = line.split("\t");
                if (fields.length != 6) {
                    throw new SvAnnRuntimeException("Bad tspec line: " + line);
                }
                String chr = fields[0];
                final Optional<Contig> contigOptional = assembly.getContigByName(chr);
                if (contigOptional.isEmpty()) {
                    LOGGER.warn("Unknown contig `{}` for tspec enhancer linee {}", chr, line);
                    continue;
                }
                final Contig contig = contigOptional.get();
                int start  = Integer.parseInt(fields[1]);
                int end  = Integer.parseInt(fields[2]);
                double tau = Double.parseDouble(fields[3]);
                TermId tid = TermId.of(fields[4]);
                String label = fields[5];
                id2labelMap.putIfAbsent(tid, label);
                Enhancer enhancer = new Enhancer(contig, start, end, tau, tid);
                id2enhancerMap.putIfAbsent(tid, new ArrayList<>());
                id2enhancerMap.get(tid).add(enhancer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Map<TermId, String> getId2labelMap() {
        return id2labelMap;
    }

    public Map<TermId, List<Enhancer>> getId2enhancerMap() {
        return id2enhancerMap;
    }
}
