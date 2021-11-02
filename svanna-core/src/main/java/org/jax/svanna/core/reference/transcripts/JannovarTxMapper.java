package org.jax.svanna.core.reference.transcripts;

import de.charite.compbio.jannovar.reference.GenomeInterval;
import de.charite.compbio.jannovar.reference.TranscriptModel;
import org.jax.svanna.core.reference.CodingTranscript;
import org.jax.svanna.core.reference.Transcript;
import org.monarchinitiative.svart.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Class for remapping Jannovar {@link TranscriptModel} to our domain model.
 */
class JannovarTxMapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(JannovarTxMapper.class);

    private static final CoordinateSystem CS = CoordinateSystem.zeroBased();

    private final GenomicAssembly assembly;

    JannovarTxMapper(GenomicAssembly assembly) {
        this.assembly = assembly;
    }

    Optional<Transcript> remap(TranscriptModel tm) {
        String contigName = tm.getTXRegion().getRefDict().getContigIDToName().get(tm.getChr());
        Contig contig = assembly.contigByName(contigName);
        if (contig == null) {
            LOGGER.warn("Unknown contig: `{}` in transcript `{}`", contigName, tm.getAccession());
            return Optional.empty();
        }

        // region spanned by exons & introns, including UTRs
        GenomeInterval txRegion = tm.getTXRegion();
        Strand strand = txRegion.getStrand().isForward()
                ? Strand.POSITIVE
                : Strand.NEGATIVE;

        // process exons
        List<Coordinates> exons = tm.getExonRegions().stream()
                .sequential()
                .map(exon -> Coordinates.of(CS, exon.getBeginPos(), exon.getEndPos()))
                .collect(Collectors.toUnmodifiableList());

        // these coordinates are already adjusted to the appropriate strand
        Transcript tx;
        if (tm.isCoding()) {
            GenomeInterval cdsRegion = tm.getCDSRegion();
            tx = CodingTranscript.of(contig, strand, CS,
                    txRegion.getBeginPos(), txRegion.getEndPos(),
                    tm.getAccession(), exons, cdsRegion.getBeginPos(), cdsRegion.getEndPos());
        } else {
            tx = Transcript.noncoding(contig, strand, CS, txRegion.getBeginPos(), txRegion.getEndPos(),
                    tm.getAccession(), exons);
        }

        return Optional.of(tx);
    }
}
