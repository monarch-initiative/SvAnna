package org.jax.l2o.vcf;

public class VcfOverlap {

    private final VcfOverlapType overlapType;
    /** This field's meaning depends on the type. For INTERGENIC, it is the distance to the 5' (left) nearest gene.
     * For INTRONIC, it is the distance to the 5' (left) nearest exon.
     */
    private final int leftDistance;

    private final int rightDistance;

    private final String description;


    public VcfOverlap(VcfOverlapType type, int left, int right, String desc) {
        this.overlapType = type;
        this.leftDistance = left;
        this.rightDistance = right;
        this.description = desc;
    }

    @Override
    public String toString() {
        return String.format("VcfOverlap [%s] 5': (%dbp); 3': (%dbp)", description, leftDistance, rightDistance);
    }
}
