package org.jax.svanna.cli.cmd;

import org.jax.svanna.cli.writer.ResultWriterFactory;
import org.jax.svanna.core.exception.LogUtils;
import org.jax.svanna.core.hpo.PhenotypeDataService;
import org.jax.svanna.core.landscape.AnnotationDataService;
import org.jax.svanna.core.overlap.GeneOverlapper;
import org.jax.svanna.core.overlap.Overlapper;
import org.jax.svanna.core.reference.GeneService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.Callable;

@Configuration
@EnableAutoConfiguration
public abstract class SvAnnaCommand implements Callable<Integer> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SvAnnaCommand.class);

    @CommandLine.Option(names = {"-c", "--config"},
            required = true,
            paramLabel = "svanna-config.yml",
            description = "Path to configuration file generated by the `generate-config` command")
    public Path configFile;

    protected ConfigurableApplicationContext getContext() {
        // bootstrap Spring application context
        LogUtils.logDebug(LOGGER, "Using SvAnna configuration file at `{}`", configFile.toAbsolutePath());
        return new SpringApplicationBuilder(SvAnnaCommand.class)
                .properties(Map.of("spring.config.location", configFile.toString()))
                .run();
    }

    @Bean
    public ResultWriterFactory resultWriterFactory(GeneService geneService, AnnotationDataService annotationDataService, PhenotypeDataService phenotypeDataService) {
        Overlapper overlapper = new GeneOverlapper(geneService.getChromosomeMap());
        return new ResultWriterFactory(overlapper, annotationDataService, phenotypeDataService);
    }
}
