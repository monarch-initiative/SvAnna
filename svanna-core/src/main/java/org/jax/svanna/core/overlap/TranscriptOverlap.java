package org.jax.svanna.core.overlap;

import xyz.ielis.silent.genes.model.Transcript;

/**
 * An object that represents the type and degree of overlap of a structural variant and
 * a transcript or enhancer feature.
 */
public class TranscriptOverlap {

    private final OverlapType overlapType;
    /**
     * This field's meaning depends on the type, INTERGENIC, INTRONIC, EXONIC, SPANNING.
     */
    private final OverlapDistance overlapDistance;

    private final String accessionId;

    private final String description;

    public static TranscriptOverlap of(OverlapType type, String accessionId, OverlapDistance odist, String desc) {
        return new TranscriptOverlap(type, accessionId, odist, desc);
    }

    private TranscriptOverlap(OverlapType type, String accessionId, OverlapDistance odist, String desc) {
        this.overlapType = type;
        this.accessionId = accessionId;
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
        return overlapDistance.overlapsCds();
    }

    public String getAccession() {
        return accessionId;
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
