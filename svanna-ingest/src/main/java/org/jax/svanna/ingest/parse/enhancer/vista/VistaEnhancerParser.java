package org.jax.svanna.ingest.parse.enhancer.vista;

import org.jax.svanna.core.landscape.Enhancer;
import org.jax.svanna.core.landscape.EnhancerSource;
import org.jax.svanna.core.landscape.EnhancerTissueSpecificity;
import org.jax.svanna.db.landscape.BaseEnhancer;
import org.jax.svanna.ingest.hpomap.HpoMapping;
import org.jax.svanna.ingest.parse.IngestRecordParser;
import org.monarchinitiative.phenol.ontology.data.Term;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.svart.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class VistaEnhancerParser implements IngestRecordParser<Enhancer> {

    private static final Logger LOGGER = LoggerFactory.getLogger(VistaEnhancerParser.class);

    private static final Pattern TISSUE_PATTERN = Pattern.compile("(?<termName>[\\w\\s]+)\\[(?<termId>[\\w:]+)]");

    // Unlike FANTOM5, Vista enhancers do not report read counts, we use a global constant instead.
    private static final double VISTA_TISSUE_SCORE = .1;

    // Vista enhancers do not allow to calculate tissue specificity, we use a global constant instead.
    private static final double VISTA_TAU = .1;

    private final GenomicAssembly assembly;

    private final Map<TermId, HpoMapping> uberonToHpoMap;

    private final Path vistaPath;

    public VistaEnhancerParser(GenomicAssembly assembly, Path vistaPath, Map<TermId, HpoMapping> uberonToHpoMap) {
        this.assembly = assembly;
        this.uberonToHpoMap = uberonToHpoMap;
        this.vistaPath = vistaPath;
    }

    @Override
    public Stream<? extends Enhancer> parse() throws IOException {
        return Files.newBufferedReader(vistaPath).lines()
                .skip(1) // header
                .map(toEnhancer())
                .filter(Optional::isPresent)
                .map(Optional::get);
    }

    private Function<String, Optional<? extends Enhancer>> toEnhancer() {
        return line -> {
            String[] fields = line.split("\t");
            if (fields.length < 5) {
                if (LOGGER.isWarnEnabled())
                    LOGGER.warn("Found {} columns (5 required) in line `{}`", fields.length, line);
                return Optional.empty();
            }
            String id = fields[0];
            Contig contig = assembly.contigByName(fields[1]);
            if (contig == Contig.unknown()) {
                if (LOGGER.isWarnEnabled()) LOGGER.warn("Unknown contig {} in line `{}`", fields[1], line);
                return Optional.empty();
            }

            Position start = Position.of(Integer.parseInt(fields[2]));
            Position end = Position.of(Integer.parseInt(fields[3]));

            // fields[4] is like neural tube[UBERON:0001049];presumptive hindbrain[UBERON:0007277];...
            String[] tissueList = fields[4].split(";");
            Set<EnhancerTissueSpecificity> tissues = new HashSet<>();
            for (String tissue : tissueList) {
                Matcher matcher = TISSUE_PATTERN.matcher(tissue);
                if (!matcher.matches()) {
                    if (LOGGER.isWarnEnabled()) LOGGER.warn("Malformed tissue field {} in line `{}`", tissue, line);
                    continue;
                }

                Term uberon = Term.of(matcher.group("termId"), matcher.group("termName"));
                if (!uberonToHpoMap.containsKey(uberon.getId())) {
                    if (LOGGER.isWarnEnabled()) LOGGER.warn("{} not found", uberon.getId().getValue());
                    continue;
                }
                HpoMapping hpoMapping = uberonToHpoMap.get(uberon.getId());
                Term hpo = Term.of(hpoMapping.getHpoTermId(), hpoMapping.getHpoLabel());

                EnhancerTissueSpecificity specificity = EnhancerTissueSpecificity.of(uberon, hpo, VISTA_TISSUE_SCORE);
                tissues.add(specificity);
            }
            return Optional.of(BaseEnhancer.of(contig, Strand.POSITIVE, CoordinateSystem.oneBased(), start, end,
                    id, EnhancerSource.VISTA, true, VISTA_TAU, tissues));
        };
    }
}
