package org.jax.svann.reference;

import java.nio.file.Path;


class DefaultGenomeAssemblyProvider extends AssemblyReportGenomeAssemblyProvider {

    private static final Path[] ASSEMBLY_REPORTS;
    static {
        ASSEMBLY_REPORTS = new Path[]{
                // GRCh37
                Path.of("src/main/resources/GCA_000001405.14_GRCh37.p13_assembly_report.txt"),
                // GRCh38
                Path.of("src/main/resources/GCA_000001405.28_GRCh38.p13_assembly_report.txt")
        };
    }

    private static final DefaultGenomeAssemblyProvider INSTANCE = new DefaultGenomeAssemblyProvider();

    private DefaultGenomeAssemblyProvider() {
        super(ASSEMBLY_REPORTS);
    }

    static DefaultGenomeAssemblyProvider getInstance() {
        return INSTANCE;
    }
}
