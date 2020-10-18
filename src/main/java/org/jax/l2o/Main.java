package org.jax.l2o;


import org.jax.l2o.cmd.AnnotateCommand;
import org.jax.l2o.cmd.DownloadCommand;
import picocli.CommandLine;

import java.util.concurrent.Callable;

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
