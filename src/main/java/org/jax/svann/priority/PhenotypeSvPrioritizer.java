package org.jax.svann.priority;

import org.jax.svann.reference.SequenceRearrangement;

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
public class PhenotypeSvPrioritizer implements SvPrioritizer {
    @Override
    public SvPriority prioritize(SvPriority rearrangement) {
        return null;
    }
}
