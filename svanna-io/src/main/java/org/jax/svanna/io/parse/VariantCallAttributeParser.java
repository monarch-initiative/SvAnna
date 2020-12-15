package org.jax.svanna.io.parse;

import htsjdk.variant.variantcontext.Genotype;
import org.jax.svanna.core.reference.VariantMetadata;
import org.jax.svanna.core.reference.Zygosity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

class VariantCallAttributeParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(VariantCallAttributeParser.class);

    private static final VariantCallAttributeParser INSTANCE = new VariantCallAttributeParser();

    static VariantCallAttributeParser getInstance() {
        return INSTANCE;
    }

    private VariantCallAttributeParser() {
        // private no-op
    }

    VariantCallAttributes parseAttributes(Map<String, Object> attributes, Genotype genotype) {
        // first, parse zygosity
        Zygosity zygosity = parseZygosity(genotype);

        // then read depth
        int dp = VariantMetadata.MISSING_DEPTH_PLACEHOLDER;
        if (genotype.hasDP()) {
            dp = genotype.getDP();
        } else {
            if (attributes.containsKey("RE")) {
                // Sniffles: ##INFO=<ID=RE,Number=1,Type=Integer,Description="read support">
                try {
                    dp = Integer.parseInt((String) attributes.get("RE"));
                } catch (ClassCastException e) {
                    LOGGER.warn("Unable to parse `RE` attribute: {}", attributes.get("RE"));
                }
            }
        }

        // finally allelic depths
        int ref = VariantMetadata.MISSING_DEPTH_PLACEHOLDER;
        int alt = VariantMetadata.MISSING_DEPTH_PLACEHOLDER;
        if (genotype.hasAD()) {
            int[] ads = genotype.getAD();
            ref = ads[0];
            alt = ads[1];
        } else if (genotype.hasExtendedAttribute("DR") && genotype.hasExtendedAttribute("DV")) {
            // Sniffles:
            //  - ##FORMAT=<ID=DR,Number=1,Type=Integer,Description="# high-quality reference reads">
            //  - ##FORMAT=<ID=DV,Number=1,Type=Integer,Description="# high-quality variant reads">
            try {
                ref = Integer.parseInt((String) genotype.getExtendedAttribute("DR", "-1"));
            } catch (ClassCastException e) {
                LOGGER.warn("Unable to cast `DR` attribute: {}", genotype.getExtendedAttribute("DR"));
            } catch (NumberFormatException e) {
                LOGGER.warn("Not an integer in `DR` attribute: {}", genotype.getExtendedAttribute("DR"));
            }
            try {
                alt = Integer.parseInt((String) genotype.getExtendedAttribute("DV", "-1"));
            } catch (ClassCastException e) {
                LOGGER.warn("Unable to cast `DV` attribute: {}", genotype.getExtendedAttribute("DV"));
            } catch (NumberFormatException e) {
                LOGGER.warn("Not an integer in `DV` attribute: {}", genotype.getExtendedAttribute("DV"));
            }
        }

        return new VariantCallAttributes(zygosity, dp, ref, alt);
    }

    private static Zygosity parseZygosity(Genotype gt) {
        switch (gt.getType()) {
            case HET:
                return Zygosity.HETEROZYGOUS;
            case HOM_VAR:
            case HOM_REF:
                return Zygosity.HOMOZYGOUS;
            case NO_CALL:
            case UNAVAILABLE:
            default:
                return Zygosity.UNKNOWN;
        }
    }
}
