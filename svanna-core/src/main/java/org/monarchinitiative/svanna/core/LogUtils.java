package org.monarchinitiative.svanna.core;

import org.monarchinitiative.svart.GenomicBreakend;
import org.monarchinitiative.svart.GenomicVariant;
import org.monarchinitiative.svart.Strand;
import org.monarchinitiative.svart.VariantType;
import org.slf4j.Logger;

public class LogUtils {

    private LogUtils() {
        // private no-op
    }

    public static String variantSummary(GenomicVariant variant) {
        if (VariantType.isSymbolic(variant.alt()))
            return String.format("%s %s:%d-%d %s", variant.id(), variant.contigName(),
                    variant.startOnStrand(Strand.POSITIVE), variant.endOnStrand(Strand.POSITIVE), variant.alt());
        return String.format("%s %s:%d-%d %s>%s", variant.id(), variant.contigName(),
                variant.startOnStrand(Strand.POSITIVE), variant.endOnStrand(Strand.POSITIVE), variant.ref(), variant.alt());
    }


    public static String breakendSummary(GenomicBreakend variant) {
        return String.format("%s %s:%d-%d",
                variant.id(), variant.contigName(), variant.startOnStrand(Strand.POSITIVE), variant.endOnStrand(Strand.POSITIVE));
    }

    public static void logError(Logger logger, String template, Object... objects) {
        if (logger.isErrorEnabled())
            logger.error(template, objects);
    }

    public static void logWarn(Logger logger, String template, Object... objects) {
        if (logger.isWarnEnabled())
            logger.warn(template, objects);
    }

    public static void logInfo(Logger logger, String template, Object... objects) {
        if (logger.isInfoEnabled())
            logger.info(template, objects);
    }

    public static void logDebug(Logger logger, String template, Object... objects) {
        if (logger.isDebugEnabled())
            logger.debug(template, objects);
    }

    public static void logTrace(Logger logger, String template, Object... objects) {
        if (logger.isTraceEnabled())
            logger.trace(template, objects);
    }

}
