package org.jax.svanna.io.hpo;

import com.google.common.collect.ImmutableMap;
import de.charite.compbio.jannovar.impl.intervals.IntervalArray;
import de.charite.compbio.jannovar.impl.intervals.IntervalEndExtractor;
import org.jax.svanna.core.exception.SvAnnRuntimeException;
import org.jax.svanna.core.reference.SomeEnhancer;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.svart.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This class is intended to parse files from the tspec repository
 * https://github.com/pnrobinson/tspec. The file represents FANTOM5 enhancer and has information
 * about tissue specificity
 * <pre>
 * chrom	start	end	tau	ontology.id	ontology.label	HPO.id	HPO.label
 * chr10	100006233	100006603	0.343407	CL:0000071	blood vessel endothelial cell	HP:0002597	Abnormality of the vasculature
 * chr10	100008181	100008444	0.317072	CL:0000359	vascular associated smooth muscle cell	HP:0002597	Abnormality of the vasculature
 * (...)
 * </pre>
 * tau reflects the degree of tissue specificity, where 0 means unspecific (ubiquitously expressed across tissues/cells)
 * and 1 means specific (exclusively expressed in one tissue/cell).
 * [0] chrom; [1]start; [2]end; [3]tau; [4]ontology.id; [5]ontology.label; [6]HPO.id; [7]HPO.label
 */
public class TSpecParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(TSpecParser.class);
    /** key: an HPO id, e.g., HP:0001234; value: corresponding label. TODO may not be needed here, it is more for output*/
    private final Map<TermId, String> id2labelMap;
    /** Key: An HPO id; value: List of {@link SomeEnhancer} objects annotated to the HPO id. */
    private final Map<TermId, List<SomeEnhancer>> id2enhancerMap;
    /** Key: A chromosome id; value: a Jannovar Interval array for searching for overlapping enhancers. */
    private final Map<Integer, IntervalArray<SomeEnhancer>> chromosomeToEnhancerIntervalArrayMap;

    public TSpecParser(Path tspecPath, GenomicAssembly assembly) {
        id2labelMap = new HashMap<>();
        id2enhancerMap = new HashMap<>();
        final List<SomeEnhancer> enhancers = new ArrayList<>();
        try (BufferedReader br = Files.newBufferedReader(tspecPath)) {
            String line;
            br.readLine(); // skip header
            while ((line = br.readLine()) != null) {
                String [] fields = line.split("\t");
                if (fields.length != 8) {
                    throw new SvAnnRuntimeException("Bad tspec line: " + line);
                }
                String chr = fields[0];
                Contig contig = assembly.contigByName(chr);
                if (contig == null) {
                    LOGGER.warn("Unknown contig `{}` for tspec enhancer linee {}", chr, line);
                    continue;
                }
                int start  = Integer.parseInt(fields[1]);
                int end  = Integer.parseInt(fields[2]);
                double tau = Double.parseDouble(fields[3]);
                TermId tid = TermId.of(fields[4]);
                String otherOntologyLabel = fields[5]; // UBERON or CL
                TermId hpoId = TermId.of(fields[6]);
                String hpoLabel = fields[7];
                id2labelMap.putIfAbsent(tid, otherOntologyLabel);
                SomeEnhancer enhancer = SomeEnhancer.of(contig, Strand.POSITIVE, CoordinateSystem.zeroBased(),
                        Position.of(start), Position.of(end),
                        tau, hpoId, otherOntologyLabel);
                id2enhancerMap.putIfAbsent(tid, new ArrayList<>());
                id2enhancerMap.get(tid).add(enhancer);
                enhancers.add(enhancer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        // Now create the Interval Array
        ImmutableMap.Builder<Integer, IntervalArray<SomeEnhancer>> builder = new ImmutableMap.Builder<>();
        HashMap<Integer, ArrayList<SomeEnhancer>> enhancerMap = new HashMap<>();
        assembly.contigs().stream()
                .map(Contig::id).
                 forEach(i -> enhancerMap.put(i, new ArrayList<>()));

        for (SomeEnhancer e : enhancers)
            enhancerMap.get(e.contigId()).add(e);
        Set<Contig> chromosomeIdSet = enhancers
               .stream()
               .map(SomeEnhancer::contig)
               .collect(Collectors.toSet());
        EnhancerIntervalEndExtractor enhancerEndExtractor = new EnhancerIntervalEndExtractor();
        for (Contig contig: chromosomeIdSet) {
            IntervalArray<SomeEnhancer> iTree = new IntervalArray<>(enhancerMap.get(contig.id()), enhancerEndExtractor);
            builder.put(contig.id(), iTree);
        }
        chromosomeToEnhancerIntervalArrayMap = builder.build();
    }

    public Map<TermId, String> getId2labelMap() {
        return id2labelMap;
    }

    public Map<TermId, List<SomeEnhancer>> getId2enhancerMap() {
        return id2enhancerMap;
    }

    public Map<Integer, IntervalArray<SomeEnhancer>> getChromosomeToEnhancerIntervalArrayMap() {
        return chromosomeToEnhancerIntervalArrayMap;
    }

    /**
     * This class is required to build the Interval Array using Jannovar classes.
     * Note that in our implementation, all {@link SomeEnhancer} objects are on the POSITIVE strand.
     */
    private static class EnhancerIntervalEndExtractor implements IntervalEndExtractor<SomeEnhancer> {
        @Override
        public int getBegin(SomeEnhancer x) {
            return x.startOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased());
        }

        @Override
        public int getEnd(SomeEnhancer x) {
            return x.endOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased());
        }
    }


}
