package org.jax.svanna.core.reference;

import org.monarchinitiative.svart.CoordinateSystem;
import org.monarchinitiative.svart.GenomicRegion;
import org.monarchinitiative.svart.Strand;

import java.util.List;
import java.util.Optional;

public interface Transcript extends GenomicRegion {

    String accessionId();

    // TODO - should be moved to gene?
    String hgvsSymbol();

    Optional<GenomicRegion> cdsRegion();

    default boolean isCoding() {
        return cdsRegion().isPresent();
    }

    List<GenomicRegion> exons();

    @Override
    Transcript withCoordinateSystem(CoordinateSystem coordinateSystem);

    @Override
    Transcript withStrand(Strand other);
}
