package org.jax.svann.vcf;

import org.jax.svann.except.SvAnnRuntimeException;
import org.jax.svann.structuralvar.SvAnn;
import org.jax.svann.structuralvar.SvDeletionSimple;
import org.jax.svann.structuralvar.SvTranslocation;
import org.jax.svann.structuralvar.SvType;

public class SvAnnFactory {


    public static SvAnn fromBnd(BndAnnotation bann) {
        String mateAref = bann.getMate_a_ref();
        String mateBref = bann.getMate_b_ref();
        String mateAalt = bann.getMate_a_alt();
        String mateBalt = bann.getMate_b_alt();

        BndType bndTypeA = BndType.fromAltString(mateAalt);
        BndType bndTypeB = BndType.fromAltString(mateBalt);
        if (bann.bothContigsIdentical()) {
            SvType svtype = SvType.UNKNOWN;
            if (bndTypeA.equals(BndType.CASE_1) && bndTypeB.equals(BndType.CASE_3)) {
                return new SvDeletionSimple(
                        bann.getMate_a_id(),
                        bann.getMateId(),
                        bann.getMate_a_contig(),
                        bann.getMate_a_start(),
                        bann.getMate_b_start());
            } else if (bndTypeA.equals(BndType.CASE_3) && bndTypeB.equals(BndType.CASE_1)) {
                return new SvDeletionSimple(
                        bann.getMate_a_id(),
                        bann.getMateId(),
                        bann.getMate_a_contig(),
                        bann.getMate_a_start(),
                        bann.getMate_b_start());
            } else if ( (bndTypeA.isCase4() && bndTypeB.isCase4() ) ||
                    (bndTypeA.isCase2() && bndTypeB.isCase2()) )  {
                return new SvDeletionSimple(
                        bann.getMate_a_id(),
                        bann.getMateId(),
                        bann.getMate_a_contig(),
                        bann.getMate_a_start(),
                        bann.getMate_b_start());
            }
          throw new SvAnnRuntimeException("Could not find type on same chromosomee");
        }

        if (bann.differentContigs()) {
            // TODO -- exploit the different cases!
            // td do decide how to model the different translocation types
            return new SvTranslocation(
                    bann.getMate_a_id(),
                    bann.getMateId(),
                    bann.getMate_a_contig(),
                    bann.getMate_b_contig(),
                    bann.getMate_a_start(),
                    bann.getMate_b_start());
        }

        System.out.println("A(ref):" + mateAref + "; (alt):" + String.join(";", mateAalt));
        System.out.println("B(ref):" + mateBref + "; (alt):" + String.join(";", mateBalt));


        throw new SvAnnRuntimeException("Could not find type on different chromosomee");
    }


}
