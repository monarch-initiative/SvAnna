package org.jax.l2o.vcf;

import org.jax.l2o.except.L2ORuntimeException;

public enum BndType {


        CASE_1,
        CASE_2,
        CASE_3,
        CASE_4;



        public static BndType fromAltString(String altString) {
            if (altString.startsWith("[")) {
                int i = altString.indexOf("[",2);
                if (i<0 || i==altString.length()-1) {
                    throw new L2ORuntimeException("Malformed altstring (4): " + altString);
                }
                return CASE_4;
            } else if (altString.startsWith("]")) {
                int i = altString.indexOf("]",2);
                if (i<0 || i==altString.length()-1) {
                    throw new L2ORuntimeException("Malformed altstring (3): " + altString);
                }
                return CASE_3;
            } else if (altString.charAt(altString.length()-1) == '[') {
                int i = altString.indexOf("[");
                if (i>0 && i < altString.length()-1) {
                    return CASE_1;
                } else {
                    throw new L2ORuntimeException("Malformed altstring (1): " + altString);
                }
            } else if (altString.charAt(altString.length()-1) == ']') {
                int i = altString.indexOf("]");
                if (i>0 && i < altString.length()-1) {
                    return CASE_2;
                } else {
                    throw new L2ORuntimeException("Malformed altstring (2): " + altString);
                }
            } else {
                throw new L2ORuntimeException("Malformed altstring (5): " + altString);
            }
        }

        public boolean isCase1() {
            return this.equals(CASE_1);
        }

    public boolean isCase2() {
        return this.equals(CASE_2);
    }

    public boolean isCase3() {
        return this.equals(CASE_3);
    }

    public boolean isCase4() {
        return this.equals(CASE_4);
    }

}
