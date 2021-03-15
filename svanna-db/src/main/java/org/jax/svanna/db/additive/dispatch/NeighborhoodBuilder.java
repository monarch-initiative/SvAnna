package org.jax.svanna.db.additive.dispatch;

import org.monarchinitiative.svart.Variant;

public interface NeighborhoodBuilder {

    <V extends Variant> Neighborhood intrachromosomalNeighborhood(VariantArrangement<V> arrangement);

    <V extends Variant> Neighborhood interchromosomalNeighborhood(VariantArrangement<V> arrangement);

}
