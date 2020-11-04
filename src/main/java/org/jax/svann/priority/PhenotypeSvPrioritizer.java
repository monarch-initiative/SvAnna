package org.jax.svann.priority;

import org.jax.svann.hpo.GeneWithId;
import org.jax.svann.hpo.HpoDiseaseSummary;
import org.jax.svann.reference.SequenceRearrangement;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *  This class prioritizes structural variants according to the phenotypic associations of the
 *  genes they affect. The {@link SequenceSvPrioritizer} <b>must</b> be applied first. The rules
 *  are as follows (whereby we define HPO relevance as the fact that the HPO terms entered by
 *  the user match with HPO terms used to annotate diseases associated with a gene affected
 *  by the structural variant).
 *  <ol>
 *      <li>HIGH: A HIGH impact as calculated by {@link SequenceSvPrioritizer} and HPO relevance. </li>
 *      <li>INTERMEDIATE: High sequence impact without HPO relevance, or Intermediate sequence impact
 *      with HPO relevance</li>
 *      <li>LOW: everything else</li>
 *  </ol>
 * @author Peter N Robinson
 * @author Daniel Danis
 */
@Deprecated // in favor of PrototypeSvPrioritizer
public class PhenotypeSvPrioritizer implements SvPrioritizer {

    private final List<TermId> targetHpoIdList;

    private final Map<TermId, Set<HpoDiseaseSummary>> relevantGeneIdToAssociatedDiseaseMap;

    public PhenotypeSvPrioritizer(List<TermId> targetHpos, Map<TermId, Set<HpoDiseaseSummary>> diseaseMap){
        this.targetHpoIdList = targetHpos;
        this.relevantGeneIdToAssociatedDiseaseMap = diseaseMap;
    }


    SvPriority prioritizeByPhenotype(SvPriority svPriority) {
        Set<GeneWithId> genes = svPriority.getAffectedGeneIds();
        List<HpoDiseaseSummary> relevantDiseases = new ArrayList<>();
        for (var gene : genes) {
            TermId tid = gene.getGeneId();
            if (this.relevantGeneIdToAssociatedDiseaseMap.containsKey(tid)) {
                relevantDiseases.addAll(this.relevantGeneIdToAssociatedDiseaseMap.get(tid));
            }
        }
        return new DefaultSvPriority(svPriority, relevantDiseases);
    }

    SvPriority prioritizeInversion(SvPriority svPriority) {
        return prioritizeByPhenotype(svPriority); // TODO IMPLEMENT ME
    }

    SvPriority prioritizeTranslocation(SvPriority svPriority) {
        return prioritizeByPhenotype(svPriority); // TODO IMPLEMENT ME
    }


    /**
     * This method adjusts the sequence-based priority according to whether the
     * involved genes share HPO annotations with {@link #targetHpoIdList}.
     * TODO -- still need to implement special rules for inversions and translocations.
     * @param svPriority
     * @return phenotypic prioritization
     */
//    @Override
    public SvPriority prioritize(SvPriority svPriority) {
        switch (svPriority.getRearrangement().getType()) {
            case TRANSLOCATION:
                return prioritizeTranslocation(svPriority);
            case INVERSION:
                return prioritizeInversion(svPriority);
            default:
                return prioritizeByPhenotype(svPriority);
        }
    }

    @Override
    public SvPriority prioritize(SequenceRearrangement rearrangement) {
        return SvPriority.unknown();
    }
}
