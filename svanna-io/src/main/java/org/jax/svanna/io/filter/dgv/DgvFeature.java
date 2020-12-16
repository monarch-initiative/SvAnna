package org.jax.svanna.io.filter.dgv;

import org.jax.svanna.core.filter.SVFeatureOrigin;
import org.jax.svanna.core.filter.SvFeature;
import org.monarchinitiative.variant.api.*;

import java.util.Objects;

class DgvFeature extends BaseGenomicRegion<DgvFeature> implements SvFeature {

    private final String accession;
    private final VariantType variantType;
    private final float frequency;
    private DgvFeature(Contig contig,
                       Strand strand,
                       CoordinateSystem coordinateSystem,
                       Position startPosition,
                       Position endPosition,
                       String accession,
                       VariantType variantType,
                       float frequency) {
        super(contig, strand, coordinateSystem, startPosition, endPosition);

        this.accession = Objects.requireNonNull(accession);
        this.variantType = Objects.requireNonNull(variantType);
        if (1 < frequency || frequency < 0) {
            throw new IllegalArgumentException("Frequency must be in range [0,1]: " + frequency);
        }
        this.frequency = frequency;
    }

    static DgvFeature of(Contig contig,
                         Strand strand,
                         CoordinateSystem coordinateSystem,
                         int startPosition,
                         int endPosition,
                         String accession,
                         VariantType variantType,
                         float frequency) {
        return new DgvFeature(contig, strand, coordinateSystem, Position.of(startPosition), Position.of(endPosition), accession, variantType, frequency);
    }

    public String accession() {
        return accession;
    }

    @Override
    public SVFeatureOrigin getOrigin() {
        return SVFeatureOrigin.DGV;
    }

    @Override
    public VariantType variantType() {
        return variantType;
    }

    @Override
    public float frequency() {
        return frequency;
    }

    @Override
    protected DgvFeature newRegionInstance(Contig contig, Strand strand, CoordinateSystem coordinateSystem,
                                           Position start, Position end) {
        return new DgvFeature(contig, strand, coordinateSystem, start, end, accession, variantType, frequency);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        DgvFeature that = (DgvFeature) o;
        return Float.compare(that.frequency, frequency) == 0 && Objects.equals(accession, that.accession) && variantType == that.variantType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), accession, variantType, frequency);
    }

    @Override
    public String toString() {
        return "DgvFeature{" +
                "accession='" + accession + '\'' +
                ", variantType=" + variantType +
                ", frequency=" + frequency +
                "} " + super.toString();
    }
}
