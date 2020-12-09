package org.jax.svann.io;


import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Command to download the files used by SvAnn for analysis.
 */
public class SvAnnDownloader {

    /** Directory to which we will download the files. */
    private final String downloadDirectory;
    /** If true, download new version whether or not the file is already present. */
    private final boolean overwrite;

    private final static String HP_OBO = "hp.obo";
    /** URL of the hp.obo file. */
    private final static String HP_OBO_URL ="https://raw.githubusercontent.com/obophenotype/human-phenotype-ontology/master/hp.obo";
    /** URL of the annotation file phenotype.hpoa. */
    private final static String HP_ANNOTATION_URL ="http://compbio.charite.de/jenkins/job/hpo.annotations.current/lastSuccessfulBuild/artifact/current/phenotype.hpoa";
    /** Basename of the phenotype annotation file. */
    private final static String HP_ANNOTATION ="phenotype.hpoa";

    private final static String MIM2GENE_MEDGEN = "mim2gene_medgen";

    private final static String MIM2GENE_MEDGEN_URL = "ftp://ftp.ncbi.nlm.nih.gov/gene/DATA/mim2gene_medgen";

    private final static String GENE_INFO = "Homo_sapiens_gene_info.gz";

    private final static String GENE_INFO_URL = "ftp://ftp.ncbi.nih.gov/gene/DATA/GENE_INFO/Mammalia/Homo_sapiens.gene_info.gz";

    //private final static String HUGO_URL = "ftp://ftp.ebi.ac.uk/pub/databases/genenames/new/tsv/hgnc_complete_set.txt";

    private final static String GENCODE_FILE = "gencode.v35.chr_patch_hapl_scaff.basic.annotation.gtf.gz";
    private final static String GENECODE_URL = "ftp://ftp.ebi.ac.uk/pub/databases/gencode/Gencode_human/release_35/gencode.v35.chr_patch_hapl_scaff.basic.annotation.gtf.gz";



    public SvAnnDownloader(String path){
        this(path,false);
    }

    public SvAnnDownloader(String path, boolean overwrite){
        this.downloadDirectory=path;
        this.overwrite=overwrite;
    }

    /**
     * Download the files unless they are already present.
     */
    public void download() {
        downloadFileIfNeeded(HP_OBO,HP_OBO_URL);
        downloadFileIfNeeded(HP_ANNOTATION,HP_ANNOTATION_URL);
        downloadFileIfNeeded(GENE_INFO,GENE_INFO_URL);
        downloadFileIfNeeded(MIM2GENE_MEDGEN,MIM2GENE_MEDGEN_URL);
        downloadFileIfNeeded(GENCODE_FILE, GENECODE_URL);
    }







    private void downloadFileIfNeeded(String filename, String webAddress) {
        File f = new File(String.format("%s%s%s",downloadDirectory,File.separator,filename));
        if (f.exists() && (! overwrite)) {
            return;
        }
        FileDownloader downloader=new FileDownloader();
        try {
            URL url = new URL(webAddress);
            downloader.copyURLToFile(url, new File(f.getAbsolutePath()));
        } catch (MalformedURLException | FileDownloadException e) {
            e.printStackTrace();
        }
        System.out.println("[INFO] Downloaded " + filename);
    }





}