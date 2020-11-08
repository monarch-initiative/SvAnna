package org.jax.svann.overlap;

import de.charite.compbio.jannovar.reference.TranscriptModel;

/**
 * An object that represents the type and degree of overlap of a structural variant and
 * a transcript or enhancer feature.
 */
public class Overlap {

    private final OverlapType overlapType;
    /**
     * This field's meaning depends on the type, INTERGENIC, INTRONIC, EXONIC, SPANNING.
     */
    private OverlapDistance overlapDistance;

    private final TranscriptModel transcriptModel;

    private final String description;

    private final boolean overlapsCds;



    public Overlap(OverlapType type, TranscriptModel tmod, OverlapDistance odist) {
        this.overlapType = type;
        this.transcriptModel = tmod;
        this.description = odist.getDescription();
        this.overlapsCds = false;
        this.overlapDistance = odist;
    }



    public Overlap(OverlapType type, TranscriptModel tmod, OverlapDistance odist, String desc) {
        this.overlapType = type;
        this.overlapDistance = odist;
        this.transcriptModel = tmod;
        this.description = desc;
        this.overlapsCds = false;
    }


    /**
     * @return true if this overlap involves exonic sequence
     */
    public boolean isExonic() {
        return this.overlapType.isExonic();
    }

    public boolean overlapsTranscript() {
        return OverlapType.overlapsTranscript(this.overlapType);
    }

    public boolean inversionDisruptable() {
        return OverlapType.inversionDisruptable(this.overlapType);
    }

    /**
     * @return true if this overlap involves exonic sequence
     */
    public boolean overlapsCds() {
        return overlapsCds;
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

    public OverlapDistance getOverlapDistance() { return this.overlapDistance; }

    public TranscriptModel getTranscriptModel() {
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
