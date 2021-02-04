package org.jax.svanna.io.parse;

import htsjdk.variant.variantcontext.VariantContext;

class Utils {

    private Utils() {
        // static utility class
    }

    static String makeVariantRepresentation(VariantContext vc) {
        return String.format("%s-%d:(%s)", vc.getContig(), vc.getStart(), vc.getID());
    }

}
