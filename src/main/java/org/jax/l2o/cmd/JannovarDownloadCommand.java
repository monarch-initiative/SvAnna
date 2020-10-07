package org.jax.l2o.cmd;


import de.charite.compbio.jannovar.Jannovar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;

/**
 * Note -- there is a bug that requires us to first run this code and then
 * delete the line chrM    16571   /gbdb/hg19/hg19.2bit from the chromInfo.txt.gz file
 * (and then recompressing the file).
 */
@CommandLine.Command(name = "jannovar",  mixinStandardHelpOptions = true, description = "Create jannovar file")
public class JannovarDownloadCommand implements Callable<Integer> {
    static Logger logger = LoggerFactory.getLogger(JannovarDownloadCommand.class);
    private static final Path iniPath = Paths.get("src","main","resources","jannovar_sources.ini");
    private static final String iniAbsPath = iniPath.toAbsolutePath().toString();
    private static final String downloadDir = "data";

    @Override
    public Integer call() throws Exception {
        logger.debug("Executing CreateJannovarTranscriptFile");
        String args[]={"download","-d", "hg38/ensembl", "-s", iniAbsPath, "--download-dir", downloadDir};
        Jannovar.main(args);
        return 0;
    }
}
