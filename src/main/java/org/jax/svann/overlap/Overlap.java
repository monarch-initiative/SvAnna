package org.jax.svann.overlap;

/**
 * An object that represents the type and degree of overlap of a structural variant and
 * a transcript or enhancer feature.
 */
public class Overlap {

    private final OverlapType overlapType;
    /** This field's meaning depends on the type. For INTERGENIC, it is the distance to the 5' (left) nearest gene.
     * For INTRONIC, it is the distance to the 5' (left) nearest exon.
     */
    private final int leftDistance;

    private final int rightDistance;

    private final String description;

    private boolean overlapsCds;


    public Overlap(OverlapType type, int left, int right, String desc) {
        this.overlapType = type;
        this.leftDistance = left;
        this.rightDistance = right;
        this.description = desc;
        this.overlapsCds = false;
    }

    public Overlap(OverlapType type,  boolean overlapsCds, String desc) {
        this.overlapType = type;
        this.leftDistance = 0;
        this.rightDistance = 0;
        this.overlapsCds = overlapsCds;
        this.description = desc;
    }



    /**
     * @todo REVISE ME
     * @return
     */
    public boolean isCoding() {
        return OverlapType.isExonic(this.overlapType);
    }

    @Override
    public String toString() {
        return String.format("VcfOverlap [%s:%s] 5': (%dbp); 3': (%dbp)",
                overlapType, description, leftDistance, rightDistance);
    }
}
