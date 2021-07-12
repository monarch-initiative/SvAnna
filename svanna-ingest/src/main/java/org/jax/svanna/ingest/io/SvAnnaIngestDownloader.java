package org.jax.svanna.ingest.io;


import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

public class SvAnnaIngestDownloader {

    /** Directory to which we will download the files. */
    private final String downloadDirectory;
    /** If true, download new version whether or not the file is already present. */
    private final boolean overwrite;

    private final static String ENHANCER_FILE = "F5.hg38.enhancers.expression.matrix.gz";
     /** URL of the annotation file phenotype.hpoa. */
    private final static String ENHANCERS_URL ="https://fantom.gsc.riken.jp/5/datafiles/reprocessed/hg38_latest/extra/enhancer/F5.hg38.enhancers.expression.matrix.gz";

    private final static String SAMPLENAME_FILE = "Human.sample_name2library_id.txt";

    private final static String SAMPLENAME_URL = "https://fantom.gsc.riken.jp/5/datafiles/latest/extra/Enhancers/Human.sample_name2library_id.txt";

    public SvAnnaIngestDownloader(String path){
        this(path,false);
    }

    public SvAnnaIngestDownloader(String path, boolean overwrite){
        this.downloadDirectory=path;
        this.overwrite=overwrite;
    }

    /**
     * Download the files unless they are already present.
     */
    public void download() {
        downloadFileIfNeeded(ENHANCER_FILE,ENHANCERS_URL);
        downloadFileIfNeeded(SAMPLENAME_FILE, SAMPLENAME_URL);
    }







    private void downloadFileIfNeeded(String filename, String webAddress) {
        File f = new File(String.format("%s%s%s",downloadDirectory,File.separator,filename));
        if (f.exists() && (! overwrite)) {
            return;
        }
        FileDownloader downloader= new FileDownloader();
        try {
            URL url = new URL(webAddress);
            downloader.copyURLToFile(url, new File(f.getAbsolutePath()));
        } catch (MalformedURLException | FileDownloadException e) {
            e.printStackTrace();
        }
        System.out.println("[INFO] Downloaded " + filename);
    }





}
