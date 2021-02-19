package org.jax.svanna.cli.cmd;

import java.util.concurrent.ForkJoinPool;

public class PoolUtils {

    private PoolUtils() {
    }

    /**
     * Prepare <code>ForkJoinPool</code> for variant annotation.
     *
     * @param parallelism number of threads to use for variant annotation
     * @return the pool
     */
    public static ForkJoinPool makePool(int parallelism) {
        return new ForkJoinPool(parallelism, SvAnnaWorkerThread::new, null, false);
    }
}
