package org.jax.svanna.core.landscape;

import org.monarchinitiative.svart.*;

import java.util.Objects;

public class BasePopulationVariant extends BaseGenomicRegion<BasePopulationVariant> implements PopulationVariant {

    private final String id;
    private final VariantType variantType;
    private final float alleleFrequency;
    private final PopulationVariantOrigin populationVariantOrigin;

    public static BasePopulationVariant of(Contig contig, Strand strand, CoordinateSystem coordinateSystem, Position startPosition, Position endPosition,
                                           String id, VariantType variantType, float alleleFrequency, PopulationVariantOrigin populationVariantOrigin) {
        return new BasePopulationVariant(contig, strand, coordinateSystem, startPosition, endPosition, id, variantType, alleleFrequency, populationVariantOrigin);
    }

    protected BasePopulationVariant(Contig contig, Strand strand, CoordinateSystem coordinateSystem, Position startPosition, Position endPosition,
                                    String id, VariantType variantType, float alleleFrequency, PopulationVariantOrigin populationVariantOrigin) {
        super(contig, strand, coordinateSystem, startPosition, endPosition);
        this.id = id;
        this.variantType = variantType;
        this.alleleFrequency = alleleFrequency;
        this.populationVariantOrigin = populationVariantOrigin;
    }

    @Override
    public String id() {
        return id;
    }

    public VariantType variantType() {
        return variantType;
    }

    @Override
    public PopulationVariantOrigin origin() {
        return populationVariantOrigin;
    }

    @Override
    public float alleleFrequency() {
        return alleleFrequency;
    }

    @Override
    protected BasePopulationVariant newRegionInstance(Contig contig, Strand strand, CoordinateSystem coordinateSystem, Position startPosition, Position endPosition) {
        return new BasePopulationVariant(contig, strand, coordinateSystem, startPosition, endPosition, id, variantType, alleleFrequency, populationVariantOrigin);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        BasePopulationVariant that = (BasePopulationVariant) o;
        return Float.compare(that.alleleFrequency, alleleFrequency) == 0 && Objects.equals(id, that.id) && variantType == that.variantType && populationVariantOrigin == that.populationVariantOrigin;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), id, variantType, alleleFrequency, populationVariantOrigin);
    }

    @Override
    public String toString() {
        return "Popvar{" +
                "region=" + super.toString() +
                ", id=" + id +
                ", variantType=" + variantType +
                ", alleleFrequency=" + alleleFrequency +
                ", popvarSource=" + populationVariantOrigin +
                '}';
    }
}