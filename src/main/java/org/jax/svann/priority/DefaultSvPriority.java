package org.jax.svann.priority;

import de.charite.compbio.jannovar.reference.TranscriptModel;
import org.jax.svann.genomicreg.Enhancer;
import org.jax.svann.hpo.GeneWithId;
import org.jax.svann.hpo.HpoDiseaseSummary;
import org.jax.svann.overlap.Overlap;
import org.jax.svann.reference.SequenceRearrangement;
import org.jax.svann.reference.SvType;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public class DefaultSvPriority implements SvPriority {
    /** TODO -- can we delete this object? */
    private static final DefaultSvPriority UNKNOWN =
            new DefaultSvPriority(null, SvType.UNKNOWN,SvImpact.UNKNOWN, Set.of(), Set.of(), List.of(), List.of());
    private final SequenceRearrangement rearrangement;
    private final SvType svType;
    private final SvImpact svImpact;
    private final Set<TranscriptModel> affectedTranscripts;
    private final Set<GeneWithId> affectedGeneIds;
    private final List<Enhancer> affectedEnhancers;
    private final List<HpoDiseaseSummary> diseases;
    private final List<Overlap> overlaps;

    public DefaultSvPriority(SequenceRearrangement rearrangement,
                             SvType svType,
                             SvImpact svImpact,
                             Set<TranscriptModel> affectedTranscripts,
                             Set<GeneWithId> affectedGeneIds,
                             List<Enhancer> affectedEnhancers,
                             List<Overlap> olaps) {
        this.rearrangement = rearrangement;
        this.svType = svType;
        this.svImpact = svImpact;
        this.affectedTranscripts = affectedTranscripts;
        this.affectedGeneIds = affectedGeneIds;
        this.affectedEnhancers = affectedEnhancers;
        diseases = List.of(); // not relevant at this stage
        overlaps = olaps;
    }

    public DefaultSvPriority(SvPriority svprio,
                             List<HpoDiseaseSummary> diseaseList) {
        this.rearrangement = svprio.getRearrangement();
        this.svType = svprio.getType();
        if (diseaseList.isEmpty()) {
            switch (svprio.getImpact()) {
                case HIGH_IMPACT:
                    this.svImpact = SvImpact.INTERMEDIATE_IMPACT;
                    break;
                case INTERMEDIATE_IMPACT:
                    this.svImpact = SvImpact.LOW_IMPACT;
                    break;
                default:
                    this.svImpact = svprio.getImpact();
            }
        } else {
            this.svImpact = svprio.getImpact();
        }

        this.affectedTranscripts = svprio.getAffectedTranscripts();
        this.affectedGeneIds = svprio.getAffectedGeneIds();
        this.affectedEnhancers = svprio.getAffectedEnhancers();
        this.overlaps = svprio.getOverlaps();
        this.diseases = diseaseList;
    }

    static DefaultSvPriority unknown() {
        return UNKNOWN;
    }

    @Override
    public SequenceRearrangement getRearrangement() {
        return this.rearrangement;
    }

    @Override
    public SvType getType() {
        return this.svType;
    }

    @Override
    public SvImpact getImpact() {
        return svImpact;
    }

    @Override
    public List<HpoDiseaseSummary> getDiseases() {
        return this.diseases;
    }

    @Override
    public Set<TranscriptModel> getAffectedTranscripts() {
        return affectedTranscripts;
    }

    @Override
    public Set<GeneWithId> getAffectedGeneIds() {
        return affectedGeneIds;
    }

    @Override
    public List<Enhancer> getAffectedEnhancers() {
        return affectedEnhancers;
    }

    @Override
    public List<Overlap> getOverlaps() {
        return overlaps;
    }

    /**
     * An SvEvent is phenotypically relevant if it is assigned to one or more diseases.
     */
    @Override
    public boolean hasPhenotypicRelevance() {
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DefaultSvPriority that = (DefaultSvPriority) o;
        return  Objects.equals(this.rearrangement, that.rearrangement) &&
                this.svType == that.svType &&
                this.svImpact == that.svImpact &&
                Objects.equals(affectedTranscripts, that.affectedTranscripts) &&
                Objects.equals(affectedGeneIds, that.affectedGeneIds) &&
                Objects.equals(affectedEnhancers, that.affectedEnhancers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rearrangement, svType, svImpact, affectedTranscripts, affectedGeneIds, affectedEnhancers);
    }

    @Override
    public String toString() {
        return "PrioritizedSv{" +
                "svImpact=" + svImpact +
                ", affectedTranscripts=" + affectedTranscripts +
                ", affectedGeneIds=" + affectedGeneIds +
                ", affectedEnhancers=" + affectedEnhancers +
                '}';
    }

   // TODO do we really need this? It is ugly
    public static SvPriority createBaseSvPriority(SequenceRearrangement rearrangement) {
        return new DefaultSvPriority(rearrangement, SvType.UNKNOWN, SvImpact.UNKNOWN, Set.of(),Set.of(),List.of(), List.of());
    }


}
