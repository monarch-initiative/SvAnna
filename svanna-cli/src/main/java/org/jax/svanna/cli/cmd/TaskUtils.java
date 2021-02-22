package org.jax.svanna.cli.cmd;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;

public class TaskUtils {

    private TaskUtils() {
    }

    public static <T> T executeBlocking(Callable<T> callable, int parallelism) throws ExecutionException, InterruptedException {
        ForkJoinPool pool = new ForkJoinPool(parallelism, SvAnnaWorkerThread::new, null, false);
        ForkJoinTask<T> task = pool.submit(callable);
        pool.shutdown();
        return task.get();
    }
}
