package org.jax.l2o;


import org.jax.l2o.cmd.AnnotateCommand;
import org.jax.l2o.cmd.DownloadCommand;
import org.jax.l2o.html.HtmlTemplate;
import org.jax.l2o.io.L2ODownloader;
import org.jax.l2o.lirical.LiricalHit;
import org.jax.l2o.vcf.AnnotatedVcfParser;
import org.jax.l2o.vcf.SvAnn;
import org.monarchinitiative.phenol.ontology.data.TermId;
import picocli.CommandLine;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

@CommandLine.Command(name = "l2o", mixinStandardHelpOptions = true, version = "l2o 0.0.1",
        description = "LIRICAL to overlapping SV")
public class Main implements Callable<Integer>  {





    public static void main(String [] args) {
        CommandLine cline = new CommandLine(new Main())
                .addSubcommand("download", new DownloadCommand())
                .addSubcommand("annotate", new AnnotateCommand());
        cline.setToggleBooleanFlags(false);
        int exitCode = cline.execute(args);
        System.exit(exitCode);


    }




    @Override
    public Integer call() throws Exception {
        // work done in subcommands
        return 0;
    }
}
