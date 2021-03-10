package org.jax.svanna.ingest.cmd;


import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.io.IOUtils;
import org.flywaydb.core.Flyway;
import org.jax.svanna.core.exception.LogUtils;
import org.jax.svanna.core.hpo.TermPair;
import org.jax.svanna.core.landscape.Enhancer;
import org.jax.svanna.core.landscape.PopulationVariant;
import org.jax.svanna.core.landscape.TadBoundary;
import org.jax.svanna.db.IngestDao;
import org.jax.svanna.db.landscape.DbPopulationVariantDao;
import org.jax.svanna.db.landscape.EnhancerAnnotationDao;
import org.jax.svanna.db.landscape.RepetitiveRegionDao;
import org.jax.svanna.db.landscape.TadBoundaryDao;
import org.jax.svanna.ingest.Main;
import org.jax.svanna.ingest.hpomap.HpoMapping;
import org.jax.svanna.ingest.hpomap.HpoTissueMapParser;
import org.jax.svanna.ingest.parse.IngestRecordParser;
import org.jax.svanna.ingest.parse.RepetitiveRegionParser;
import org.jax.svanna.ingest.parse.enhancer.fantom.FantomEnhancerParser;
import org.jax.svanna.ingest.parse.enhancer.vista.VistaEnhancerParser;
import org.jax.svanna.ingest.parse.population.DgvFileParser;
import org.jax.svanna.ingest.parse.population.GnomadSvVcfParser;
import org.jax.svanna.ingest.parse.population.HgSvc2VcfParser;
import org.jax.svanna.ingest.parse.tad.McArthur2021TadBoundariesParser;
import org.jax.svanna.ingest.similarity.ResnikSimilarity;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.svart.GenomicAssemblies;
import org.monarchinitiative.svart.GenomicAssembly;
import org.monarchinitiative.svart.GenomicRegion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import picocli.CommandLine;

import javax.sql.DataSource;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "build-db",
        aliases = "B",
        header = "ingest the annotation files into H2 database",
        mixinStandardHelpOptions = true,
        version = Main.VERSION,
        usageHelpWidth = Main.WIDTH,
        footer = Main.FOOTER)
@EnableAutoConfiguration
@EnableConfigurationProperties(value = {
        IngestDbProperties.class,
        IngestDbProperties.EnhancerProperties.class,
        IngestDbProperties.VariantProperties.class,
        IngestDbProperties.PhenotypeProperties.class,
        IngestDbProperties.TadProperties.class
})
public class BuildDb implements Callable<Integer> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BuildDb.class);

    private static final String LOCATIONS = "classpath:db/migration";

    @CommandLine.Option(names = {"-o", "--overwrite"},
            description = "remove existing database (default: ${DEFAULT-VALUE})")
    public boolean overwrite = true;

    @CommandLine.Parameters(index = "0",
            paramLabel = "svanna-ingest-config.yml",
            description = "Path to configuration file generated by the `generate-config` command")
    public Path configFile;

    @CommandLine.Parameters(index = "1",
            description = "path to directory where the database will be built (default: ${DEFAULT-VALUE})")
    public Path buildDir = Path.of("data");

    @Override
    public Integer call() throws Exception {
        try (ConfigurableApplicationContext context = getContext()) {
            IngestDbProperties properties = context.getBean(IngestDbProperties.class);

            GenomicAssembly assembly = GenomicAssemblies.GRCh38p13();
            if (buildDir.toFile().exists()) {
                if (!buildDir.toFile().isDirectory()) {
                    if (LOGGER.isErrorEnabled()) LOGGER.error("Not a directory: `{}`", buildDir);
                    return 1;
                }
            } else {
                if (!buildDir.toFile().mkdirs()) {
                    if (LOGGER.isErrorEnabled()) LOGGER.error("Unable to create build directory");
                    return 1;
                }
            }

            Path dbPath = buildDir.resolve("svanna_db.mv.db");
            if (dbPath.toFile().isFile()) {
                if (overwrite) {
                    if (LOGGER.isInfoEnabled()) LOGGER.info("Removing the old database");
                    Files.delete(dbPath);
                } else {
                    LOGGER.info("Abort since the database already exists at `{}`. ", dbPath);
                    return 0;
                }
            }

            LOGGER.info("Creating database at `{}`", dbPath);
            DataSource dataSource = initializeDataSource(dbPath);

            downloadPhenotypeFiles(buildDir, properties);
            ingestEnhancers(properties, assembly, dataSource);

            Path tmpDir = buildDir.resolve("build");
            URL chainUrl = new URL(properties.hg19toHg38ChainUrl()); // download hg19 to hg38 liftover chain
            Path hg19ToHg38Chain = downloadUrl(chainUrl, tmpDir);
            ingestPopulationVariants(tmpDir, properties, assembly, dataSource, hg19ToHg38Chain);
            ingestRepeats(tmpDir, properties, assembly, dataSource);
            ingestTads(tmpDir, properties, assembly, dataSource, hg19ToHg38Chain);
            precomputeResnikSimilarity(buildDir);

            return 0;
        }
    }

    private static DataSource initializeDataSource(Path dbPath) {
        DataSource dataSource = makeDataSourceAt(dbPath);

        int migrations = applyMigrations(dataSource);
        LOGGER.info("Applied {} migration(s)", migrations);
        return dataSource;
    }

    private static DataSource makeDataSourceAt(Path databasePath) {
        String absolutePath = databasePath.toFile().getAbsolutePath();
        if (absolutePath.endsWith(".mv.db"))
            absolutePath = absolutePath.substring(0, absolutePath.length() - 6);

        String jdbcUrl = String.format("jdbc:h2:file:%s", absolutePath);
        HikariConfig config = new HikariConfig();
        config.setUsername("sa");
        config.setPassword("sa");
        config.setDriverClassName("org.h2.Driver");
        config.setJdbcUrl(jdbcUrl);

        return new HikariDataSource(config);
    }

    private static int applyMigrations(DataSource dataSource) {
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations(LOCATIONS)
                .load();
        return flyway.migrate();
    }

    private static void ingestEnhancers(IngestDbProperties properties, GenomicAssembly assembly, DataSource dataSource) throws IOException {
        Map<TermId, HpoMapping> uberonToHpoMap;
        try (InputStream is = BuildDb.class.getResourceAsStream("/hpo_enhancer_map.csv")) {
            HpoTissueMapParser hpoTissueMapParser = new HpoTissueMapParser(is);
            uberonToHpoMap = hpoTissueMapParser.getOtherToHpoMap();
        }

        IngestRecordParser<? extends Enhancer> vistaParser = new VistaEnhancerParser(assembly, Path.of(properties.enhancers().vista()), uberonToHpoMap);
        IngestDao<Enhancer> ingestDao = new EnhancerAnnotationDao(dataSource, assembly);
        int updated = ingestTrack(vistaParser, ingestDao);
        LOGGER.info("Ingest of Vista enhancers affected {} rows", updated);

        IngestRecordParser<? extends Enhancer> fantomParser = new FantomEnhancerParser(assembly, Path.of(properties.enhancers().fantomMatrix()), Path.of(properties.enhancers().fantomSample()), uberonToHpoMap);
        updated = ingestTrack(fantomParser, ingestDao);
        LOGGER.info("Ingest of FANTOM5 enhancers affected {} rows", updated);
    }

    private static void ingestPopulationVariants(Path tmpDir, IngestDbProperties properties, GenomicAssembly assembly, DataSource dataSource, Path hg19Hg38chainPath) throws IOException {
        // DGV
        URL dgvUrl = new URL(properties.variants().dgvUrl());
        Path dgvLocalPath = downloadUrl(dgvUrl, tmpDir);
        LOGGER.info("Ingesting DGV data");
        DbPopulationVariantDao ingestDao = new DbPopulationVariantDao(dataSource, assembly);
        int dgvUpdated = ingestTrack(new DgvFileParser(assembly, dgvLocalPath), ingestDao);
        LOGGER.info("DGV ingest updated {} rows", dgvUpdated);

        // GNOMAD SV
        URL gnomadUrl = new URL(properties.variants().gnomadSvVcfUrl());
        Path gnomadLocalPath = downloadUrl(gnomadUrl, tmpDir);
        LOGGER.info("Ingesting GnomadSV data");
        IngestRecordParser<PopulationVariant> gnomadParser = new GnomadSvVcfParser(assembly, gnomadLocalPath, hg19Hg38chainPath);
        int gnomadUpdated = ingestTrack(gnomadParser, ingestDao);
        LOGGER.info("GnomadSV ingest updated {} rows", gnomadUpdated);

        // HGSVC2
        URL hgsvc2 = new URL(properties.variants().hgsvc2VcfUrl());
        Path hgsvc2Path = downloadUrl(hgsvc2, tmpDir);
        LOGGER.info("Ingesting HGSVC2 data");
        IngestRecordParser<PopulationVariant> hgSvc2VcfParser = new HgSvc2VcfParser(assembly, hgsvc2Path);
        int hgsvc2Updated = ingestTrack(hgSvc2VcfParser, ingestDao);
        LOGGER.info("HGSVC2 ingest updated {} rows", hgsvc2Updated);

    }

    private static void ingestRepeats(Path tmpDir, IngestDbProperties properties, GenomicAssembly assembly, DataSource dataSource) throws IOException {
        URL repeatsUrl = new URL(properties.getRepetitiveRegionsUrl());
        Path repeatsLocalPath = downloadUrl(repeatsUrl, tmpDir);

        LOGGER.info("Ingesting repeats data");
        int repetitiveUpdated = ingestTrack(new RepetitiveRegionParser(assembly, repeatsLocalPath), new RepetitiveRegionDao(dataSource, assembly));
        LOGGER.info("Repeats ingest updated {} rows", repetitiveUpdated);
    }

    private static void ingestTads(Path tmpDir, IngestDbProperties properties, GenomicAssembly assembly, DataSource dataSource, Path chain) throws IOException {
        // McArthur2021 supplement
        URL mcArthurSupplement = new URL(properties.tad().mcArthur2021Supplement());
        Path localPath = downloadUrl(mcArthurSupplement, tmpDir);

        try (ZipFile zipFile = new ZipFile(localPath.toFile())) {
            // this is the single file from the entire ZIP that we're interested in
            String entryName = "emcarthur-TAD-stability-heritability-184f51a/data/boundariesByStability/100kbBookendBoundaries_mainText/100kbBookendBoundaries_byStability.bed";
            ZipArchiveEntry entry = zipFile.getEntry(entryName);
            InputStream is = zipFile.getInputStream(entry);
            IngestRecordParser<TadBoundary> parser = new McArthur2021TadBoundariesParser(assembly, is, chain);
            IngestDao<TadBoundary> dao = new TadBoundaryDao(dataSource, assembly);
            int updated = ingestTrack(parser, dao);
            LOGGER.info("Ingest of TAD boundaries affected {} rows", updated);
        }
    }

    private static void precomputeResnikSimilarity(Path buildDir) {
        Path ontologyPath = buildDir.resolve("hp.obo");
        Path hpoaPath = buildDir.resolve("phenotype.hpoa");
        Map<TermPair, Double> similarityMap = ResnikSimilarity.precomputeResnikSimilarity(ontologyPath, hpoaPath);

        Path resnikSimilarityPath = buildDir.resolve("resnik_similarity.csv.gz");
        LogUtils.logInfo(LOGGER, "Compressing the precomputed similarities into `{}`", resnikSimilarityPath.toAbsolutePath());
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new GzipCompressorOutputStream(new FileOutputStream(resnikSimilarityPath.toFile()))))) {
            CSVPrinter printer = CSVFormat.DEFAULT
                    .withHeader("LEFT", "RIGHT", "SIMILARITY")
                    .print(writer);

            for (Map.Entry<TermPair, Double> entry : similarityMap.entrySet()) {
                TermPair tp = entry.getKey();
                printer.print(tp.left().getValue());
                printer.print(tp.right().getValue());
                printer.print(entry.getValue());
                printer.println();
            }
        } catch (IOException e) {
            LogUtils.logWarn(LOGGER, "Error storing similarities: {}", e.getMessage());
        }
    }

    private static <T extends GenomicRegion> int ingestTrack(IngestRecordParser<? extends T> ingestRecordParser, IngestDao<? super T> ingestDao) throws IOException {
        return ingestRecordParser.parse()
                .mapToInt(ingestDao::insertItem)
                .sum();
    }

    private static Path downloadUrl(URL url, Path downloadDir) throws IOException {
        String file = url.getFile();
        String urlFileName = file.substring(file.lastIndexOf('/') + 1);
        Path localPath = downloadDir.resolve(urlFileName);
        LOGGER.info("Downloading data from `{}` to `{}`", url, localPath);
        downloadFile(url, localPath.toFile());
        return localPath;
    }

    private static void downloadFile(URL source, File target) throws IOException {
        if (target.isFile()) return;
        File parent = target.getParentFile();
        if (!parent.isDirectory() && !parent.mkdirs())
            throw new IOException("Unable to create parent directory `" + parent.getAbsolutePath() + "` for downloading `" + target.getAbsolutePath() + '`');

        URLConnection connection;
        if ("http".equals(source.getProtocol()))
            connection = openHttpConnectionHandlingRedirects(source);
        else
            connection = source.openConnection();

        try (BufferedInputStream is = new BufferedInputStream(connection.getInputStream())) {
            FileOutputStream os = new FileOutputStream(target);
            IOUtils.copyLarge(is, os);
        }
    }

    private static URLConnection openHttpConnectionHandlingRedirects(URL source) throws IOException {
        LogUtils.logDebug(LOGGER, "Opening HTTP connection to `{}`", source);
        HttpURLConnection connection = (HttpURLConnection) source.openConnection();
        int status = connection.getResponseCode();

        if (status != HttpURLConnection.HTTP_OK) {
            LogUtils.logDebug(LOGGER, "Received response `{}`", status);
            if (status == HttpURLConnection.HTTP_MOVED_PERM || status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_SEE_OTHER) {
                String location = connection.getHeaderField("Location");
                LogUtils.logDebug(LOGGER, "Redirecting to `{}`", location);
                return new URL(location).openConnection();
            }
        }
        return connection;
    }

    private static void downloadPhenotypeFiles(Path buildDir, IngestDbProperties properties) throws IOException {
        IngestDbProperties.PhenotypeProperties phenotype = properties.phenotype();
        List<String> fieldsToDownload = List.of(phenotype.hpoOboUrl(), phenotype.hpoAnnotationsUrl(),
                phenotype.mim2geneMedgenUrl(), phenotype.geneInfoUrl(), phenotype.gencodeUrl());
        for (String field : fieldsToDownload) {
            URL url = new URL(field);
            downloadUrl(url, buildDir);
        }
    }

    protected ConfigurableApplicationContext getContext() {
        // bootstrap Spring application context
        return new SpringApplicationBuilder(BuildDb.class)
                .properties(Map.of("spring.config.location", configFile.toString()))
                .run();
    }

}