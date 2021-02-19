package org.jax.svanna.core.exception;

import org.monarchinitiative.svart.Strand;
import org.monarchinitiative.svart.Variant;
import org.slf4j.Logger;

public class LogUtils {

    private LogUtils() {
        // private no-op
    }

    public static String variantSummary(Variant variant) {
        return String.format("%s %s:%d-%d %s>%s", variant.id(), variant.contigName(),
                variant.startOnStrand(Strand.POSITIVE), variant.endOnStrand(Strand.POSITIVE), variant.ref(), variant.alt());
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

}
