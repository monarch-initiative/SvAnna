package org.jax.svanna.core.priority;

import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.svart.Variant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

public interface SvPriorityFactory {

     Logger LOGGER = LoggerFactory.getLogger(SvPriorityFactory.class);

    <V extends Variant, P extends SvPriority> SvPrioritizer<V, P> getPrioritizer(SvPrioritizerType type, Collection<TermId> patientTerms);


}
