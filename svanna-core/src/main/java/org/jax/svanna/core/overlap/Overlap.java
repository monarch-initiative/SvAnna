package org.jax.svanna.core.overlap;

import org.jax.svanna.core.reference.Transcript;

/**
 * An object that represents the type and degree of overlap of a structural variant and
 * a transcript or enhancer feature.
 */
public class Overlap {

    private final OverlapType overlapType;
    /**
     * This field's meaning depends on the type, INTERGENIC, INTRONIC, EXONIC, SPANNING.
     */
    private final OverlapDistance overlapDistance;

    private final Transcript transcriptModel;

    private final String hgvsSymbol;

    private final String description;


    public Overlap(OverlapType type, Transcript tx, String hgvsSymbol, OverlapDistance odist) {
        this(type, tx, hgvsSymbol, odist, odist.getDescription());
    }


    public Overlap(OverlapType type, Transcript tx, String hgvsSymbol, OverlapDistance odist, String desc) {
        this.overlapType = type;
        this.transcriptModel = tx;
        this.hgvsSymbol = hgvsSymbol;
        this.overlapDistance = odist;
        this.description = desc;
    }


    /**
     * @return true if this overlap involves exonic sequence
     */
    public boolean isExonic() {
        return overlapType.isExonic();
    }

    public boolean overlapsTranscript() {
        return OverlapType.overlapsTranscript(overlapType);
    }

    public boolean inversionDisruptable() {
        return OverlapType.inversionDisruptable(overlapType);
    }

    /**
     * @return true if this overlap involves exonic sequence
     */
    public boolean overlapsCds() {
        return overlapDistance.isOverlapsCds();
    }


    public String getGeneSymbol() {
        return hgvsSymbol;
    }

    public String getAccession() {
        return this.transcriptModel.accessionId();
    }

    public OverlapType getOverlapType() {
        return overlapType;
    }

    public int getDistance() {
        return overlapDistance.getShortestDistance();
    }

    public OverlapDistance getOverlapDistance() {
        return overlapDistance;
    }

    public Transcript getTranscriptModel() {
        return transcriptModel;
    }

    public String getDescription() {
        return description;
    }


    @Override
    public String toString() {
        String distanceS = "";//distanceString(this.distance);
        if (this.overlapType.isUpstream()) {
            return "Intergenic/Upstream " + distanceS + "; " + description;
        }
        if (this.overlapType.isDownstream()) {
            return "Intergenic/Downstream " + distanceS + "; " + description;
        }
        if (this.overlapType.isSingleExon()) {
            return description;
        }
        if (this.overlapType.isIntronic()) {
            return this.overlapDistance.getDescription();
        }


        return String.format("VcfOverlap [%s:%s] %dbp; 3'",
                overlapType, description, overlapDistance.getShortestDistance());
    }



}
