package org.jax.svann.vcf;

import org.yaml.snakeyaml.emitter.ScalarAnalysis;

public class SvAnnFactory {


    public static SvAnn fromBnd(BndAnnotation bann) {
        String mateAref = bann.getMate_a_ref();
        String mateBref = bann.getMate_b_ref();
        String mateAalt = bann.getMate_a_alt();
        String mateBalt = bann.getMate_b_alt();

        BndType bndTypeA = BndType.fromAltString(mateAalt);
        BndType bndTypeB = BndType.fromAltString(mateBalt);
        if (bann.bothContigsIdentical()) {
            if (bann.bothContigsIdentical() && bndTypeA.equals(BndType.CASE_1) && bndTypeB.equals(BndType.CASE_3)) {
                // simple deletion, no 'twisting'
                SvAnn.SvAnnBuilder builder = new SvAnn.SvAnnBuilder(SvType.DELETION)
                        .chromA(bann.getMate_a_contig())
                        .chromB(bann.getMate_b_contig())
                        .svlen(bann.getMateDistance())
                        .startPos(bann.getMate_a_start())
                        .endPos(bann.getMate_b_start());

                return builder.build();
            }
        }

        if (bann.differentContigs() && bndTypeA.isCase1() && bndTypeB.isCase2()) {
            SvAnn.SvAnnBuilder builder = new SvAnn.SvAnnBuilder(SvType.TRANSLOCATION)
                    .chromA(bann.getMate_a_contig())
                    .chromB(bann.getMate_b_contig());
            return builder.build();
        } else if (bann.differentContigs()) {
            // td do decide how to model the different translocation types
            SvAnn.SvAnnBuilder builder = new SvAnn.SvAnnBuilder(SvType.TRANSLOCATION)
                    .chromA(bann.getMate_a_contig())
                    .chromB(bann.getMate_b_contig());
            return builder.build();
        }

        System.out.println("A(ref):" + mateAref + "; (alt):" + String.join(";", mateAalt));
        System.out.println("B(ref):" + mateBref + "; (alt):" + String.join(";", mateBalt));


        SvAnn.SvAnnBuilder b2 = new SvAnn.SvAnnBuilder(SvType.UNKNOWN);

        return b2.build();
    }


}
