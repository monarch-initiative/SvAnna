package org.monarchinitiative.svanna.io;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class IOUtils {

    private IOUtils() {
        // static utility class
    }
    public static BufferedReader openForReading(Path path) throws IOException {
        return (path.toFile().getName().endsWith(".gz"))
                ? new BufferedReader(new InputStreamReader(new GZIPInputStream(Files.newInputStream(path))))
                : Files.newBufferedReader(path);
    }

    public static BufferedWriter openForWriting(Path path) throws IOException {
        return (path.toFile().getName().endsWith(".gz"))
                ? new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(Files.newOutputStream(path))))
                : Files.newBufferedWriter(path);
    }
}
