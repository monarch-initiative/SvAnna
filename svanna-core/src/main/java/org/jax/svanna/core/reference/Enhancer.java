package org.jax.svanna.core.reference;

import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.variant.api.*;

import java.util.Objects;

public class Enhancer extends BaseGenomicRegion<Enhancer> {

    private final double tau;
    private final TermId hpoId;
    /**
     * Label of the UBERON or CL term for this enhancer.
     */
    private final String tissueLabel;

    private Enhancer(Contig contig, Strand strand, CoordinateSystem coordinateSystem, Position start, Position end,
                     double tau, TermId hpoId, String tissueLabel) {
        super(contig, strand, coordinateSystem, start, end);
        this.tau = tau;
        this.hpoId = Objects.requireNonNull(hpoId);
        this.tissueLabel = Objects.requireNonNull(tissueLabel);
    }

    public static Enhancer of(GenomicRegion region, double tau, TermId hpoId, String tissueLabel) {
        return of(region.contig(), region.strand(), region.coordinateSystem(), region.startPosition(), region.endPosition(), tau, hpoId, tissueLabel);
    }

    public static Enhancer of(Contig contig, Strand strand, CoordinateSystem coordinateSystem, Position start, Position end,
                              double tau, TermId hpoId, String tissueLabel) {
        return new Enhancer(contig, strand, coordinateSystem, start, end, tau, hpoId, tissueLabel);
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
    protected Enhancer newRegionInstance(Contig contig, Strand strand, CoordinateSystem coordinateSystem, Position startPosition, Position endPosition) {
        return new Enhancer(contig, strand, coordinateSystem, startPosition, endPosition, tau, hpoId, tissueLabel) ;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Enhancer enhancer = (Enhancer) o;
        return Double.compare(enhancer.tau, tau) == 0 && Objects.equals(hpoId, enhancer.hpoId) && Objects.equals(tissueLabel, enhancer.tissueLabel);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), tau, hpoId, tissueLabel);
    }

    @Override
    public String toString() {
        return "Enhancer{" +
                "tau=" + tau +
                ", hpoId=" + hpoId +
                ", tissueLabel='" + tissueLabel + '\'' +
                "} " + super.toString();
    }
}
