package org.jax.svann.reference.transcripts;

import de.charite.compbio.jannovar.reference.GenomeInterval;
import de.charite.compbio.jannovar.reference.TranscriptModel;
import org.jax.svann.reference.GenomicPosition;
import org.jax.svann.reference.GenomicRegion;
import org.jax.svann.reference.Strand;
import org.jax.svann.reference.genome.Contig;
import org.jax.svann.reference.genome.GenomeAssembly;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Class for remapping Jannovar {@link de.charite.compbio.jannovar.reference.TranscriptModel} to our domain model.
 */
class JannovarTxMapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(JannovarTxMapper.class);

    private final GenomeAssembly assembly;

    JannovarTxMapper(GenomeAssembly assembly) {
        this.assembly = assembly;
    }

    Optional<SvAnnTxModel> remap(TranscriptModel tm) {
        String contigName = tm.getTXRegion().getRefDict().getContigIDToName().get(tm.getChr());
        Optional<Contig> cnOpt = assembly.getContigByName(contigName);
        if (cnOpt.isEmpty()) {
            LOGGER.warn("Unknown contig: `{}` in transcript `{}`", contigName, tm.getAccession());
            return Optional.empty();
        }
        Contig contig = cnOpt.get();

        // region spanned by exons & introns, including UTRs
        GenomeInterval txRegion = tm.getTXRegion();
        Strand strand = txRegion.getStrand().isForward()
                ? Strand.FWD
                : Strand.REV;

        // these coordinates are already adjusted to the appropriate strand
        GenomicPosition txStart = new TxGenomicPosition(contig, txRegion.getBeginPos(), strand);
        GenomicPosition txEnd = new TxGenomicPosition(contig, txRegion.getEndPos(), strand);

        GenomeInterval cdsRegion = tm.getCDSRegion();
        GenomicPosition cdsStart = new TxGenomicPosition(contig, cdsRegion.getBeginPos(), strand);
        GenomicPosition cdsEnd = new TxGenomicPosition(contig, cdsRegion.getEndPos(), strand);

        // process exons
        List<GenomicRegion> exons = new ArrayList<>();
        for (GenomeInterval exon : tm.getExonRegions()) {
            GenomicPosition start = new TxGenomicPosition(contig, exon.getBeginPos(), strand);
            GenomicPosition end = new TxGenomicPosition(contig, exon.getEndPos(), strand);
            exons.add(new TxGenomicRegion(start, end));
        }

        return Optional.of(new SvAnnTxModel(tm.getAccession(), tm.getGeneSymbol(), txStart, txEnd, cdsStart, cdsEnd, exons));
    }
}
