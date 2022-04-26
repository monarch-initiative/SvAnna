package org.monarchinitiative.svanna.ingest;

import org.monarchinitiative.svanna.ingest.cmd.BuildDb;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The purpose of this class is to create an empty database which can be used with IDE to provide SQL autocompletion
 * and other hints.
 * <p>
 * The database is built at the top level of the repo.
 */
@Disabled("Test suite designed to be run manually in order to create an empty database which can be used with an IDE to provide SQL autocompletion etc.")
public class MakeAnEmptyDatabase {

    /**
     * Path to top-level Squirls code-base directory.
     */
    private Path appHomeDir;

    @BeforeEach
    public void setUp() {
        appHomeDir = Path.of(MakeAnEmptyDatabase.class.getResource("/").getPath()).getParent().getParent().getParent();
    }

    @Test
    public void makeDatabase() throws Exception {
        Path databasePath = appHomeDir.resolve("1710_svanna_empty");
        Path filePath = appHomeDir.resolve("1710_svanna_empty.mv.db");

        if (filePath.toFile().isFile()) {
            System.err.printf("Removing already existing database file at `%s`%n", filePath);
            Files.delete(filePath);
        }

        System.err.printf("Making an empty database at `%s`%n", databasePath);
        BuildDb.initializeDataSource(databasePath);
    }

}
