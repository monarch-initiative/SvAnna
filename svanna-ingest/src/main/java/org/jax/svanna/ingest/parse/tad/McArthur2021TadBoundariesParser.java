package org.jax.svanna.ingest.parse.tad;

import htsjdk.samtools.liftover.LiftOver;
import htsjdk.samtools.util.Interval;
import org.jax.svanna.core.landscape.TadBoundary;
import org.jax.svanna.core.landscape.TadBoundaryDefault;
import org.jax.svanna.ingest.parse.IngestRecordParser;
import org.monarchinitiative.svart.Contig;
import org.monarchinitiative.svart.CoordinateSystem;
import org.monarchinitiative.svart.GenomicAssembly;
import org.monarchinitiative.svart.Strand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Class for parsing TAD boundaries from supplement file of
 * <a href="https://pubmed.ncbi.nlm.nih.gov/33545030/">Topologically associating domain boundaries that are stable
 * across diverse cell types are evolutionarily constrained and enriched for heritability</a>.
 */
public class McArthur2021TadBoundariesParser implements IngestRecordParser<TadBoundary> {

    private static final Logger LOGGER = LoggerFactory.getLogger(McArthur2021TadBoundariesParser.class);

    private final GenomicAssembly genomicAssembly;

    private final InputStream boundariesStabilityInputStream;

    private final LiftOver liftOver;

    public McArthur2021TadBoundariesParser(GenomicAssembly genomicAssembly, InputStream boundariesStabilityInputStream, Path hg19ToHg38Chain) {
        this.genomicAssembly = genomicAssembly;
        this.boundariesStabilityInputStream = boundariesStabilityInputStream;
        this.liftOver = new LiftOver(hg19ToHg38Chain.toFile());
    }

    @Override
    public Stream<? extends TadBoundary> parse() throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(boundariesStabilityInputStream));
        return reader.lines()
                .skip(1) // header
                .map(toTadBoundary())
                .filter(Optional::isPresent)
                .map(Optional::get);
    }

    private Function<String, Optional<? extends TadBoundary>> toTadBoundary() {
        return line -> {
            String[] column = line.trim().split("\\s+");
            if (column.length < 5) {
                if (LOGGER.isWarnEnabled()) LOGGER.warn("Ignoring record with {}!=5 fields: `{}`", column.length, line);
                return Optional.empty();
            }

            // To minimize number of regions that cannot be lifted over, we do not lift over the entire TAD border region,
            // but only the middle point. The TAD border is recreated after lift over
            int begin = Integer.parseInt(column[1]);
            int end = Integer.parseInt(column[2]);
            int halfLength = (end - begin) / 2;
            int median = begin + halfLength;

            // the original location on hg19 is stored as the ID
            String id = String.format("%s:%s-%s", column[0], column[1], column[2]);
            Interval interval = new Interval(column[0], median, median, false, id);
            Interval lifted = liftOver.liftOver(interval);
            if (lifted == null) {
                if (LOGGER.isWarnEnabled()) LOGGER.warn("Could not lift over record {} of `{}`", id, line);
                return Optional.empty();
            }
            Contig contig = genomicAssembly.contigByName(lifted.getContig());
            if (contig.isUnknown()) {
                if (LOGGER.isWarnEnabled()) LOGGER.warn("Unknown contig `{}` after lifting over record `{}`", lifted.getContig(), line);
                return Optional.empty();
            }
            int liftedStart = lifted.getStart() - halfLength;
            int liftedEnd = lifted.getEnd() + halfLength;
            return Optional.of(TadBoundaryDefault.of(contig, Strand.POSITIVE, CoordinateSystem.zeroBased(), liftedStart, liftedEnd, id, Float.parseFloat(column[4])));
        };
    }
}
