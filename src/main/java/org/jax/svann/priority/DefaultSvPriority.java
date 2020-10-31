package org.jax.svann.priority;

import de.charite.compbio.jannovar.reference.TranscriptModel;
import org.jax.svann.genomicreg.Enhancer;
import org.jax.svann.hpo.GeneWithId;
import org.jax.svann.reference.SvType;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.Objects;
import java.util.Set;

public class DefaultSvPriority implements SvPriority {

    private static final DefaultSvPriority UNKNOWN =
            new DefaultSvPriority(SvType.UNKNOWN,SvImpact.UNKNOWN, Set.of(), Set.of(), Set.of(), false);
    private final SvType svType;
    private final SvImpact svImpact;
    private final Set<TranscriptModel> affectedTranscripts;
    private final Set<GeneWithId> affectedGeneIds;
    private final Set<Enhancer> affectedEnhancers;
    private final boolean hasPhenotypicRelevance;

    public DefaultSvPriority(SvType svType,
                             SvImpact svImpact,
                      Set<TranscriptModel> affectedTranscripts,
                      Set<GeneWithId> affectedGeneIds,
                      Set<Enhancer> affectedEnhancers,
                      boolean hasPhenotypicRelevance) {
        this.svType = svType;
        this.svImpact = svImpact;
        this.affectedTranscripts = affectedTranscripts;
        this.affectedGeneIds = affectedGeneIds;
        this.affectedEnhancers = affectedEnhancers;
        this.hasPhenotypicRelevance = hasPhenotypicRelevance;
    }

    static DefaultSvPriority unknown() {
        return UNKNOWN;
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
    public Set<Enhancer> getAffectedEnhancers() {
        return affectedEnhancers;
    }

    /**
     * An SvEvent is phenotypically relevant if it is assigned to one or more diseases.
     */
    @Override
    public boolean hasPhenotypicRelevance() {
        return hasPhenotypicRelevance;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DefaultSvPriority that = (DefaultSvPriority) o;
        return hasPhenotypicRelevance == that.hasPhenotypicRelevance &&
                svImpact == that.svImpact &&
                Objects.equals(affectedTranscripts, that.affectedTranscripts) &&
                Objects.equals(affectedGeneIds, that.affectedGeneIds) &&
                Objects.equals(affectedEnhancers, that.affectedEnhancers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(svImpact, affectedTranscripts, affectedGeneIds, affectedEnhancers, hasPhenotypicRelevance);
    }

    @Override
    public String toString() {
        return "PrioritizedSv{" +
                "svImpact=" + svImpact +
                ", affectedTranscripts=" + affectedTranscripts +
                ", affectedGeneIds=" + affectedGeneIds +
                ", affectedEnhancers=" + affectedEnhancers +
                ", hasPhenotypicRelevance=" + hasPhenotypicRelevance +
                '}';
    }
}
