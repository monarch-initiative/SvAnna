package org.jax.svanna.cli.cmd.benchmark;

import org.jax.svanna.cli.cmd.ProgressReporter;
import org.jax.svanna.cli.cmd.TaskUtils;
import org.jax.svanna.core.exception.LogUtils;
import org.jax.svanna.core.filter.PopulationFrequencyAndRepetitiveRegionFilter;
import org.jax.svanna.core.priority.SvPrioritizer;
import org.jax.svanna.core.priority.SvPrioritizerType;
import org.jax.svanna.core.priority.SvPriority;
import org.jax.svanna.core.priority.SvPriorityFactory;
import org.jax.svanna.core.reference.SvannaVariant;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.svart.Variant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PrioritizationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(PrioritizationRunner.class);
    private static final int TICK = 5_000;

    private final PopulationFrequencyAndRepetitiveRegionFilter filter;

    private final SvPriorityFactory priorityFactory;

    private final int nThreads;

    public PrioritizationRunner(PopulationFrequencyAndRepetitiveRegionFilter filter,
                                SvPriorityFactory priorityFactory,
                                int nThreads) {
        this.filter = filter;
        this.priorityFactory = priorityFactory;
        this.nThreads = nThreads;
    }

    public List<VariantPriority> prioritize(Set<TermId> termIds, List<SvannaVariant> variants) {
        SvPrioritizer<Variant, SvPriority> prioritizer = priorityFactory.getPrioritizer(SvPrioritizerType.ADDITIVE, termIds);

        LogUtils.logInfo(LOGGER, "Filtering variants");
        List<SvannaVariant> filtered = filter.filter(variants).stream()
                .filter(SvannaVariant::passedFilters)
                .collect(Collectors.toList());
        LogUtils.logInfo(LOGGER, "Removed {} variants that failed the filtering", variants.size() - filtered.size());

        LogUtils.logInfo(LOGGER, "Prioritizing variants");
        ProgressReporter progressReporter = new ProgressReporter(TICK);
        Stream<VariantPriority> annotationStream = filtered.parallelStream()
                .onClose(progressReporter.summarize())
                .peek(progressReporter::logItem)
                .map(v -> new VariantPriority(v, prioritizer.prioritize(v)));
        try {
            return TaskUtils.executeBlocking(() -> annotationStream.collect(Collectors.toList()), nThreads);
        } catch (ExecutionException | InterruptedException e) {
            LogUtils.logWarn(LOGGER, "Error: {}", e.getMessage());
            return List.of();
        }
    }

}
