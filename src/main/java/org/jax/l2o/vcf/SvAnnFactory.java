package org.jax.l2o.vcf;

import java.util.List;

public class SvAnnFactory {



    public static SvAnn fromBnd(BndAnnotation bann) {
        if (bann.bothContigsIdentical()) {
            String mateAref = bann.getMate_a_ref();
            String mateBref = bann.getMate_b_ref();
            List<String> mateAalt = bann.getMate_a_alt();
            List<String> mateBalt = bann.getMate_b_alt();
            System.out.println("A(ref):" + mateAref + "; (alt):" + String.join(";", mateAalt));
            System.out.println("B(ref):" + mateBref + "; (alt):" + String.join(";", mateBalt));
        }




        return null;
    }


}
