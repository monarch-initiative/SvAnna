package org.jax.svanna.core.prioritizer;

import org.jax.svanna.core.hpo.GeneWithId;
import org.jax.svanna.core.hpo.HpoDiseaseSummary;
import org.jax.svanna.core.landscape.Enhancer;
import org.jax.svanna.core.overlap.Overlap;
import org.jax.svanna.core.reference.Transcript;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * This class organizes the prioritization approach to
 */
class DefaultAnnotatedSvPriority implements AnnotatedSvPriority {
    private static final DefaultAnnotatedSvPriority UNKNOWN = new DefaultAnnotatedSvPriority(SvImpact.UNKNOWN, Set.of(), Set.of(), List.of(), List.of(), List.of());
    private final SvImpact svImpact;
    private final Set<Transcript> affectedTranscripts;
    private final Set<GeneWithId> affectedGeneIds;
    private final List<Enhancer> affectedEnhancers;
    private final List<HpoDiseaseSummary> diseases;
    private final List<Overlap> overlaps;

    DefaultAnnotatedSvPriority(SvImpact svImpact,
                               Set<Transcript> affectedTranscripts,
                               Set<GeneWithId> affectedGeneIds,
                               List<Enhancer> affectedEnhancers,
                               List<Overlap> olaps,
                               List<HpoDiseaseSummary> diseaseList) {
        this.svImpact = svImpact;
        this.affectedTranscripts = affectedTranscripts;
        this.affectedGeneIds = affectedGeneIds;
        this.affectedEnhancers = affectedEnhancers;
        this.diseases = diseaseList;
        overlaps = olaps;
    }

    static DefaultAnnotatedSvPriority unknown() {
        return UNKNOWN;
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
    public Set<Transcript> getAffectedTranscripts() {
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
        return this.diseases.size() > 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DefaultAnnotatedSvPriority that = (DefaultAnnotatedSvPriority) o;
        return
                this.svImpact == that.svImpact &&
                Objects.equals(affectedTranscripts, that.affectedTranscripts) &&
                Objects.equals(affectedGeneIds, that.affectedGeneIds) &&
                Objects.equals(affectedEnhancers, that.affectedEnhancers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(svImpact, affectedTranscripts, affectedGeneIds, affectedEnhancers);
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
}
