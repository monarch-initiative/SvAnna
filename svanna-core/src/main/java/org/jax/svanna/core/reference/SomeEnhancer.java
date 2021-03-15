package org.jax.svanna.core.reference;

import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.svart.*;

import java.util.Objects;

@Deprecated(forRemoval = true)
public class SomeEnhancer extends BaseGenomicRegion<SomeEnhancer> {

    private final String id = "Enhancer"; // TODO - use
    private final double tau;
    private final TermId hpoId; // TODO - remove
    /**
     * Label of the UBERON or CL term for this enhancer.
     */
    private final String tissueLabel; // TODO - remove

    private SomeEnhancer(Contig contig, Strand strand, CoordinateSystem coordinateSystem, Position start, Position end,
                         double tau, TermId hpoId, String tissueLabel) {
        super(contig, strand, coordinateSystem, start, end);
        this.tau = tau;
        this.hpoId = Objects.requireNonNull(hpoId);
        this.tissueLabel = Objects.requireNonNull(tissueLabel);
    }

    public static SomeEnhancer of(GenomicRegion region, double tau, TermId hpoId, String tissueLabel) {
        return of(region.contig(), region.strand(), region.coordinateSystem(), region.startPosition(), region.endPosition(), tau, hpoId, tissueLabel);
    }

    public static SomeEnhancer of(Contig contig, Strand strand, CoordinateSystem coordinateSystem, Position start, Position end,
                                  double tau, TermId hpoId, String tissueLabel) {
        return new SomeEnhancer(contig, strand, coordinateSystem, start, end, tau, hpoId, tissueLabel);
    }

    public double maxTau() {
        return tau;
    }

    public TermId maxTauHpoTermId() {
        return hpoId;
    }

    public String maxTauTissueName() {
        return tissueLabel;
    }

    @Override
    protected SomeEnhancer newRegionInstance(Contig contig, Strand strand, CoordinateSystem coordinateSystem, Position startPosition, Position endPosition) {
        return new SomeEnhancer(contig, strand, coordinateSystem, startPosition, endPosition, tau, hpoId, tissueLabel) ;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        SomeEnhancer enhancer = (SomeEnhancer) o;
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
