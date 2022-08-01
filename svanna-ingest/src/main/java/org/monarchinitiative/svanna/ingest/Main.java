package org.monarchinitiative.svanna.ingest;

import org.monarchinitiative.svanna.ingest.cmd.BuildDb;
import org.monarchinitiative.svanna.ingest.cmd.DownloadCommand;
import org.monarchinitiative.svanna.ingest.cmd.GenerateConfigCommand;
import picocli.CommandLine;

import java.util.Locale;
import java.util.concurrent.Callable;

import static picocli.CommandLine.Help.Ansi.Style.*;

@CommandLine.Command(name = "svanna-ingest.jar",
        header = "ET(L) for svanna data sources.",
        mixinStandardHelpOptions = true,
        version = Main.VERSION,
        usageHelpWidth = Main.WIDTH,
        footer = Main.FOOTER)
public class Main implements Callable<Integer> {
    public static final String VERSION = "svanna-ingest v1.0.2";

    public static final int WIDTH = 120;

    public static final String FOOTER = "See the full documentation at `https://svanna.readthedocs.io/en/master`";

    private static final CommandLine.Help.ColorScheme COLOR_SCHEME = new CommandLine.Help.ColorScheme.Builder()
            .commands(bold, fg_blue, underline)
            .options(fg_yellow)
            .parameters(fg_yellow)
            .optionParams(italic)
            .build();

    private static CommandLine commandLine;

    public static void main(String [] args) {
        Locale.setDefault(Locale.US);
        commandLine = new CommandLine(new Main())
                .setColorScheme(COLOR_SCHEME)
                .addSubcommand("generate-config", new GenerateConfigCommand())
                .addSubcommand("download", new DownloadCommand())
                .addSubcommand("build-db", new BuildDb());

        commandLine.setToggleBooleanFlags(false);
        System.exit(commandLine.execute(args));
    }

    @Override
    public Integer call() {
        // work done in subcommands
        commandLine.usage(commandLine.getOut());
        return 0;
    }

}
