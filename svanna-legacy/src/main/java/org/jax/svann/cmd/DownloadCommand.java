package org.jax.svann.cmd;

import org.jax.svann.io.SvAnnDownloader;
import picocli.CommandLine;

import java.util.concurrent.Callable;

@CommandLine.Command(name = "download", aliases = {"D"}, mixinStandardHelpOptions = true, description = "Download files")
public class DownloadCommand implements Callable<Integer> {

    public DownloadCommand(){

    }

    @Override
    public Integer call() {
        SvAnnDownloader downloader = new SvAnnDownloader("data");
        downloader.download();
        return 0;
    }
}
