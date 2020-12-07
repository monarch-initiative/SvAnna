package org.jax.svanna.core.reference.transcripts;

import de.charite.compbio.jannovar.reference.GenomeInterval;
import de.charite.compbio.jannovar.reference.TranscriptModel;
import org.jax.svanna.core.reference.Transcript;
import org.monarchinitiative.variant.api.*;
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
        GenomicPosition txStart = GenomicPosition.zeroBased(contig, strand, Position.of(txRegion.getBeginPos()));
        GenomicPosition txEnd = GenomicPosition.oneBased(contig, strand, Position.of(txRegion.getEndPos()));

        GenomeInterval cdsRegion = tm.getCDSRegion();
        GenomicPosition cdsStart = GenomicPosition.zeroBased(contig, strand, Position.of(cdsRegion.getBeginPos()));
        GenomicPosition cdsEnd = GenomicPosition.oneBased(contig, strand, Position.of(cdsRegion.getEndPos()));

        // process exons
        List<GenomicRegion> exons = new ArrayList<>();
        for (GenomeInterval exon : tm.getExonRegions()) {
            exons.add(GenomicRegion.zeroBased(contig, strand, Position.of(exon.getBeginPos()), Position.of(exon.getEndPos())));
        }

        return Optional.of(Transcript.of(contig, txRegion.getBeginPos(), txRegion.getEndPos(), strand, CoordinateSystem.ZERO_BASED,
                cdsStart, cdsEnd, tm.getAccession(), tm.getGeneSymbol(), tm.isCoding(),
                exons));
    }
}
