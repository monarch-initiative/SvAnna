package org.monarchinitiative.svanna.cli.cmd;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Function;

public class TaskUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskUtils.class);
    private static final Thread.UncaughtExceptionHandler HANDLER = (thread, throwable) ->
            LOGGER.error("Error on thread {}: {}", thread.getName(), throwable.getMessage());

    private TaskUtils() {
    }

    public static <T> T executeBlocking(Callable<T> callable, int parallelism) throws ExecutionException, InterruptedException {
        ForkJoinPool pool = new ForkJoinPool(parallelism, SvAnnaWorkerThread::new, HANDLER, true);
        ForkJoinTask<T> task = pool.submit(callable);
        pool.shutdown();
        return task.get();
    }

    public static <T, U> List<U> executeBlocking(List<T> items, Function<T, U> mapper, int parallelism) {
        ForkJoinPool pool = new ForkJoinPool(parallelism, SvAnnaWorkerThread::new, HANDLER, true);
        Chunk<T, U> task = new Chunk<>(items, mapper);
        List<U> results = pool.invoke(task);
        pool.shutdown();
        return results;
    }

    private static class Chunk<T, U> extends RecursiveTask<List<U>> {

        // We process at most 100 items within a task
        private static final int GRANULARITY = 100;

        private final List<T> items;

        private final Function<T, U> mapper;

        private final int start, end;

        private Chunk(List<T> items, Function<T, U> mapper) {
            this(items, mapper, 0, items.size());
        }

        private Chunk(List<T> items, Function<T, U> mapper, int start, int end) {
            this.items = items;
            this.mapper = mapper;
            this.start = start;
            this.end = end;
        }

        @Override
        protected List<U> compute() {
            List<U> results = new LinkedList<>();
            if (end - start <= GRANULARITY) {
                // compute
                for (T item : items.subList(start, end)) {
                    results.add(mapper.apply(item));
                }
            } else {
                // fork
                int midpoint = (end - start) / 2;
                Chunk<T, U> left = new Chunk<>(items, mapper, start, start + midpoint);
                Chunk<T, U> right = new Chunk<>(items, mapper, start + midpoint, end);

                left.fork();
                right.fork();
                results.addAll(right.join());
                results.addAll(left.join());

            }
            return results;
        }
    }
}
