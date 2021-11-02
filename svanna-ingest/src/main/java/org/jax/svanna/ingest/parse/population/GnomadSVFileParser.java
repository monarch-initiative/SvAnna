package org.jax.svanna.ingest.parse.population;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.jax.svanna.ingest.parse.IOUtils;
import org.jax.svanna.ingest.parse.IngestRecordParser;
import org.jax.svanna.model.landscape.PopulationVariant;
import org.monarchinitiative.svart.GenomicAssembly;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.zip.GZIPInputStream;

public class GnomadSVFileParser implements IngestRecordParser<PopulationVariant> {

    private static final Logger LOGGER = LoggerFactory.getLogger(GnomadSVFileParser.class);

    private final GenomicAssembly genomicAssembly;
    private final Path gnomadSvPath;

    public GnomadSVFileParser(GenomicAssembly genomicAssembly, Path gnomadSvPath) {
        this.genomicAssembly = genomicAssembly;
        this.gnomadSvPath = gnomadSvPath;
    }


    @Override
    public Stream<PopulationVariant> parse() throws IOException {
        Reader reader = new InputStreamReader(new GZIPInputStream(Files.newInputStream(gnomadSvPath)));
        CSVParser parser = CSVFormat.DEFAULT.withFirstRecordAsHeader()
                .parse(reader);
        return StreamSupport.stream(parser.spliterator(), false)
                .onClose(IOUtils.close(reader))
                .map(toVariants())
                .flatMap(Collection::stream);
    }



    private Function<? super CSVRecord, Collection<? extends PopulationVariant>> toVariants() {
        return csv -> {
            String id = csv.get("Variant ID");
            String contigName = csv.get("Chromosome Accession");
            int start = Integer.parseInt(csv.get("Start"));
            int end = Integer.parseInt(csv.get("End"));

            return List.of();
        };
    }

}
