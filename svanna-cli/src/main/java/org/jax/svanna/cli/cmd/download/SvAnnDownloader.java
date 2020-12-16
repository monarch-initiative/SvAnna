package org.jax.svanna.cli.cmd.download;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;

/**
 * Command to download the files used by SvAnn for analysis.
 */
class SvAnnDownloader {

    private static final Logger LOGGER = LoggerFactory.getLogger(SvAnnDownloader.class);
    private final static String HP_OBO = "hp.obo";
    /* URL of the hp.obo file. */
    private final static String HP_OBO_URL = "https://raw.githubusercontent.com/obophenotype/human-phenotype-ontology/master/hp.obo";
    /* URL of the annotation file phenotype.hpoa */
    private final static String HP_ANNOTATION_URL = "http://compbio.charite.de/jenkins/job/hpo.annotations.current/lastSuccessfulBuild/artifact/current/phenotype.hpoa";
    /* Basename of the phenotype annotation file. */
    private final static String HP_ANNOTATION = "phenotype.hpoa";

    private final static String MIM2GENE_MEDGEN = "mim2gene_medgen";
    private final static String MIM2GENE_MEDGEN_URL = "ftp://ftp.ncbi.nlm.nih.gov/gene/DATA/mim2gene_medgen";
    private final static String GENE_INFO = "Homo_sapiens_gene_info.gz";
    private final static String GENE_INFO_URL = "ftp://ftp.ncbi.nih.gov/gene/DATA/GENE_INFO/Mammalia/Homo_sapiens.gene_info.gz";
    private final static String GENCODE_FILE = "gencode.v35.chr_patch_hapl_scaff.basic.annotation.gtf.gz";
    private final static String GENECODE_URL = "ftp://ftp.ebi.ac.uk/pub/databases/gencode/Gencode_human/release_35/gencode.v35.chr_patch_hapl_scaff.basic.annotation.gtf.gz";

    //private final static String HUGO_URL = "ftp://ftp.ebi.ac.uk/pub/databases/genenames/new/tsv/hgnc_complete_set.txt";

    private SvAnnDownloader() {
        // private no-op
    }

    private static void downloadFileIfNeeded(String webAddress, Path path, boolean overwrite) throws IOException {
        if (path.toFile().isFile() && (!overwrite)) {
            LOGGER.info("Skipping download of `{}` since the file already exists at `{}`", webAddress, path);
            return;
        }
        FileDownloader downloader = new FileDownloader();

        URL url = new URL(webAddress);
        downloader.copyURLToFile(url, path.toAbsolutePath().toFile());
        LOGGER.info("Downloaded `{}`", path);
    }

    /**
     * Download the files unless they are already present.
     *
     * @param downloadDirectory directory to which we will download the files
     * @param overwrite if true, download new version whether or not the file is already present
     */
    public static void download(Path downloadDirectory, boolean overwrite) throws IOException {
        downloadFileIfNeeded(HP_OBO_URL, downloadDirectory.resolve(HP_OBO), overwrite);
        downloadFileIfNeeded(HP_ANNOTATION_URL, downloadDirectory.resolve(HP_ANNOTATION), overwrite);
        downloadFileIfNeeded(GENE_INFO_URL, downloadDirectory.resolve(GENE_INFO), overwrite);
        downloadFileIfNeeded(MIM2GENE_MEDGEN_URL, downloadDirectory.resolve(MIM2GENE_MEDGEN), overwrite);
        downloadFileIfNeeded(GENECODE_URL, downloadDirectory.resolve(GENCODE_FILE), overwrite);
    }

}
