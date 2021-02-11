package org.jax.svanna.ingest.parse;

import org.jax.svanna.core.reference.RepeatFamily;
import org.jax.svanna.core.reference.RepetitiveRegion;
import org.monarchinitiative.svart.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

public class RepetitiveRegionParser implements IngestRecordParser<RepetitiveRegion> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RepetitiveRegionParser.class);

    private final GenomicAssembly genomicAssembly;

    private final Path repeatsPath;

    public RepetitiveRegionParser(GenomicAssembly genomicAssembly, Path repeatsPath) {
        this.genomicAssembly = genomicAssembly;
        this.repeatsPath = repeatsPath;
    }

    @Override
    public Stream<? extends RepetitiveRegion> parse() throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(Files.newInputStream(repeatsPath))));
        return reader.lines()
                .onClose(IOUtils.close(reader))
                .skip(3) // header
                .map(toRepetitiveRegion())
                .filter(Optional::isPresent)
                .map(Optional::get);
    }

    private Function<String, Optional<? extends RepetitiveRegion>> toRepetitiveRegion() {
        return line -> {
            String[] column = line.trim().split("\\s+");
            if (column.length < 15) {
                if (LOGGER.isWarnEnabled()) LOGGER.warn("Found {} columns, expected 15 in line `{}`", column.length, line);
                return Optional.empty();
            }

            Contig contig = genomicAssembly.contigByName(column[4]);
            if (contig.isUnknown()) {
                if (LOGGER.isWarnEnabled()) LOGGER.warn("Unknown contig `{}` in line `{}`", column[4], line);
                return Optional.empty();
            }

            Position start, end;
            try {
                start = Position.of(Integer.parseInt(column[5]));
                end = Position.of(Integer.parseInt(column[6]));
            } catch (NumberFormatException e) {
                if (LOGGER.isWarnEnabled()) LOGGER.warn("Invalid start/end coordinate in line `{}`", line);
                return Optional.empty();
            }

            RepeatFamily repeatFamily = parseRepeatFamily(column[10]);
            return Optional.of(RepetitiveRegion.of(contig, Strand.POSITIVE, CoordinateSystem.oneBased(), start, end, repeatFamily));
        };
    }

    private static RepeatFamily parseRepeatFamily(String value) {
        switch (value.toLowerCase()) {
            case "dna":
            case "dna?":
                return RepeatFamily.DNA;
            case "dna/hat":
            case "dna/hat?":
                return RepeatFamily.DNA_hAT;
            case "dna/hat-ac":
                return RepeatFamily.DNA_hAT_Ac;
            case "dna/hat-blackjack":
                return RepeatFamily.DNA_hAT_Blackjack;
            case "dna/hat-charlie":
                return RepeatFamily.DNA_hAT_Charlie;
            case "dna/hat-tag1":
                return RepeatFamily.DNA_hAT_Tag1;
            case "dna/hat-tip100":
            case "dna/hat-tip100?":
                return RepeatFamily.DNA_hAT_Tip100;
            case "dna/merlin":
                return RepeatFamily.DNA_Merlin;
            case "dna/mule-mudr":
                return RepeatFamily.DNA_MULE_MuDR;
            case "dna/pif-harbinger":
                return RepeatFamily.DNA_PIF_Harbinger;
            case "dna/piggybac":
            case "dna/piggybac?":
                return RepeatFamily.DNA_PiggyBac;
            case "dna/tcmar":
            case "dna/tcmar?":
                return RepeatFamily.DNA_TcMar;
            case "dna/tcmar-mariner":
                return RepeatFamily.DNA_TcMar_Mariner;
            case "dna/tcmar-pogo":
                return RepeatFamily.DNA_TcMar_Pogo;
            case "dna/tcmar-tc2":
                return RepeatFamily.DNA_TcMar_Tc2;
            case "dna/tcmar-tigger":
                return RepeatFamily.DNA_TcMar_Tigger;
            case "line/cr1":
            case "line/dong-r4":
            case "line/l1":
            case "line/l2":
            case "line/penelope":
            case "line/rte-bovb":
            case "line/rte-x":
                return RepeatFamily.LINE;
            case "low_complexity":
                return RepeatFamily.LOW_COMPLEXITY;
            case "ltr":
            case "ltr?":
                return RepeatFamily.LTR;
            case "ltr/erv1":
            case "ltr/erv1?":
                return RepeatFamily.LTR_ERV1;
            case "ltr/ervk":
                return RepeatFamily.LTR_ERVK;
            case "ltr/ervl":
            case "ltr/ervl?":
                return RepeatFamily.LTR_ERVL;
            case "ltr/ervl-malr":
                return RepeatFamily.LTR_ERVL_MaLR;
            case "ltr/gypsy":
            case "ltr/gypsy?":
                return RepeatFamily.LTR_Gypsy;
            case "sine?":
                return RepeatFamily.SINE;
            case "sine/5s-deu-l2":
                return RepeatFamily.SINE_5SDeuL2;
            case "sine/alu":
                return RepeatFamily.SINE_ALU;
            case "sine/mir":
                return RepeatFamily.SINE_MIR;
            case "sine/trna":
            case "sine?/trna":
                return RepeatFamily.SINE_tRNA;
            case "sine/trna-deu":
                return RepeatFamily.SINE_tRNA_Deu;
            case "sine/trna-rte":
                return RepeatFamily.SINE_tRNA_RTE;
            case "retroposon/sva":
                return RepeatFamily.RETROPOSON;
            case "rc/helitron":
            case "rc?/helitron?":
                return RepeatFamily.RC_HELITRON;
            case "rna":
                return RepeatFamily.RNA;
            case "rrna":
                return RepeatFamily.RNA_rRNA;
            case "scrna":
                return RepeatFamily.RNA_scRNA;
            case "snrna":
                return RepeatFamily.RNA_snRNA;
            case "srprna":
                return RepeatFamily.RNA_srpRNA;
            case "trna":
                return RepeatFamily.RNA_tRNA;
            case "satellite":
                return RepeatFamily.SATELLITE;
            case "satellite/acro":
                return RepeatFamily.SATELLITE_ACRO;
            case "satellite/centr":
                return RepeatFamily.SATELLITE_CENTR;
            case "satellite/telo":
                return RepeatFamily.SATELLITE_TELO;
            case "simple_repeat":
                return RepeatFamily.SIMPLE_REPEAT;
            case "unknown":
            default:
                return RepeatFamily.UNKNOWN;
        }
    }
}
