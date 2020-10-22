package org.jax.svann.reference;

public interface BreakendCoordinate {

    ChromosomalRegion getPosition();

    BreakendDirection getDirection();

    default Strand getStrand() {
        return getPosition().getStrand();
    }
}
