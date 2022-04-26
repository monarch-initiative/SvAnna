package org.monarchinitiative.svanna.ingest.parse.dosage;

import org.monarchinitiative.svanna.ingest.parse.IOUtils;
import org.monarchinitiative.svanna.ingest.parse.IngestRecordParser;
import org.monarchinitiative.svanna.model.landscape.dosage.DosageRegion;
import org.monarchinitiative.svart.*;
import org.monarchinitiative.svart.assembly.GenomicAssembly;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.monarchinitiative.svanna.ingest.parse.dosage.DosageElementsUtil.makeDosageElements;

public class ClingenRegionCurationParser implements IngestRecordParser<DosageRegion> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClingenRegionCurationParser.class);

    private static final Pattern GENOMIC_LOCATION_PATTERN = Pattern.compile("(?<contig>chr([XY]|\\d+)):(?<start>\\d+)-(?<end>\\d+)");

    private final Path clingenRegionPath;

    private final GenomicAssembly genomicAssembly;

    public ClingenRegionCurationParser(Path clingenRegionPath, GenomicAssembly genomicAssembly) {
        this.clingenRegionPath = clingenRegionPath;
        this.genomicAssembly = genomicAssembly;
    }


    @Override
    public Stream<? extends DosageRegion> parse() throws IOException {
        BufferedReader reader = Files.newBufferedReader(clingenRegionPath);
        return reader.lines()
                .onClose(IOUtils.close(reader))
                .filter(line -> !line.startsWith("#")) // header
                .map(toDosageElements())
                .flatMap(Collection::stream);
    }

    private Function<String, List<DosageRegion>> toDosageElements() {
        return line -> {
            /*
            A line like:
            #ISCA ID        ISCA Region Name        cytoBand        Genomic Location        Haploinsufficiency Score        Haploinsufficiency Description  Haploinsufficiency PMID1        Haploinsufficiency PMID2        Haploinsufficiency PMID3        Haploinsufficiency PMID4        Haploinsufficiency PMID5        Haploinsufficiency PMID6        Triplosensitivity Score Triplosensitivity Description   Triplosensitivity PMID1 Triplosensitivity PMID2 Triplosensitivity PMID3 Triplosensitivity PMID4 Triplosensitivity PMID5 Triplosensitivity PMID6 Date Last Evaluated     Loss phenotype OMIM ID  Triplosensitive phenotype OMIM ID
            ISCA-46739      Yq11.223 population region (DGV_Gold_Standard_June_2021_gssvL138428)    Yq11.223        chrY:23707042-24000812  40      Dosage sensitivity unlikely       0       No evidence available                                                   2021-08-03

            We're interested in columns (indices) `Genomic Location` (3), `Haploinsufficiency Score` (4), and `Triplosensitivity Score` (12)
             */
            String[] tokens = line.split("\t", 23);
            if (tokens.length != 23) {
                LOGGER.warn("Expected {} columns and found {}. Skipping the line `{}`", 23, tokens.length, line);
                return List.of();
            }

            // ID
            String id = tokens[0];
            if (id.isBlank()) {
                LOGGER.warn("Skipping line with no ID: `{}`", line);
                return List.of();
            }

            // some regions have an extra space, e.g. `chrX:30176883 -30336883`, or `chrX: 48447780-52444264`
            String regionString = tokens[3].replaceAll("\\s", "");
            Matcher matcher = GENOMIC_LOCATION_PATTERN.matcher(regionString);
            if (!matcher.matches()) {
                LOGGER.warn("Invalid genomic location `{}`, skipping the line `{}`", regionString, line);
                return List.of();
            }
            Contig contig = genomicAssembly.contigByName(matcher.group("contig"));
            if (contig.isUnknown()) {
                LOGGER.warn("Unknown contig `{}` in line `{}`", matcher.group("contig"), line);
                return List.of();
            }
            int start = Integer.parseInt(matcher.group("start"));
            int end = Integer.parseInt(matcher.group("end"));
            Coordinates coordinates = Coordinates.of(CoordinateSystem.oneBased(), start, end);


            return makeDosageElements(contig, Strand.POSITIVE, coordinates, id, tokens[4], tokens[12]);
        };
    }
}
