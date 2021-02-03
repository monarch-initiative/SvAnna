package org.jax.svanna.cli.cmd.download;

import org.jax.svanna.cli.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "download",
        aliases = "D",
        header = "download annotation files",
        mixinStandardHelpOptions = true,
        version = Main.VERSION,
        usageHelpWidth = Main.WIDTH,
        footer = Main.FOOTER)
public class DownloadCommand implements Callable<Integer> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DownloadCommand.class);

    @CommandLine.Option(names = {"-d", "--download-dir"}, description = "path to download directory (default: ${DEFAULT-VALUE})")
    public Path downloadDir = Path.of("data");

    @CommandLine.Option(names = {"-f", "--force-overwrite"}, description = "force overwrite the files (default: ${DEFAULT-VALUE})")
    public boolean overwrite = false;

    @Override
    public Integer call() {
        try {
            SvAnnDownloader.download(downloadDir, overwrite);
        } catch (IOException e) {
            LOGGER.error("Error: {}", e.getMessage());
            return 1;
        }
        LOGGER.info("Done!");
        return 0;
    }
}
