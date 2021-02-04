package org.jax.svanna.core.reference.transcripts;

import de.charite.compbio.jannovar.reference.GenomeInterval;
import de.charite.compbio.jannovar.reference.TranscriptModel;
import org.jax.svanna.core.reference.Transcript;
import org.monarchinitiative.svart.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Class for remapping Jannovar {@link TranscriptModel} to our domain model.
 */
class JannovarTxMapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(JannovarTxMapper.class);

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

        // these coordinates are already adjusted to the appropriate strand
        GenomeInterval cdsRegion = tm.getCDSRegion();
        int cdsStart = cdsRegion.getBeginPos();
        int cdsEnd = cdsRegion.getEndPos();

        // process exons
        List<GenomicRegion> exons = new ArrayList<>();
        for (GenomeInterval exon : tm.getExonRegions()) {
            exons.add(GenomicRegion.of(contig, strand, CoordinateSystem.zeroBased(), Position.of(exon.getBeginPos()), Position.of(exon.getEndPos())));
        }

        return Optional.of(
                Transcript.of(
                        contig, strand, CoordinateSystem.zeroBased(), txRegion.getBeginPos(), txRegion.getEndPos(),
                        cdsStart, cdsEnd, tm.getAccession(), tm.getGeneSymbol(), tm.isCoding(), exons));
    }
}
