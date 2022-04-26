package org.monarchinitiative.svanna.benchmark.util;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Daniel Danis
 */
class SvAnnaWorkerThread extends ForkJoinWorkerThread {

    private static final AtomicInteger THREAD_COUNTER = new AtomicInteger(1);

    /**
     * Creates a ForkJoinWorkerThread operating in the given pool.
     *
     * @param pool the pool this thread works in
     * @throws NullPointerException if pool is null
     */
    SvAnnaWorkerThread(ForkJoinPool pool) {
        super(pool);
        setName("svanna-worker-" + THREAD_COUNTER.getAndIncrement());
    }
}
