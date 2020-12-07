package org.jax.svanna.core.reference;

import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.variant.api.*;

public class Enhancer extends PreciseGenomicRegion {

    private final double tau;
    private final TermId hpoId;
    /**
     * Label of the UBERON or CL term for this enhancer.
     */
    private final String tissueLabel;

    private Enhancer(GenomicRegion region, double tau, TermId hpoId, String tissueLabel) {
        super(region);
        this.tau = tau;
        this.hpoId = hpoId;
        this.tissueLabel = tissueLabel;
    }

    public static Enhancer of(GenomicRegion region, double tau, TermId hpoId, String tissueLabel) {
        return of(region.contig(), region.strand(), region.coordinateSystem(), region.startPosition(), region.endPosition(), tau, hpoId, tissueLabel);
    }
    public static Enhancer of(Contig contig, Strand strand, CoordinateSystem coordinateSystem, Position start, Position end,
                              double tau, TermId hpoId, String tissueLabel) {
        GenomicRegion region = PreciseGenomicRegion.of(contig, strand, coordinateSystem, start, end);
        return new Enhancer(region, tau, hpoId, tissueLabel);
    }

    public double tau() {
        return tau;
    }

    public TermId hpoId() {
        return hpoId;
    }

    public String tissueLabel() {
        return tissueLabel;
    }

    @Override
    public Enhancer withStrand(Strand other) {
        return strand == other
                ? this
                : new Enhancer(super.withStrand(other), tau, hpoId, tissueLabel);
    }

    @Override
    public Enhancer toOppositeStrand() {
        return withStrand(strand.opposite());
    }

    @Override
    public Enhancer withCoordinateSystem(CoordinateSystem other) {
        return coordinateSystem == other
                ? this
                : new Enhancer(super.withCoordinateSystem(other), tau, hpoId, tissueLabel);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        Enhancer enhancer = (Enhancer) o;

        if (Double.compare(enhancer.tau, tau) != 0) return false;
        if (hpoId != null ? !hpoId.equals(enhancer.hpoId) : enhancer.hpoId != null) return false;
        return tissueLabel != null ? tissueLabel.equals(enhancer.tissueLabel) : enhancer.tissueLabel == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        long temp;
        temp = Double.doubleToLongBits(tau);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + (hpoId != null ? hpoId.hashCode() : 0);
        result = 31 * result + (tissueLabel != null ? tissueLabel.hashCode() : 0);
        return result;
    }
}
