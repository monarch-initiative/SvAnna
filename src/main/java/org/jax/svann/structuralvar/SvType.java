package org.jax.svann.structuralvar;

import org.jax.svann.except.SvAnnRuntimeException;

public enum SvType {
    DELETION, INSERTION, DUPLICATION, CNV, INVERSION, TRANSLOCATION, DEL_INV, DUP_INS, INV_DUP, INV_INV_DUP, BND, UNKNOWN,
    /** A deletion in which both rejoined segments retain the same orientation. */
    DELETION_SIMPLE,
    /** A deletion in which both rejoined segments have a different orientation. */
    DELETION_TWISTED;


    public static SvType fromString(String svt) {
        switch (svt.toUpperCase()) {
            case "DEL":
                return DELETION;
            case "INS":
                return INSERTION;
            case "DUP":
                return DUPLICATION;
            case "CNV":
                return CNV;
            case "INV":
                return INVERSION;
            case "DEL/INV":
                return DEL_INV;
            case "DUP/INS":
                return DUP_INS;
            case "INVDUP":
                return INV_DUP;
            case "INV/INVDUP":
                return INV_INV_DUP;
            case "BND":
                //BND is a catch-all for a generic breakpoint, so you can't assume all to be translocations
                return BND;
            default:
                throw new SvAnnRuntimeException("Did not recognize SV TYPE: \"" + svt + "\"");
        }
    }
}
