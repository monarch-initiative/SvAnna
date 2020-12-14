package org.jax.svann.overlap;

import org.jax.svann.reference.transcripts.SvAnnTxModel;

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

    private final SvAnnTxModel transcriptModel;

    private final String description;


    public Overlap(OverlapType type, SvAnnTxModel tx, OverlapDistance odist) {
        this(type, tx, odist, odist.getDescription());
    }


    public Overlap(OverlapType type, SvAnnTxModel tx, OverlapDistance odist, String desc) {
        this.overlapType = type;
        this.transcriptModel = tx;
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
        return this.transcriptModel.getGeneSymbol();
    }

    public String getAccession() {
        return this.transcriptModel.getAccession();
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

    public SvAnnTxModel getTranscriptModel() {
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
