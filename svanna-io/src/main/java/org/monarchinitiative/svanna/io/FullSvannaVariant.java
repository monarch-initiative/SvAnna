package org.monarchinitiative.svanna.io;

import htsjdk.variant.variantcontext.VariantContext;
import org.monarchinitiative.svanna.core.priority.SvPriority;
import org.monarchinitiative.svanna.core.reference.SvannaVariant;
import org.monarchinitiative.svanna.core.reference.VariantCallAttributes;
import org.monarchinitiative.svart.GenomicVariant;

import java.util.Set;

public interface FullSvannaVariant extends SvannaVariant {

    VariantContext variantContext();

    static FullSvannaVariant of(GenomicVariant variant,
                                VariantCallAttributes variantCallAttributes,
                                SvPriority svPriority,
                                VariantContext variantContext) {
        return FullSvannaVariantDefault.of(variant, variantCallAttributes, Set.of(), Set.of(), svPriority, variantContext);
    }

    static FullSvannaVariantBuilder builder(GenomicVariant variant) {
        return new FullSvannaVariantBuilder(variant);
    }

}
