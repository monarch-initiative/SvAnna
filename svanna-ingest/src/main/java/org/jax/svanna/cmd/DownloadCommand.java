package org.jax.svanna.cmd;

import org.jax.svanna.Main;
import org.jax.svanna.io.SvAnnaIngestDownloader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.util.concurrent.Callable;

@CommandLine.Command(name = "download",
        aliases = "D",
        header = "ingest and transform annotation files",
        mixinStandardHelpOptions = true,
        version = Main.VERSION,
        usageHelpWidth = Main.WIDTH,
        footer = Main.FOOTER)
public class DownloadCommand implements Callable<Integer> {
    private static final Logger LOGGER = LoggerFactory.getLogger(IngestCommand.class);

    @CommandLine.Option(names ={"-d"}, description = "download directory")
    private String downloadPath = "data";

    @Override
    public Integer call() {
        SvAnnaIngestDownloader download = new SvAnnaIngestDownloader(downloadPath);
        download.download();
        return 0;
    }

}
