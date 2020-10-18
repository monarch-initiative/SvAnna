package org.jax.l2o.vcf;

import htsjdk.variant.variantcontext.VariantContext;
import org.jax.l2o.except.L2ORuntimeException;

import java.util.ArrayList;
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
    private final int cpos_a_5;
    /** confidence interval 3' */
    private final int cpos_a_3;
    /** confidence interval 5' for second member of pair*/
    private int cpos_b_5 = NOT_AVAILABLE;
    /** confidence interval 3' for second member of pair*/
    private int cpos_b_3 = NOT_AVAILABLE;
    private final String mateId;

    private final String mate_a_contig;
    /** Chromosome of the second mate of the pair,
     *  if available, this will be set by {@link #addSecondMate(Map, VariantContext)}.
     */
    private String mate_b_contig = NOT_PROVIDED ;

    private final String mate_a_id;

    private final int mate_a_start;
    private final int mate_a_stop;



    private int mate_b_start;
    private int mate_b_stop;

    private String mate_a_ref;
    private String mate_a_alt;
    private String mate_b_ref;
    private String mate_b_alt;

    /** This is set to true if we identify the mate of the first entry (it will remain false for single breakends). */
    private boolean hasMate = false;

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
            cpos_a_5 = Integer.parseInt(ciPos.get(0));
            cpos_a_3 = Integer.parseInt(ciPos.get(1));
        } else {
            cpos_a_5 = NOT_AVAILABLE;
            cpos_a_3 = NOT_AVAILABLE;
        }
        if (attributes.containsKey("MATEID")) {
            this.mateId =  (String) attributes.get("MATEID");
        } else {
            this.mateId = NOT_PROVIDED;
        }
        this.mate_a_id = vc.getID();
        this.mate_a_contig = vc.getContig();
        this.mate_a_start = vc.getStart();
        this.mate_a_stop = vc.getEnd();
        this.mate_a_ref = vc.getReference().getDisplayString();
        if (vc.getAlternateAlleles().size()>1) {
            System.err.println("[WARNING] Multiple alternate alleles encountered. We are taking the first only TODO FIX");
        }

        this.mate_a_alt = vc.getAlternateAllele(0).getDisplayString();

    }


    public SvType getSvType() {
        return svType;
    }

    public int getMateDistance() {
        return mateDistance;
    }

    public int getCpos_a_5() {
        return cpos_a_5;
    }

    public int getCpos3() {
        return cpos_a_3;
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
            System.out.println("[SECOND] " + k +": " + attributes.get(k));
        }
        String mateAId;
        if (attributes.containsKey("MATEID")) {
            mateAId =  (String) attributes.get("MATEID");
        } else {
            mateAId = NOT_PROVIDED;
        }
        if (! mateAId.equals(this.mate_a_id)) {
            String msg = String.format("Mate A ids do not match %s vs. %s (from mate b)", this.mate_a_id, mateAId);
            throw new L2ORuntimeException(msg);
        }
        String myString = (String) attributes.getOrDefault("SVTYPE", "UNKNOWN");
        if (!myString.equals("BND")) {
            throw new L2ORuntimeException("Unexpected SVTYPE annotation: " + myString);
        }
        myString = (String) attributes.getOrDefault("MATEDIST", NOT_PROVIDED);
        int mdist;
        if (myString.equals(NOT_PROVIDED)) {
            mdist = NOT_AVAILABLE;
        } else {
            mdist = Integer.parseInt(myString);
        }
        if (mdist != this.mateDistance) {
            String msg = String.format("Mate distance does not match: from A: %d, from B: %d", this.mateDistance, mdist);
            throw new L2ORuntimeException(msg);
        }
        if (attributes.containsKey("CIPOS")) {
            List<String> ciPos = (List<String>) attributes.get("CIPOS");
            // this should be a string such as "[-69, 85]"
            if (ciPos.size() != 2) {
                throw new L2ORuntimeException("Malformed CIPOS (after split): \"" + ciPos + "\"");
            }
            cpos_b_5 = Integer.parseInt(ciPos.get(0));
            cpos_b_3 = Integer.parseInt(ciPos.get(1));
        }
        // note that cpos_b_* are initialized to NOT_AVAILABLE, we do not need to do that here
        this.mate_b_contig = vc.getContig();
        this.mate_b_start = vc.getStart();
        this.mate_b_stop = vc.getEnd();
        this.mate_b_ref = vc.getReference().getDisplayString();
        if (vc.getAlternateAlleles().size()>1) {
            System.err.println("[WARNING] Multiple alternate alleles encountered. We are taking the first only TODO FIX");
        }
        this.mate_b_alt = vc.getAlternateAllele(0).getDisplayString();
        System.out.println(vc);
        hasMate = true;
    }

    public boolean hasMate() {
        return hasMate;
    }

    public int getCpos_a_3() {
        return cpos_a_3;
    }

    public int getCpos_b_5() {
        return cpos_b_5;
    }

    public int getCpos_b_3() {
        return cpos_b_3;
    }

    public String getMate_b_contig() {
        return mate_b_contig;
    }

    public int getMate_a_stop() {
        return mate_a_stop;
    }

    public int getMate_b_start() {
        return mate_b_start;
    }

    public int getMate_b_stop() {
        return mate_b_stop;
    }

    public String getMate_a_ref() {
        return mate_a_ref;
    }

    public String getMate_a_alt() {
        return mate_a_alt;
    }

    public String getMate_b_ref() {
        return mate_b_ref;
    }

    public String getMate_b_alt() {
        return mate_b_alt;
    }

    public boolean bothContigsIdentical() {
        return this.mate_a_contig.equals(this.mate_b_contig);
    }

    public boolean differentContigs() {
        return ! this.mate_a_contig.equals(this.mate_b_contig);
    }

    @Override
    public String toString() {
        return String.format("[BND] a_id = %s", this.mate_a_id);
    }
}
