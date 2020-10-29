package org.jax.svann.parse;

import org.jax.svann.reference.*;
import org.jax.svann.reference.genome.Contig;
import org.jax.svann.reference.genome.GenomeAssembly;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * There are 3 types of events in the merged file:
 * <ul>
 *     <li>DUP</li>
 *     <li>DEL</li>
 *     <li>INV</li>
 * </ul>
 */
public class MergedStructuralRearrangementParser implements StructuralRearrangementParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(MergedStructuralRearrangementParser.class);

    private static final String DELIMITER = "\t";

    private final GenomeAssembly assembly;

    public MergedStructuralRearrangementParser(GenomeAssembly assembly) {
        this.assembly = assembly;
    }

    private static Map<Caller, RecordData> parseData(String callerData, String infoColumnString) {
        // callers:    e.g. `pbsv:sniffles:svim`
        // infoColumn: e.g. `pbsv.DEL.30582;15;PASS;1/1:96836;33;PASS;1/1:svim.DEL.24308;35;PASS;1/1`
        String[] callers = callerData.split(":");
        String[] splits = infoColumnString.split(":");
        if (callers.length != splits.length) {
            return Map.of();
        }
        Map<Caller, RecordData> map = new HashMap<>();
        for (int i = 0; i < callers.length; i++) {
            Caller caller = Caller.valueOf(callers[i]);
            String[] fields = splits[i].split(";");
            RecordData recordData;
            try {
                recordData = new RecordData(fields[0], Integer.parseInt(fields[1]), fields[2], fields[3]);
            } catch (NumberFormatException e) {
                LOGGER.warn("Invalid depth `{}`", fields[1]);
                continue;
            }
            map.put(caller, recordData);
        }

        return map;
    }

    @Override
    public Collection<SequenceRearrangement> parseFile(Path filePath) throws IOException {
        /* Tab-delimited file with entries like:
        CM000663.2	1080765	1080860	DEL	pbsv:sniffles:svim	SAMPLE1	pbsv.DEL.50;10;PASS;0/1:122;24;PASS;1/1:svim.DEL.166;24;PASS;0/1
        CM000663.2	1099137	1099828	DUP	sniffles:svim	SAMPLE1	125;9;PASS;0/0:svim.DUP_TANDEM.17;5;PASS;./.
        CM000663.2	2031552	2031782	INV	sniffles:svim	PID1048	245;4;PASS;0/0:svim.INV.3;3;PASS;./.
         */
        try (BufferedReader reader = Files.newBufferedReader(filePath)) {
            return reader.lines()
                    .map(toSequenceRearrangement())
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toList());
        }
    }

    private Function<String, Optional<SequenceRearrangement>> toSequenceRearrangement() {
        return line -> {
            // line like:
            // CM000663.2	1080765	1080860	DEL	pbsv:sniffles:svim	SAMPLE1	pbsv.DEL.50;10;PASS;0/1:122;24;PASS;1/1:svim.DEL.166;24;PASS;0/1

            String[] tokens = line.split(DELIMITER);
            List<Adjacency> adjacencies = new ArrayList<>();

            // get SvType
            SvType svType = SvType.fromString(tokens[3]);


            switch (svType) {
                case DELETION:
                    // intrachromosomal event represented by a single adjacency
                    makeDeletion(tokens).ifPresent(adjacencies::add);
                    break;
                case DUPLICATION:
                    // intrachromosomal event represented by a single adjacency
                    makeDuplication(tokens).ifPresent(adjacencies::add);
                    break;
                case INVERSION:
                    // intrachromosomal event represented by two adjacencies
                    adjacencies.addAll(makeInversion(tokens));
                    break;
                default:
                    LOGGER.warn("Unsupported svtype `{}` in line `{}`", svType, line);
                    return Optional.empty();
            }

            return Optional.of(SimpleSequenceRearrangement.of(svType, adjacencies));
        };
    }

    private Collection<? extends Adjacency> makeInversion(String[] tokens) {
        // tokens represent record with an inversion
        // we have two adjacencies - alpha, and beta
        Optional<CoreCoords> coreCoordsOpt = makeCoreCoords(tokens);
        if (coreCoordsOpt.isEmpty()) {
            return List.of();
        }
        CoreCoords coreCoords = coreCoordsOpt.get();
        String id = coreCoords.id;

        // the 1st adjacency (alpha) starts one base before begin coordinate (POS), by convention on the (+) strand
        ChromosomalPosition alphaLeftPos = ChromosomalPosition.precise(coreCoords.contig,
                coreCoords.begin.getBegin() - 1, Strand.FWD);
        Breakend alphaLeft = SimpleBreakend.of(alphaLeftPos, id);
        // the right position is the last base of the inverted segment on the (-) strand
        ChromosomalPosition alphaRightPos = ChromosomalPosition.of(coreCoords.contig, coreCoords.end.getBeginPosition(), Strand.FWD).withStrand(Strand.REV);
        SimpleBreakend alphaRight = SimpleBreakend.of(alphaRightPos, id);
        Adjacency alpha = SimpleAdjacency.empty(alphaLeft, alphaRight);

        // the 2nd adjacency (beta) starts at the begin coordinate on (-) strand
        ChromosomalPosition betaLeftPos = ChromosomalPosition.of(coreCoords.contig, coreCoords.begin.getBeginPosition(), Strand.FWD).withStrand(Strand.REV);
        SimpleBreakend betaLeft = SimpleBreakend.of(betaLeftPos, id);
        // the right position is one base past end coordinate, by convention on (+) strand
        ChromosomalPosition betaRightPos = ChromosomalPosition.precise(coreCoords.contig,
                coreCoords.end.getBegin() + 1, Strand.FWD);
        Breakend betaRight = SimpleBreakend.of(betaRightPos, id);
        Adjacency beta = SimpleAdjacency.empty(betaLeft, betaRight);

        return List.of(alpha, beta);
    }

    private Optional<Adjacency> makeDuplication(String[] tokens) {
        // tokens represent record with a duplication
        return makeCoreCoords(tokens).map(data -> {
            // the order for a duplication is inverted
            SimpleBreakend left = SimpleBreakend.of(data.end, data.id);
            SimpleBreakend right = SimpleBreakend.of(data.begin, data.id);

            return SimpleAdjacency.empty(left, right);
        });
    }

    private Optional<Adjacency> makeDeletion(String[] tokens) {
        return makeCoreCoords(tokens).map(data -> {
            SimpleBreakend left = SimpleBreakend.of(data.begin, data.id);
            SimpleBreakend right = SimpleBreakend.of(data.end, data.id);

            return SimpleAdjacency.empty(left, right);
        });
    }

    private Optional<CoreCoords> makeCoreCoords(String[] tokens) {
        // tokens represent record with a deletion
        Optional<Contig> contigOpt = assembly.getContigByName(tokens[0]);
        if (contigOpt.isEmpty()) {
            LOGGER.warn("Unknown contig {} in line {}", tokens[0], String.join(DELIMITER, tokens));
            return Optional.empty();
        }
        // contig
        Contig contig = contigOpt.get();
        // coordinates
        ChromosomalPosition begin, end;
        try {
            int beginPos = Integer.parseInt(tokens[1]);
            begin = ChromosomalPosition.of(contig, Position.precise(beginPos, CoordinateSystem.ZERO_BASED), Strand.FWD);
            int endPos = Integer.parseInt(tokens[2]);
            end = ChromosomalPosition.of(contig, Position.precise(endPos, CoordinateSystem.ONE_BASED), Strand.FWD);
        } catch (NumberFormatException e) {
            LOGGER.warn("Invalid begin/end coordinate in line {}", String.join(DELIMITER, tokens));
            return Optional.empty();
        }
        Map<Caller, RecordData> data = parseData(tokens[4], tokens[6]);

        return Optional.of(new CoreCoords(contig, begin, end, data));
    }

    private enum Caller {
        pbsv,
        sniffles,
        svim;


        public String shortName() {
            // get `sv`, `sn`, `pb`
            return name().substring(0, 2);
        }
    }

    private static class CoreCoords {
        private final Contig contig;
        private final ChromosomalPosition begin, end;
        private final Map<Caller, RecordData> dataMap;
        private final String id;

        private CoreCoords(Contig contig, ChromosomalPosition begin, ChromosomalPosition end, Map<Caller, RecordData> dataMap) {
            this.contig = contig;
            this.begin = begin;
            this.end = end;
            this.dataMap = dataMap;
            this.id = dataMap.entrySet().stream()
                    .map(e -> String.format("%s(%s)", e.getKey().shortName(), e.getValue().id))
                    .sorted()
                    .collect(Collectors.joining(";"));
        }
    }

    private static class RecordData {

        private final String id;
        private final int depth;
        private final String filter;
        private final String genotype;


        private RecordData(String id, int depth, String filter, String genotype) {
            this.id = id;
            this.depth = depth;
            this.filter = filter;
            this.genotype = genotype;
        }

    }

}
