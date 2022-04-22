package org.jax.svanna.ingest.io;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.zip.Deflater;

/**
 * Add files into a ZIP file.
 * @author Daniel Danis
 */
public class ZipCompressionWrapper implements Closeable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZipCompressionWrapper.class);

    private final ZipArchiveOutputStream archive;

    public ZipCompressionWrapper(File zipPath) throws IOException {
        archive = new ZipArchiveOutputStream(zipPath);
        archive.setLevel(Deflater.BEST_COMPRESSION);
    }

    public void addResource(File file, String name) throws IOException {
        BasicFileAttributes attributes = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
        if (attributes.isDirectory()) {
            LOGGER.info("Skipping directory {}", file);
            return;
        }

        ArchiveEntry entry = archive.createArchiveEntry(file, name);
        archive.putArchiveEntry(entry);
        if (attributes.isRegularFile()) {
            try (InputStream is = Files.newInputStream(file.toPath())) {
                IOUtils.copy(is, archive);
            }
        }
        archive.closeArchiveEntry();
    }

    @Override
    public void close() throws IOException {
        archive.finish();
        archive.close();
    }
}
