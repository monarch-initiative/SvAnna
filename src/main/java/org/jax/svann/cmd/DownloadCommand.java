package org.jax.svann.cmd;

import org.jax.svann.io.L2ODownloader;
import picocli.CommandLine;

import java.util.concurrent.Callable;

@CommandLine.Command(name = "download", aliases = {"D"}, mixinStandardHelpOptions = true, description = "Download files")
public class DownloadCommand implements Callable<Integer> {

    public DownloadCommand(){

    }

    @Override
    public Integer call() {
        L2ODownloader downloader = new L2ODownloader("data");
        downloader.download();
        return 0;
    }
}