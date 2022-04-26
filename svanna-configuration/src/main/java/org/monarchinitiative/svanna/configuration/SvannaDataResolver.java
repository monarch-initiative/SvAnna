package org.monarchinitiative.svanna.configuration;

import org.monarchinitiative.svanna.configuration.exception.MissingResourceException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class SvannaDataResolver {

    private final Path svannaDataDirectory;

    public SvannaDataResolver(Path svannaDataDirectory) throws MissingResourceException {
        this.svannaDataDirectory = svannaDataDirectory;

        // now check that we have all files present
        List<Path> paths = List.of(fullDataSourcePath(), hpOntologyPath(), genesJsonPath());
        for (Path path : paths) {
            if (!(Files.isRegularFile(path) && Files.isReadable(path))) {
                throw new MissingResourceException(String.format("The file `%s` is missing in SvAnna directory", path.toFile().getName()));
            }
        }
    }

    public Path dataSourcePath() {
        return svannaDataDirectory.resolve("svanna_db");
    }

    public Path fullDataSourcePath() {
        return svannaDataDirectory.resolve("svanna_db.mv.db");
    }

    public Path hpOntologyPath() {
        return svannaDataDirectory.resolve("hp.json");
    }

    public Path phenotypeHpoaPath() {
        return svannaDataDirectory.resolve("phenotype.hpoa");
    }

    public Path mim2geneMedgenPath() {
        return svannaDataDirectory.resolve("mim2gene_medgen");
    }

    public Path geneInfoPath() {
        return svannaDataDirectory.resolve("Homo_sapiens.gene_info.gz");
    }

    public Path genesJsonPath() {
        return svannaDataDirectory.resolve("gencode.v38.genes.json.gz");
    }
}
