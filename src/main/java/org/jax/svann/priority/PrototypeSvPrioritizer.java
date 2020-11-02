package org.jax.svann.priority;

import de.charite.compbio.jannovar.data.JannovarData;
import de.charite.compbio.jannovar.impl.intervals.IntervalArray;
import org.jax.svann.genomicreg.Enhancer;
import org.jax.svann.hpo.GeneWithId;
import org.jax.svann.hpo.HpoDiseaseSummary;
import org.jax.svann.overlap.EnhancerOverlapper;
import org.jax.svann.overlap.Overlapper;
import org.jax.svann.reference.SequenceRearrangement;
import org.jax.svann.reference.genome.GenomeAssembly;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;

public class PrototypeSvPrioritizer implements SvPrioritizer {

    private static final Logger LOGGER = LoggerFactory.getLogger(PrototypeSvPrioritizer.class);

    /**
     * In the first pass, we just look at sequence and do not analyze diseases, so this is the empty set.
     */
    private static final Set<HpoDiseaseSummary> EMPTYSET = Set.of();
    /**
     * Genome assembly in use.
     */
    private final GenomeAssembly assembly;
    /**
     * The overlapper tests overlap of structural variants with transcripts.
     */
    private final Overlapper overlapper;
    /**
     * The enhancer overlapper tests overlap of structural variants with enhancers.
     */
    private final EnhancerOverlapper enhancerOverlapper;
    /**
     * Key -- gene symbol, value, {@link GeneWithId} object with symbol and id
     */
    private final Map<String, GeneWithId> geneSymbolMap;

    /**
     * @param assembly      An object representing the assembly, e.g., HG38
     * @param enhancerMap   A map of enhancers
     * @param geneSymbolMap A map of gene symbols and NCBI ids
     * @param jannovarData  An object that contains representations of all transcripts
     */
    public PrototypeSvPrioritizer(GenomeAssembly assembly,
                                  Map<Integer, IntervalArray<Enhancer>> enhancerMap,
                                  Map<String, GeneWithId> geneSymbolMap,
                                  JannovarData jannovarData) {
        this.assembly = assembly;
        this.geneSymbolMap = geneSymbolMap;
        this.overlapper = new Overlapper(jannovarData);
        this.enhancerOverlapper = new EnhancerOverlapper(jannovarData, enhancerMap);
    }

    @Override
    public SvPriority prioritize(SequenceRearrangement rearrangement) {
        // TODO: 2. 11. 2020 implement
        return SvPriority.unknown();
    }
}
