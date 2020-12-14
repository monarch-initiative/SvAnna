package org.jax.svanna.io.parse;

import org.monarchinitiative.variant.api.*;
import org.monarchinitiative.variant.api.impl.SymbolicVariant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This parser expects to get path to a BED-like file with records that describe symbolic variants only.
 */
public class MergedVariantParser implements VariantParser<Variant> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MergedVariantParser.class);

    private static final int MISSING_DEPTH_PLACEHOLDER = -1;

    /**
     * The parser supports these symbolic variant types.
     */
    private static final Set<VariantType> SUPPORTED_TYPES = Set.of(VariantType.DEL, VariantType.DUP, VariantType.INV);


    private static final NumberFormat NF = NumberFormat.getInstance();
    private static final String DELIMITER = "\t";
    private final GenomicAssembly assembly;

    public MergedVariantParser(GenomicAssembly assembly) {
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
                recordData = new RecordData(fields[0], NF.parse(fields[1]).intValue(), fields[2], fields[3]);
            } catch (ParseException e) {
                LOGGER.warn("Invalid depth `{}`", fields[1]);
                continue;
            }
            map.put(caller, recordData);
        }

        return map;
    }

    @Override
    public Stream<Variant> createVariantAlleles(Path filePath) throws IOException {
        /* Tab-delimited file with entries like:
        CM000663.2	1080765	1080860	DEL	pbsv:sniffles:svim	SAMPLE1	pbsv.DEL.50;10;PASS;0/1:122;24;PASS;1/1:svim.DEL.166;24;PASS;0/1
        CM000663.2	1099137	1099828	DUP	sniffles:svim	SAMPLE1	125;9;PASS;0/0:svim.DUP_TANDEM.17;5;PASS;./.
        CM000663.2	2031552	2031782	INV	sniffles:svim	PID1048	245;4;PASS;0/0:svim.INV.3;3;PASS;./.
         */
        BufferedReader reader = Files.newBufferedReader(filePath);
        return reader.lines()
                .map(toVariant())
                .filter(Optional::isPresent)
                .map(Optional::get)
                .onClose(() -> {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        LOGGER.warn("Error closing the file {}", filePath);
                    }
                });
    }

    private Function<String, Optional<Variant>> toVariant() {
        return line -> {
            // line like:
            // CM000663.2	1080765	1080860	DEL	pbsv:sniffles:svim	SAMPLE1	pbsv.DEL.50;10;PASS;0/1:122;24;PASS;1/1:svim.DEL.166;24;PASS;0/1

            String[] tokens = line.split(DELIMITER);

            return makeCoreCoords(tokens).map(cco -> {
                int changeLength;
                VariantType vt = cco.variantType;
                switch (vt.baseType()) {
                    case DEL:
                        changeLength = cco.begin.pos() - cco.end.pos() + 1;
                        break;
                    case DUP:
                        changeLength = cco.end.pos() - cco.begin.pos();
                        break;
                    case INV:
                        changeLength = 0;
                        break;
                    default:
                        // shouldn't happen since we check in `makeCoreCoords()`
                        LOGGER.warn("Unsupported variant type {}", vt);
                        throw new RuntimeException();
                }
                return SymbolicVariant.zeroBased(cco.contig, cco.id, cco.begin, cco.end, "N", '<' + vt.baseType().name() + '>', changeLength);
            });
        };

    }

    private Optional<CoreCoords> makeCoreCoords(String[] tokens) {
        Contig contig = assembly.contigByName(tokens[0]);
        if (contig == null) {
            LOGGER.warn("Unknown contig {} in line {}", tokens[0], String.join(DELIMITER, tokens));
            return Optional.empty();
        }

        VariantType variantType = VariantType.parseType(tokens[3]);
        if (!SUPPORTED_TYPES.contains(variantType.baseType())) {
            LOGGER.warn("Unsupported variant type: `{}`", variantType);
            return Optional.empty();
        }

        // coordinates
        Position begin, end;
        try {
            int beginPos = NF.parse(tokens[1]).intValue();
            begin = Position.of(beginPos);
            int endPos = NF.parse(tokens[2]).intValue();
            end = Position.of(endPos);
        } catch (ParseException e) {
            LOGGER.warn("Invalid begin/end coordinate in line {}", String.join(DELIMITER, tokens));
            return Optional.empty();
        }
        Map<Caller, RecordData> data = parseData(tokens[4], tokens[6]);

        return Optional.of(new CoreCoords(contig, begin, end, data, variantType));
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
        private final Position begin, end;
        private final Map<Caller, RecordData> dataMap;
        private final VariantType variantType;
        private final String id;

        private CoreCoords(Contig contig, Position begin, Position end, Map<Caller, RecordData> dataMap, VariantType variantType) {
            this.contig = contig;
            this.begin = begin;
            this.end = end;
            this.dataMap = dataMap;
            this.id = dataMap.entrySet().stream()
                    .map(e -> String.format("%s(%s)", e.getKey().shortName(), e.getValue().id()))
                    .sorted()
                    .collect(Collectors.joining(";"));
            this.variantType = variantType;
        }

        int medianDepth() {
            List<Integer> depths = dataMap.values().stream()
                    .map(RecordData::depth)
                    .sorted()
                    .collect(Collectors.toList());

            if (depths.isEmpty()) {
                return -1;
            }
            int middle = depths.size() / 2;
            if (depths.size() % 2 == 0) {
                // even number of depths - return rounded arithmetic mean of the two middle elements
                return Math.round(((float) depths.get(middle - 1) + depths.get(middle)) / 2);
            } else {
                return depths.get(middle);
            }
        }

        int minDepth() {
            return dataMap.values().stream()
                    .mapToInt(RecordData::depth)
                    .min()
                    .orElse(MISSING_DEPTH_PLACEHOLDER);
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

        public String id() {
            return id;
        }

        public int depth() {
            return depth;
        }

        public String filter() {
            return filter;
        }

        public String genotype() {
            return genotype;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            RecordData that = (RecordData) o;
            return depth == that.depth &&
                    Objects.equals(id, that.id) &&
                    Objects.equals(filter, that.filter) &&
                    Objects.equals(genotype, that.genotype);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, depth, filter, genotype);
        }
    }
}
