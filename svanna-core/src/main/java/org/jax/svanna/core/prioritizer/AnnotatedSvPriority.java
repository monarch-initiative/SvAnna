package org.jax.svanna.core.prioritizer;

import org.jax.svanna.core.hpo.GeneWithId;
import org.jax.svanna.core.hpo.HpoDiseaseSummary;
import org.jax.svanna.core.landscape.Enhancer;
import org.jax.svanna.core.overlap.Overlap;
import org.jax.svanna.core.reference.Transcript;

import java.util.List;
import java.util.Set;

/**
 * This interface currently represents more data that should be provided by {@link SvPrioritizer}, and therefore violates
 * the single responsibility principle.
 * The interface is kept here in order not to break the existing code, mainly {@link PrototypeSvPrioritizer} and the
 * code in the {@link org.jax.svanna.core.viz} package.
 * <p>
 * IMPORTANT - do not implement a new {@link SvPrioritizer} that returns an instance of this interface.
 */
public interface AnnotatedSvPriority extends DiscreteSvPriority {

    static AnnotatedSvPriority unknown() {
        return DefaultAnnotatedSvPriority.unknown();
    }

    List<HpoDiseaseSummary> getDiseases();

    Set<Transcript> getAffectedTranscripts();

    Set<GeneWithId> getAffectedGeneIds();

    List<Enhancer> getAffectedEnhancers();

    List<Overlap> getOverlaps();

}
