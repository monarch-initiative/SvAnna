package org.jax.svanna.cli.cmd;

import org.jax.svanna.core.exception.LogUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;

public class TaskUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskUtils.class);
    private static final Thread.UncaughtExceptionHandler HANDLER = (thread, throwable) ->
            LogUtils.logError(LOGGER, "Error on thread {}: {}", thread.getName(), throwable.getMessage());

    private TaskUtils() {
    }

    public static <T> T executeBlocking(Callable<T> callable, int parallelism) throws ExecutionException, InterruptedException {
        ForkJoinPool pool = new ForkJoinPool(parallelism, SvAnnaWorkerThread::new, HANDLER, false);
        ForkJoinTask<T> task = pool.submit(callable);
        pool.shutdown();
        return task.get();
    }
}
