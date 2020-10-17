package org.jax.l2o.vcf;

import de.charite.compbio.jannovar.impl.intervals.IntervalArray;
import htsjdk.variant.variantcontext.VariantContext;
import org.jax.l2o.except.L2ORuntimeException;

import java.awt.*;
import java.util.List;
import java.util.Map;

/**
 * An object representing a Breakend annotation with the two mates of a breakend.
 */
public class BndAnnotation {

    private final static int NOT_AVAILABLE = -42;

    private final static String NOT_PROVIDED = "";
    /** This should always be BND. */
    private final SvType svType;
    /** We should always have a mate distance for BND, except for single breakends. */
    private final int mateDistance;
    /** confidence interval 5' */
    private final int cpos5;
    /** confidence interval 3' */
    private final int cpos3;

    private final String mateId;

    private final String mate_a_contig;

    private final String mate_a_id;

    private final int mate_a_start;

    /**
     * The constructor is used for the first mate of the breakend pair pair
     * @param attributes
     * @param vc
     */
    public BndAnnotation(Map<String, Object> attributes, VariantContext vc) {
        String myString = (String) attributes.getOrDefault("SVTYPE", "UNKNOWN");
        this.svType = SvType.fromString(myString);
        myString = (String) attributes.getOrDefault("MATEDIST", NOT_PROVIDED);
        if (myString.equals(NOT_PROVIDED)) {
            this.mateDistance = NOT_AVAILABLE;
        } else {
            this.mateDistance = Integer.parseInt(myString);
        }
        if (attributes.containsKey("CIPOS")) {
            List<String> ciPos = (List<String>) attributes.get("CIPOS");
            // this should be a string such as "[-69, 85]"
            if (ciPos.size() != 2) {
                throw new L2ORuntimeException("Malformed CIPOS (after split): \"" + ciPos + "\"");
            }
            cpos5 = Integer.parseInt(ciPos.get(0));
            cpos3 = Integer.parseInt(ciPos.get(1));
        } else {
            cpos5 = NOT_AVAILABLE;
            cpos3 = NOT_AVAILABLE;
        }
        if (attributes.containsKey("MATEID")) {
            this.mateId =  (String) attributes.get("MATEID");
        } else {
            this.mateId = NOT_PROVIDED;
        }
        this.mate_a_id = vc.getID();
        this.mate_a_contig = vc.getContig();
        this.mate_a_start = vc.getStart();
    }


    public SvType getSvType() {
        return svType;
    }

    public int getMateDistance() {
        return mateDistance;
    }

    public int getCpos5() {
        return cpos5;
    }

    public int getCpos3() {
        return cpos3;
    }

    public String getMateId() {
        return mateId;
    }

    public String getMate_a_contig() {
        return mate_a_contig;
    }

    public String getMate_a_id() {
        return mate_a_id;
    }

    public int getMate_a_start() {
        return mate_a_start;
    }

    public void addSecondMate(Map<String, Object> attributes, VariantContext vc) {
        for (String k : attributes.keySet()) {
            System.out.println(k +": " + attributes.get(k));
        }
        System.out.println(vc);
        System.exit(1);
    }
}
