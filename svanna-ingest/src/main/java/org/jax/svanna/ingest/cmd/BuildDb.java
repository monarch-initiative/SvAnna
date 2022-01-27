package org.jax.svanna.ingest.cmd;


import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.IOUtils;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.jax.svanna.core.LogUtils;
import org.jax.svanna.core.hpo.TermPair;
import org.jax.svanna.db.IngestDao;
import org.jax.svanna.db.gene.GeneDiseaseDao;
import org.jax.svanna.db.landscape.*;
import org.jax.svanna.db.phenotype.MicaDao;
import org.jax.svanna.ingest.Main;
import org.jax.svanna.ingest.config.*;
import org.jax.svanna.ingest.hpomap.HpoMapping;
import org.jax.svanna.ingest.hpomap.HpoTissueMapParser;
import org.jax.svanna.ingest.parse.GencodeGeneProcessor;
import org.jax.svanna.ingest.parse.IngestRecordParser;
import org.jax.svanna.ingest.parse.RepetitiveRegionParser;
import org.jax.svanna.ingest.parse.dosage.ClingenGeneCurationParser;
import org.jax.svanna.ingest.parse.dosage.ClingenRegionCurationParser;
import org.jax.svanna.ingest.parse.enhancer.fantom.FantomEnhancerParser;
import org.jax.svanna.ingest.parse.enhancer.vista.VistaEnhancerParser;
import org.jax.svanna.ingest.parse.population.DbsnpVcfParser;
import org.jax.svanna.ingest.parse.population.DgvFileParser;
import org.jax.svanna.ingest.parse.population.GnomadSvVcfParser;
import org.jax.svanna.ingest.parse.population.HgSvc2VcfParser;
import org.jax.svanna.ingest.parse.tad.McArthur2021TadBoundariesParser;
import org.jax.svanna.ingest.similarity.IcMicaCalculator;
import org.jax.svanna.model.HpoDiseaseSummary;
import org.jax.svanna.model.landscape.dosage.DosageRegion;
import org.jax.svanna.model.landscape.enhancer.Enhancer;
import org.jax.svanna.model.landscape.tad.TadBoundary;
import org.jax.svanna.model.landscape.variant.PopulationVariant;
import org.monarchinitiative.phenol.annotations.assoc.HpoAssociationLoader;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoAssociationData;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.annotations.io.hpo.DiseaseDatabase;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseAnnotationLoader;
import org.monarchinitiative.phenol.base.PhenolRuntimeException;
import org.monarchinitiative.phenol.io.OntologyLoader;
import org.monarchinitiative.phenol.ontology.data.Ontology;
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
import xyz.ielis.silent.genes.gencode.model.GencodeGene;
import xyz.ielis.silent.genes.io.GeneParser;
import xyz.ielis.silent.genes.io.GeneParserFactory;
import xyz.ielis.silent.genes.io.SerializationFormat;
import xyz.ielis.silent.genes.model.Gene;
import xyz.ielis.silent.genes.model.GeneIdentifier;
import xyz.ielis.silent.genes.model.Located;

import javax.sql.DataSource;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@CommandLine.Command(name = "build-db",
        aliases = "B",
        header = "ingest the annotation files into H2 database",
        mixinStandardHelpOptions = true,
        version = Main.VERSION,
        usageHelpWidth = Main.WIDTH,
        footer = Main.FOOTER)
@EnableAutoConfiguration
@EnableConfigurationProperties(value = {
        IngestProperties.class,
        EnhancerProperties.class,
        VariantProperties.class,
        PhenotypeProperties.class,
        TadProperties.class,
        GeneDosageProperties.class,
        GeneProperties.class
})
public class BuildDb implements Callable<Integer> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BuildDb.class);

    private static final Set<DiseaseDatabase> DISEASE_DATABASES = Set.of(DiseaseDatabase.DECIPHER,
            DiseaseDatabase.OMIM,
            DiseaseDatabase.ORPHANET);
    private static final NumberFormat NF = NumberFormat.getNumberInstance();

    static {
        NF.setMaximumFractionDigits(2);
    }

    private static final Pattern NCBI_GENE_PATTERN = Pattern.compile("NCBIGene:(?<value>\\d+)");
    private static final Pattern HGNC_GENE_PATTERN = Pattern.compile("HGNC:(?<value>\\d+)");

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

    public static DataSource initializeDataSource(Path dbPath) {
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
        MigrateResult migrate = flyway.migrate();
        return migrate.migrationsExecuted;
    }

    private static PhenotypeData downloadPhenotypeFiles(PhenotypeProperties properties,
                                                        DataSource dataSource,
                                                        Path buildDir,
                                                        Path tmpDir,
                                                        List<? extends GencodeGene> genes,
                                                        Map<Integer, Integer> ncbiGeneToHgnc) throws IOException {
        // JSON ontology belongs to the buildDir
        URL hpoJsonUrl = new URL(properties.hpoJsonUrl());
        Path hpoOboPath = downloadUrl(hpoJsonUrl, buildDir);

        // other files are temporary
        // HPOA
        URL hpoAnnotationsUrl = new URL(properties.hpoAnnotationsUrl());
        Path hpoAnnotationsPath = downloadUrl(hpoAnnotationsUrl, tmpDir);
        // mim2geneMedgen
        URL mim2geneMedgenUrl = new URL(properties.mim2geneMedgenUrl());
        Path mim2geneMedgenPath = downloadUrl(mim2geneMedgenUrl, tmpDir);
        // geneInfoPath
        URL geneInfoPathUrl = new URL(properties.geneInfoUrl());
        Path geneInfoPath = downloadUrl(geneInfoPathUrl, tmpDir);
        // Download is done

        GeneDiseaseDao geneDiseaseDao = new GeneDiseaseDao(dataSource);

        // Ingest geneIdentifiers
        int updatedGeneIdentifiers = insertGeneIdentifiers(genes, geneDiseaseDao, ncbiGeneToHgnc);
        LOGGER.info("Ingest of gene identifiers updated {} rows", NF.format(updatedGeneIdentifiers));


        // Read phenotype data
        LOGGER.debug("Reading HPO file from {}", hpoOboPath);
        Ontology hpo = OntologyLoader.loadOntology(hpoOboPath.toFile());

        LOGGER.debug("Parsing HPO disease associations at {}", hpoAnnotationsPath);
        LOGGER.debug("Parsing gene info file at {}", geneInfoPath.toAbsolutePath());
        LOGGER.debug("Parsing MIM to gene medgen file at {}", mim2geneMedgenPath.toAbsolutePath());
        HpoAssociationData hpoAssociationData = HpoAssociationLoader.loadHpoAssociationData(hpo, geneInfoPath, mim2geneMedgenPath, null, hpoAnnotationsPath, DISEASE_DATABASES);
        HpoDiseases hpoDiseases = HpoDiseaseAnnotationLoader.loadHpoDiseases(hpoAnnotationsPath, hpo, DISEASE_DATABASES);
        Map<TermId, HpoDisease> diseaseMap = hpoDiseases.diseaseById();

        // Ingest geneToDisease
        int updatedGeneToDisease = ingestGeneToDiseaseMap(hpoAssociationData, ncbiGeneToHgnc, diseaseMap, geneDiseaseDao);
        LOGGER.info("Ingest of gene to disease associations updated {} rows", NF.format(updatedGeneToDisease));

        // Ingest disease to phenotypes
        int updatedDiseaseToPhenotypes = ingestDiseaseToPhenotypes(geneDiseaseDao, diseaseMap);
        LOGGER.info("Ingest of disease to phenotypes updated {} rows", NF.format(updatedDiseaseToPhenotypes));

        // Return the PhenotypeData so that we don't have to re-read the files
        return new PhenotypeData(hpo, hpoDiseases, hpoAssociationData);
    }

    private static int insertGeneIdentifiers(List<? extends GencodeGene> genes,
                                             GeneDiseaseDao geneDiseaseDao,
                                             Map<Integer, Integer> ncbiGeneToHgnc) {
        Map<Integer, Integer> hgncToNcbiGene = ncbiGeneToHgnc.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getValue, Map.Entry::getKey));

        List<GeneIdentifier> geneIdentifiers = new ArrayList<>(genes.size());
        for (GencodeGene gene : genes) {
            Optional<String> hgncIdOpt = gene.id().hgncId();
            Optional<String> ncbiGeneOpt = gene.id().ncbiGeneId();

            String hgncId = null, ncbiGeneId = null;

            if (hgncIdOpt.isPresent()) {
                // We have the ID, this was easy
                hgncId = hgncIdOpt.get();
            } else {
                // Let's try to get the ID from the corresponding NCBIGene id
                if (ncbiGeneOpt.isPresent()) {
                    Matcher matcher = NCBI_GENE_PATTERN.matcher(ncbiGeneOpt.get());
                    if (matcher.matches()) {
                        int ncbiGeneInt = Integer.parseInt(matcher.group("value"));
                        Integer hgncIdInt = ncbiGeneToHgnc.get(ncbiGeneInt);
                        if (hgncIdInt != null)
                            hgncId = "HGNC:" + hgncIdInt;
                    }
                }
            }

            if (ncbiGeneOpt.isPresent()) {
                // We have the ID, this was easy
                ncbiGeneId = ncbiGeneOpt.get();
            } else {
                // Let's try to get the ID from corresponding HGNC id
                if (hgncIdOpt.isPresent()) {
                    Matcher matcher = HGNC_GENE_PATTERN.matcher(hgncIdOpt.get());
                    if (matcher.matches()) {
                        int hgncIdInt = Integer.parseInt(matcher.group("value"));
                        Integer ncbiGeneInt = hgncToNcbiGene.get(hgncIdInt);
                        if (ncbiGeneInt != null)
                            ncbiGeneId = "NCBIGene:" + ncbiGeneInt;
                    }
                }
            }
            geneIdentifiers.add(GeneIdentifier.of(gene.accession(), gene.symbol(), hgncId, ncbiGeneId));
        }

        return geneDiseaseDao.insertGeneIdentifiers(geneIdentifiers);
    }

    private static int ingestGeneToDiseaseMap(HpoAssociationData hpoAssociationData,
                                              Map<Integer, Integer> ncbiGeneToHgnc,
                                              Map<TermId, HpoDisease> diseaseMap,
                                              GeneDiseaseDao geneDiseaseDao) {

        Map<Integer, List<HpoDiseaseSummary>> geneToDisease = new HashMap<>();

        // extract relevant bits and pieces for diseases, and map NCBIGene to HGNC
        Map<TermId, Collection<TermId>> geneToDiseaseIdMap = hpoAssociationData.geneToDiseases();

        for (TermId ncbiGeneTermId : geneToDiseaseIdMap.keySet()) {
            Matcher matcher = NCBI_GENE_PATTERN.matcher(ncbiGeneTermId.getValue());
            if (matcher.matches()) {
                int ncbiGeneId = Integer.parseInt(matcher.group("value"));
                Integer hgncId = ncbiGeneToHgnc.get(ncbiGeneId);
                if (hgncId != null) {
                    for (TermId diseaseId : geneToDiseaseIdMap.get(ncbiGeneTermId)) {
                        HpoDisease hpoDisease = diseaseMap.get(diseaseId);
                        if (hpoDisease != null) {
                            geneToDisease.computeIfAbsent(hgncId, k -> new LinkedList<>())
                                    .add(HpoDiseaseSummary.of(diseaseId.getValue(), hpoDisease.getDiseaseName()));
                        }
                    }
                }

            }
        }

        return geneDiseaseDao.insertGeneToDisease(geneToDisease);
    }

    private static int ingestDiseaseToPhenotypes(GeneDiseaseDao geneDiseaseDao,
                                                 Map<TermId, HpoDisease> diseaseMap) {

        int updated = 0;
        for (Map.Entry<TermId, HpoDisease> entry : diseaseMap.entrySet()) {
            TermId diseaseId = entry.getKey();
            updated += geneDiseaseDao.insertDiseaseToPhenotypes(diseaseId.getValue(), entry.getValue().getPhenotypicAbnormalityTermIdList());
        }
        return updated;
    }

    private static List<? extends GencodeGene> downloadAndPreprocessGenes(GeneProperties properties,
                                                                          GenomicAssembly assembly,
                                                                          Path buildDir,
                                                                          Path tmpDir) throws IOException {
        // download Gencode GTF
        URL url = new URL(properties.gencodeGtfUrl());
        Path localGencodeGtfPath = downloadUrl(url, tmpDir);

        // Load the Gencode GTF into the "silent gene" format
        LOGGER.info("Reading Gencode GTF file at {}", localGencodeGtfPath.toAbsolutePath());
        GencodeGeneProcessor gencodeGeneProcessor = new GencodeGeneProcessor(localGencodeGtfPath, assembly);
        List<? extends GencodeGene> genes = gencodeGeneProcessor.process();
        LOGGER.info("Read {} genes", NF.format(genes.size()));

        // dump the transformed genes to compressed JSON file in the build directory
        GeneParserFactory parserFactory = GeneParserFactory.of(assembly);
        GeneParser jsonParser = parserFactory.forFormat(SerializationFormat.JSON);
        Path destination = buildDir.resolve("gencode.v38.genes.json.gz");
        LOGGER.info("Serializing the genes to {}", destination.toAbsolutePath());
        try (OutputStream os = new BufferedOutputStream(new GzipCompressorOutputStream(Files.newOutputStream(destination)))) {
            jsonParser.write(genes, os);
        }

        return genes;
    }

    private static Path downloadLiftoverChain(IngestProperties properties, Path tmpDir) throws IOException {
        URL chainUrl = new URL(properties.hg19toHg38ChainUrl()); // download hg19 to hg38 liftover chain
        return downloadUrl(chainUrl, tmpDir);
    }

    private static void ingestEnhancers(EnhancerProperties properties,
                                        GenomicAssembly assembly,
                                        DataSource dataSource) throws IOException {
        Map<TermId, HpoMapping> uberonToHpoMap;
        try (InputStream is = BuildDb.class.getResourceAsStream("/uberon_tissue_to_hpo_top_level.csv")) {
            HpoTissueMapParser hpoTissueMapParser = new HpoTissueMapParser(is);
            uberonToHpoMap = hpoTissueMapParser.getOtherToHpoMap();
        }

        IngestRecordParser<? extends Enhancer> vistaParser = new VistaEnhancerParser(assembly, Path.of(properties.vista()), uberonToHpoMap);
        IngestDao<Enhancer> ingestDao = new EnhancerAnnotationDao(dataSource, assembly);
        int updated = ingestTrack(vistaParser, ingestDao);
        LOGGER.info("Ingest of Vista enhancers affected {} rows", NF.format(updated));

        IngestRecordParser<? extends Enhancer> fantomParser = new FantomEnhancerParser(assembly, Path.of(properties.fantomMatrix()), Path.of(properties.fantomSample()), uberonToHpoMap);
        updated = ingestTrack(fantomParser, ingestDao);
        LOGGER.info("Ingest of FANTOM5 enhancers affected {} rows", NF.format(updated));
    }

    private static void ingestPopulationVariants(VariantProperties properties, GenomicAssembly assembly, DataSource dataSource, Path tmpDir,
                                                 Path hg19Hg38chainPath) throws IOException {
        // DGV
        URL dgvUrl = new URL(properties.dgvUrl());
        Path dgvLocalPath = downloadUrl(dgvUrl, tmpDir);
        LOGGER.info("Ingesting DGV data");
        DbPopulationVariantDao ingestDao = new DbPopulationVariantDao(dataSource, assembly);
        int dgvUpdated = ingestTrack(new DgvFileParser(assembly, dgvLocalPath), ingestDao);
        LOGGER.info("DGV ingest updated {} rows", NF.format(dgvUpdated));

        // GNOMAD SV
        URL gnomadUrl = new URL(properties.gnomadSvVcfUrl());
        Path gnomadLocalPath = downloadUrl(gnomadUrl, tmpDir);
        LOGGER.info("Ingesting GnomadSV data");
        IngestRecordParser<PopulationVariant> gnomadParser = new GnomadSvVcfParser(assembly, gnomadLocalPath, hg19Hg38chainPath);
        int gnomadUpdated = ingestTrack(gnomadParser, ingestDao);
        LOGGER.info("GnomadSV ingest updated {} rows", NF.format(gnomadUpdated));

        // HGSVC2
        URL hgsvc2 = new URL(properties.hgsvc2VcfUrl());
        Path hgsvc2Path = downloadUrl(hgsvc2, tmpDir);
        LOGGER.info("Ingesting HGSVC2 data");
        IngestRecordParser<PopulationVariant> hgSvc2VcfParser = new HgSvc2VcfParser(assembly, hgsvc2Path);
        int hgsvc2Updated = ingestTrack(hgSvc2VcfParser, ingestDao);
        LOGGER.info("HGSVC2 ingest updated {} rows", NF.format(hgsvc2Updated));

        // dbSNP
        URL dbsnp = new URL(properties.dbsnpVcfUrl());
        Path dbSnpPath = downloadUrl(dbsnp, tmpDir);
        LOGGER.info("Ingesting dbSNP data");
        IngestRecordParser<PopulationVariant> dbsnpVcfParser = new DbsnpVcfParser(assembly, dbSnpPath);
        int dbsnpUpdated = ingestTrack(dbsnpVcfParser, ingestDao);
        LOGGER.info("dbSNP ingest updated {} rows", NF.format(dbsnpUpdated));
    }

    private static void ingestRepeats(IngestProperties properties, GenomicAssembly assembly, DataSource dataSource, Path tmpDir) throws IOException {
        URL repeatsUrl = new URL(properties.getRepetitiveRegionsUrl());
        Path repeatsLocalPath = downloadUrl(repeatsUrl, tmpDir);

        LOGGER.info("Ingesting repeats data");
        int repetitiveUpdated = ingestTrack(new RepetitiveRegionParser(assembly, repeatsLocalPath), new RepetitiveRegionDao(dataSource, assembly));
        LOGGER.info("Repeats ingest updated {} rows", NF.format(repetitiveUpdated));
    }

    private static void ingestTads(TadProperties properties, GenomicAssembly assembly, DataSource dataSource, Path tmpDir, Path chain) throws IOException {
        // McArthur2021 supplement
        URL mcArthurSupplement = new URL(properties.mcArthur2021Supplement());
        Path localPath = downloadUrl(mcArthurSupplement, tmpDir);

        try (ZipFile zipFile = new ZipFile(localPath.toFile())) {
            // this is the single file from the entire ZIP that we're interested in
            String entryName = "emcarthur-TAD-stability-heritability-184f51a/data/boundariesByStability/100kbBookendBoundaries_mainText/100kbBookendBoundaries_byStability.bed";
            ZipArchiveEntry entry = zipFile.getEntry(entryName);
            InputStream is = zipFile.getInputStream(entry);
            IngestRecordParser<TadBoundary> parser = new McArthur2021TadBoundariesParser(assembly, is, chain);
            IngestDao<TadBoundary> dao = new TadBoundaryDao(dataSource, assembly);
            int updated = ingestTrack(parser, dao);
            LOGGER.info("Ingest of TAD boundaries affected {} rows", NF.format(updated));
        }
    }

    private static void precomputeIcMica(DataSource dataSource,
                                         Ontology hpo,
                                         Map<TermId, HpoDisease> diseaseMap) {
        Map<TermPair, Double> similarityMap = IcMicaCalculator.precomputeIcMicaValues(hpo, diseaseMap);

        MicaDao dao = new MicaDao(dataSource);
        similarityMap.forEach(dao::insertItem);
    }

    private static Map<TermId, GenomicRegion> readGeneRegions(List<? extends GencodeGene> genes, GenomicAssembly assembly) {
        Map<TermId, GenomicRegion> regionsByHgncId = new HashMap<>(genes.size());
        for (Gene gene : genes) {
            Optional<String> hgncIdOptional = gene.id().hgncId();
            if (hgncIdOptional.isEmpty())
                continue;

            try {
                TermId hgncId = TermId.of(hgncIdOptional.get());
                regionsByHgncId.put(hgncId, gene.location());
            } catch (PhenolRuntimeException e) {
                LOGGER.warn("Invalid HGNC id `{}` in gene {}", hgncIdOptional.get(), gene);
            }
        }

        return regionsByHgncId;
    }

    private static void ingestGeneDosage(GeneDosageProperties properties,
                                         GenomicAssembly assembly,
                                         DataSource dataSource,
                                         Path tmpDir,
                                         Map<TermId, ? extends GenomicRegion> geneRegions,
                                         Map<Integer, Integer> ncbiGeneToHgnc) throws IOException {
        ClingenDosageElementDao clingenDosageElementDao = new ClingenDosageElementDao(dataSource, assembly);

        // dosage sensitive genes
        URL geneUrl = new URL(properties.getGeneUrl());
        Path geneLocalPath = downloadUrl(geneUrl, tmpDir);
        ClingenGeneCurationParser geneParser = new ClingenGeneCurationParser(geneLocalPath, assembly, geneRegions, ncbiGeneToHgnc);
        try (Stream<? extends DosageRegion> geneStream = geneParser.parse()) {
            int geneUpdated = geneStream
                    .mapToInt(clingenDosageElementDao::insertItem)
                    .sum();
            LOGGER.info("Ingest of dosage sensitive genes affected {} rows", NF.format(geneUpdated));
        }

        // dosage sensitive regions
        URL regionUrl = new URL(properties.getRegionUrl());
        Path regionLocalPath = downloadUrl(regionUrl, tmpDir);
        ClingenRegionCurationParser regionParser = new ClingenRegionCurationParser(regionLocalPath, assembly);
        try (Stream<? extends DosageRegion> regionStream = regionParser.parse()) {
            int regionsUpdated = regionStream
                    .mapToInt(clingenDosageElementDao::insertItem)
                    .sum();
            LOGGER.info("Ingest of dosage sensitive regions affected {} rows", NF.format(regionsUpdated));
        }
    }

    private static Map<Integer, Integer> parseNcbiToHgncTable(String ncbiGeneToHgnc) throws IOException {
        Path tablePath = Path.of(ncbiGeneToHgnc);
        if (Files.notExists(tablePath)) {
            throw new IOException("Table for mapping NCBIGene to HGNC does not exist at " + tablePath.toAbsolutePath());
        }

        Map<Integer, Integer> results = new HashMap<>();
        try (BufferedReader reader = openForReading(tablePath);
             CSVParser parser = CSVFormat.TDF.withFirstRecordAsHeader().parse(reader)) {
            Pattern hgncPattern = Pattern.compile("HGNC:(?<payload>\\d+)");
            // HGNC ID	NCBI gene ID	Approved symbol
            // HGNC:13666	8086	AAAS
            for (CSVRecord record : parser) {
                // parse NCBIGene. Should be a number, but may be missing.
                String ncbiGene = record.get("NCBI gene ID");
                if (ncbiGene.isBlank())
                    // missing NCBI gene ID for this gene
                    continue;

                int ncbiGeneId;
                try {
                    ncbiGeneId = Integer.parseInt(ncbiGene);
                } catch (NumberFormatException e) {
                    LOGGER.warn("Skipping non-numeric NCBIGene id `{}` on line #{}: `{}`", ncbiGene, record.getRecordNumber(), record);
                    continue;
                }

                // parse HGNC id
                Matcher hgncMatcher = hgncPattern.matcher(record.get("HGNC ID"));
                if (!hgncMatcher.matches()) {
                    LOGGER.warn("Skipping HGNC id `{}` on line #{}: `{}`", record.get("HGNC ID"), record.getRecordNumber(), record);
                    continue;
                }
                Integer hgncId = Integer.parseInt(hgncMatcher.group("payload"));

                // store the results
                results.put(ncbiGeneId, hgncId);
            }
        }
        return results;
    }

    private static BufferedReader openForReading(Path tablePath) throws IOException {
        return (tablePath.toFile().getName().endsWith(".gz"))
                ? new BufferedReader(new InputStreamReader(new GzipCompressorInputStream(Files.newInputStream(tablePath))))
                : Files.newBufferedReader(tablePath);

    }

    private static <T extends Located> int ingestTrack(IngestRecordParser<? extends T> ingestRecordParser, IngestDao<? super T> ingestDao) throws IOException {
        return ingestRecordParser.parse()
                .mapToInt(ingestDao::insertItem)
                .sum();
    }

    private static Path downloadUrl(URL url, Path downloadDir) throws IOException {
        String file = url.getFile();
        String urlFileName = file.substring(file.lastIndexOf('/') + 1);
        Path localPath = downloadDir.resolve(urlFileName);
        LOGGER.info("Downloading data from `{}` to {}", url, localPath);
        downloadFile(url, localPath.toFile());
        return localPath;
    }

    private static void downloadFile(URL source, File target) throws IOException {
        if (target.isFile()) return;
        File parent = target.getParentFile();
        if (!parent.isDirectory() && !parent.mkdirs())
            throw new IOException("Unable to create parent directory " + parent.getAbsolutePath() + " for downloading " + target.getAbsolutePath());

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

    @Override
    public Integer call() throws Exception {
        try (ConfigurableApplicationContext context = getContext()) {
            IngestProperties properties = context.getBean(IngestProperties.class);

            GenomicAssembly assembly = GenomicAssemblies.GRCh38p13();
            if (buildDir.toFile().exists()) {
                if (!buildDir.toFile().isDirectory()) {
                    LOGGER.error("Not a directory: {}", buildDir);
                    return 1;
                }
            } else {
                if (!buildDir.toFile().mkdirs()) {
                    LOGGER.error("Unable to create build directory");
                    return 1;
                }
            }

            Path dbPath = buildDir.resolve("svanna_db.mv.db");
            if (dbPath.toFile().isFile()) {
                if (overwrite) {
                    LOGGER.info("Removing the old database");
                    Files.delete(dbPath);
                } else {
                    LOGGER.info("Abort since the database already exists at {}. ", dbPath);
                    return 0;
                }
            }

            LOGGER.info("Creating database at {}", dbPath);
            DataSource dataSource = initializeDataSource(dbPath);

            Path tmpDir = buildDir.resolve("build");
            List<? extends GencodeGene> genes = downloadAndPreprocessGenes(properties.getGenes(), assembly, buildDir, tmpDir);
            Map<Integer, Integer> ncbiGeneToHgncId = parseNcbiToHgncTable(properties.ncbiGeneToHgnc());

            PhenotypeData phenotypeData = downloadPhenotypeFiles(properties.phenotype(),
                    dataSource,
                    buildDir,
                    tmpDir,
                    genes,
                    ncbiGeneToHgncId);

            ingestEnhancers(properties.enhancers(), assembly, dataSource);

            Path hg19ToHg38Chain = downloadLiftoverChain(properties, tmpDir);
            ingestPopulationVariants(properties.variants(), assembly, dataSource, tmpDir, hg19ToHg38Chain);
            ingestRepeats(properties, assembly, dataSource, tmpDir);
            ingestTads(properties.tad(), assembly, dataSource, tmpDir, hg19ToHg38Chain);

            precomputeIcMica(dataSource,
                    phenotypeData.hpo(),
                    phenotypeData.hpoDiseases().diseaseById());
            Map<TermId, GenomicRegion> geneMap = readGeneRegions(genes, assembly);
            ingestGeneDosage(properties.getDosage(), assembly, dataSource, tmpDir, geneMap, ncbiGeneToHgncId);

            LOGGER.info("The ingest is complete");
            return 0;
        }
    }

    protected ConfigurableApplicationContext getContext() {
        // bootstrap Spring application context
        return new SpringApplicationBuilder(BuildDb.class)
                .properties(Map.of("spring.config.location", configFile.toString()))
                .run();
    }

    private static class PhenotypeData {
        private final Ontology hpo;
        private final HpoDiseases hpoDiseases;
        private final HpoAssociationData hpoAssociationData;

        private PhenotypeData(Ontology hpo, HpoDiseases hpoDiseases, HpoAssociationData hpoAssociationData) {
            this.hpo = hpo;
            this.hpoDiseases = hpoDiseases;
            this.hpoAssociationData = hpoAssociationData;
        }

        public Ontology hpo() {
            return hpo;
        }

        public HpoDiseases hpoDiseases() {
            return hpoDiseases;
        }

        public HpoAssociationData hpoAssociationData() {
            return hpoAssociationData;
        }

    }
}
