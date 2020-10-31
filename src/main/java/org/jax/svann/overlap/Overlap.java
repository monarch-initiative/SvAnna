package org.jax.svann.overlap;

import de.charite.compbio.jannovar.reference.TranscriptModel;

/**
 * An object that represents the type and degree of overlap of a structural variant and
 * a transcript or enhancer feature.
 */
public class Overlap {

    private final OverlapType overlapType;
    /** This field's meaning depends on the type. For INTERGENIC, it is the distance to the 5' (left) nearest gene.
     * For INTRONIC, it is the distance to the nearest exon.
     */
    private final int distance;

    private final TranscriptModel transcriptModel;

    private final String description;

    private final boolean overlapsCds;


    public Overlap(OverlapType type, TranscriptModel tmod, int d, String desc) {
        this.overlapType = type;
        this.distance = d;
        this.transcriptModel = tmod;
        this.description = desc;
        this.overlapsCds = false;
    }

    public Overlap(OverlapType type, TranscriptModel tmod, boolean overlapsCds, String desc) {
        this.overlapType = type;
        this.transcriptModel = tmod;
        this.distance = 0;
        this.overlapsCds = overlapsCds;
        this.description = desc;
    }



    /**
     * @return true if this overlap involves exonic sequence
     */
    public boolean isExonic() {
        return OverlapType.isExonic(this.overlapType);
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
        return distance;
    }

    public TranscriptModel getTranscriptModel() {
        return transcriptModel;
    }

    public String getDescription() {
        return description;
    }

    public boolean isOverlapsCds() {
        return overlapsCds;
    }

    //    public TermId getGeneId() {
//        return TermId.of(this.transcriptModel.getGeneID())
//    }

    @Override
    public String toString() {
        return String.format("VcfOverlap [%s:%s] %dbp; 3'",
                overlapType, description, distance);
    }
}
