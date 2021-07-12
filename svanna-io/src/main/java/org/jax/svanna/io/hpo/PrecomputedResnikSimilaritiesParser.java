package org.jax.svanna.io.hpo;

import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.jax.svanna.core.LogUtils;
import org.jax.svanna.core.hpo.TermPair;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class PrecomputedResnikSimilaritiesParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(PrecomputedResnikSimilaritiesParser.class);

    private PrecomputedResnikSimilaritiesParser() {}


    public static Map<TermPair, Double> readSimilarities(Path similarityPath) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new GzipCompressorInputStream(new FileInputStream(similarityPath.toFile()))))) {
            return reader.lines()
                    .skip(1) // header
                    .map(toTermPairAndScore())
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toMap(DataCarrier::termPair, DataCarrier::similarity));
        }
    }

    private static Function<String, Optional<DataCarrier>> toTermPairAndScore() {
        return line -> {
            try {
                String[] token = line.split(",");
                TermPair termPair = TermPair.of(TermId.of(token[0]), TermId.of(token[1]));
                double similarity = Double.parseDouble(token[2]);
                return Optional.of(new DataCarrier(termPair, similarity));
            } catch (Exception e) {
                LogUtils.logWarn(LOGGER, "Unable to parse line `{}`", line);
                return Optional.empty();
            }
        };
    }

    private static class DataCarrier {
        private final TermPair pair;
        private final Double similarity;

        private DataCarrier(TermPair pair, Double similarity) {
            this.pair = pair;
            this.similarity = similarity;
        }

        public TermPair termPair() {
            return pair;
        }

        public Double similarity() {
            return similarity;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DataCarrier that = (DataCarrier) o;
            return Objects.equals(pair, that.pair) && Objects.equals(similarity, that.similarity);
        }

        @Override
        public int hashCode() {
            return Objects.hash(pair, similarity);
        }

    }

}
