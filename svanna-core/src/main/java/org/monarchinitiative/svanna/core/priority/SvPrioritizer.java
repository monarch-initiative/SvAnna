package org.monarchinitiative.svanna.core.priority;

import org.monarchinitiative.svart.GenomicVariant;

/**
 * The most general definition of the variant prioritizer which calculates priority {@link P} for a variant.
 * <p>
 * Priority is used to rank variants.
 */
public interface SvPrioritizer<P extends SvPriority> {

    P prioritize(GenomicVariant variant);

}
