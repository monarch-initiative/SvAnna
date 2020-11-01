package org.jax.svann.priority;

import de.charite.compbio.jannovar.reference.TranscriptModel;
import org.jax.svann.genomicreg.Enhancer;
import org.jax.svann.hpo.GeneWithId;
import org.jax.svann.reference.SequenceRearrangement;
import org.jax.svann.reference.SvType;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public class DefaultSvPriority implements SvPriority {
    /** TODO -- can we delete this object? */
    private static final DefaultSvPriority UNKNOWN =
            new DefaultSvPriority(null, SvType.UNKNOWN,SvImpact.UNKNOWN, Set.of(), Set.of(), List.of());
    private final SequenceRearrangement rearrangement;
    private final SvType svType;
    private final SvImpact svImpact;
    private final Set<TranscriptModel> affectedTranscripts;
    private final Set<GeneWithId> affectedGeneIds;
    private final List<Enhancer> affectedEnhancers;


    public DefaultSvPriority(SequenceRearrangement rearrangement,
                             SvType svType,
                             SvImpact svImpact,
                             Set<TranscriptModel> affectedTranscripts,
                             Set<GeneWithId> affectedGeneIds,
                             List<Enhancer> affectedEnhancers) {
        this.rearrangement = rearrangement;
        this.svType = svType;
        this.svImpact = svImpact;
        this.affectedTranscripts = affectedTranscripts;
        this.affectedGeneIds = affectedGeneIds;
        this.affectedEnhancers = affectedEnhancers;
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


    public static SvPriority createBaseSvPriority(SequenceRearrangement rearrangement) {
        return new DefaultSvPriority(rearrangement, SvType.UNKNOWN, SvImpact.UNKNOWN, Set.of(),Set.of(),List.of());
    }


}
