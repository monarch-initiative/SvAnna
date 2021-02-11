package org.jax.svanna.autoconfigure;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import de.charite.compbio.jannovar.data.JannovarData;
import de.charite.compbio.jannovar.data.JannovarDataSerializer;
import de.charite.compbio.jannovar.data.SerializationException;
import org.jax.svanna.autoconfigure.exception.MissingResourceException;
import org.jax.svanna.autoconfigure.exception.UndefinedResourceException;
import org.jax.svanna.core.annotation.AnnotationDataService;
import org.jax.svanna.core.annotation.PopulationVariantDao;
import org.jax.svanna.core.reference.TranscriptService;
import org.jax.svanna.core.reference.transcripts.JannovarTranscriptService;
import org.jax.svanna.db.annotation.DbAnnotationDataService;
import org.jax.svanna.db.annotation.DbPopulationVariantDao;
import org.jax.svanna.db.annotation.EnhancerAnnotationDao;
import org.jax.svanna.db.annotation.RepetitiveRegionDao;
import org.monarchinitiative.svart.GenomicAssemblies;
import org.monarchinitiative.svart.GenomicAssembly;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
@EnableConfigurationProperties(SvannaProperties.class)
public class SvannaAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "svannaDataDirectory")
    public Path svannaDataDirectory(SvannaProperties properties) throws UndefinedResourceException {
        String dataDir = properties.dataDirectory();
        if (dataDir == null || dataDir.isEmpty()) {
            throw new UndefinedResourceException("Path to Svanna data directory (`--svanna.data-directory`) is not specified");
        }
        Path dataDirPath = Paths.get(dataDir);
        if (!Files.isDirectory(dataDirPath)) {
            throw new UndefinedResourceException(String.format("Path to Svanna data directory '%s' does not point to real directory", dataDirPath));
        }
        return dataDirPath;
    }

    @Bean
    public GenomicAssembly genomicAssembly() {
        return GenomicAssemblies.GRCh38p13();
    }

    @Bean
    public AnnotationDataService annotationDataService(DataSource dataSource, GenomicAssembly genomicAssembly) {
        return new DbAnnotationDataService(new EnhancerAnnotationDao(dataSource, genomicAssembly),
                new RepetitiveRegionDao(dataSource, genomicAssembly));
    }

    @Bean
    public PopulationVariantDao populationVariantDao(DataSource dataSource, GenomicAssembly genomicAssembly) {
        return new DbPopulationVariantDao(dataSource, genomicAssembly);
    }


    @Bean
    public TranscriptService transcriptService(SvannaProperties svannaProperties, GenomicAssembly genomicAssembly) throws SerializationException {
        // TODO - replace by our internal transcript source
        JannovarData jannovarData = new JannovarDataSerializer(svannaProperties.jannovarCachePath()).load();
        return JannovarTranscriptService.of(genomicAssembly, jannovarData);

    }


    @Bean
    public SvannaDataResolver svannaDataResolver(Path svannaDataDirectory) throws MissingResourceException {
        return new SvannaDataResolver(svannaDataDirectory);
    }

    @Bean
    public DataSource svannaDatasource(SvannaDataResolver svannaDataResolver) {
        Path datasourcePath = svannaDataResolver.dataSourcePath();
        String jdbcUrl = String.format("jdbc:h2:file:%s;ACCESS_MODE_DATA=r", datasourcePath.toFile().getAbsolutePath());
        HikariConfig config = new HikariConfig();
        config.setUsername("sa");
        config.setPassword("sa");
        config.setDriverClassName("org.h2.Driver");
        config.setJdbcUrl(jdbcUrl);
        config.setPoolName("svanna-pool");

        return new HikariDataSource(config);
    }
}
