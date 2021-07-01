package org.jax.svanna.benchmark;

import picocli.CommandLine;

import java.util.Locale;
import java.util.concurrent.Callable;

import static picocli.CommandLine.Help.Ansi.Style.*;

@CommandLine.Command(name = "svanna-benchmark.jar",
        header = "Structural variant annotation",
        mixinStandardHelpOptions = true,
        version = Main.VERSION,
        usageHelpWidth = Main.WIDTH,
        footer = Main.FOOTER)
public class Main implements Callable<Integer>  {

    public static final String VERSION = "svanna v0.3.1-SNAPSHOT";

    public static final int WIDTH = 120;

    public static final String FOOTER = "See the full documentation at `https://github.com/TheJacksonLaboratory/SvAnna`";

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
                .addSubcommand("benchmark-case", new BenchmarkCaseCommand());
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
