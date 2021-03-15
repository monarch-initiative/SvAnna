package org.jax.svanna.core.priority;

import org.monarchinitiative.svart.Variant;

/**
 * The most general definition of the variant prioritizer which calculates priority {@link P} for a variant {@link V}.
 * <p>
 * Priority is used to rank variants.
 */
public interface SvPrioritizer<V extends Variant, P extends SvPriority> {

    P prioritize(V variant);

}
