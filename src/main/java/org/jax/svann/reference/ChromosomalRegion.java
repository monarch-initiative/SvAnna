package org.jax.svann.reference;

import java.util.List;

public interface ChromosomalRegion extends ChromosomalCoordinates {

    GenomePosition getBegin();

    GenomePosition getEnd();

    @Override
    default List<GenomePosition> getPositions() {
        return List.of(getBegin(), getEnd());
    }
}
