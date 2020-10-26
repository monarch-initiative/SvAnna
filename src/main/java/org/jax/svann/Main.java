package org.jax.svann;


import org.jax.svann.cmd.AnnotateCommand;
import org.jax.svann.cmd.DownloadCommand;
import picocli.CommandLine;

import java.util.concurrent.Callable;

@CommandLine.Command(name = "svann", mixinStandardHelpOptions = true, version = "0.2.9",
        description = "Structural variant annotation")
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
    public Integer call() {
        // work done in subcommands
        return 0;
    }
}
