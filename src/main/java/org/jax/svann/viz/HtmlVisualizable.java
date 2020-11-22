package org.jax.svann.viz;

import org.jax.svann.genomicreg.Enhancer;
import org.jax.svann.hpo.HpoDiseaseSummary;
import org.jax.svann.overlap.Overlap;
import org.jax.svann.priority.SvPriority;
import org.jax.svann.reference.*;
import org.jax.svann.reference.genome.Contig;
import org.jax.svann.reference.transcripts.SvAnnTxModel;

import java.util.ArrayList;
import java.util.List;

public class HtmlVisualizable implements Visualizable {

    /**
     * Representation of the structural variant as it came from the VCF file.
     */
    private final StructuralVariant rearrangement;

    private final SvPriority svPriority;

    public HtmlVisualizable(StructuralVariant rearrangement, SvPriority svPriority) {
        this.rearrangement = rearrangement;
        this.svPriority = svPriority;
    }

    private HtmlLocation getDeletionLocation(SequenceRearrangement rearrangement) {
        Contig chrom = rearrangement.getLeftmostBreakend().getContig();
        int begin = rearrangement.getLeftmostPosition();
        int end = rearrangement.getRightmostPosition();
        return new HtmlLocation(chrom, begin, end);
    }

    private HtmlLocation getInsertionLocation(SequenceRearrangement rearrangement) {
        int begin = rearrangement.getLeftmostPosition();
        int end = rearrangement.getRightmostPosition();
        Contig chrom = rearrangement.getLeftmostBreakend().getContig();
        return new HtmlLocation(chrom, begin, end);
    }

    private HtmlLocation getInversionLocation(SequenceRearrangement rearrangement) {
        Contig chrom = rearrangement.getLeftmostBreakend().getContig();
        int begin = rearrangement.getLeftmostPosition();
        int end = rearrangement.getRightmostPosition();
        return new HtmlLocation(chrom, begin, end);
    }

    private List<HtmlLocation> getTranslocationLocations(SequenceRearrangement rearrangement) {
        List<HtmlLocation> locations = new ArrayList<>();
        List<Adjacency> adjacencies = rearrangement.getAdjacencies();
        if (adjacencies.size() != 1) {
            System.err.println("Malformed insertion adjacency list with size " + adjacencies.size());
        }
        Adjacency insertion = adjacencies.get(0);
        Breakend left = insertion.getStart();
        Breakend right = insertion.getEnd();
        locations.add(new HtmlLocation(left.getContig(), left.getPosition()));
        locations.add(new HtmlLocation(right.getContig(), right.getPosition()));
        return locations;
    }


    @Override
    public StructuralVariant getRearrangement() {
        return this.rearrangement;
    }

    @Override
    public String getType() {
        return rearrangement.getType().toString();
    }


    @Override
    public String getImpact() {
        return svPriority.getImpact().toString();
    }

    @Override
    public boolean hasPhenotypicRelevance() {
        return this.svPriority.hasPhenotypicRelevance();
    }

    @Override
    public List<HpoDiseaseSummary> getDiseaseSummaries() {
        return this.svPriority.getDiseases();
    }

    @Override
    public List<SvAnnTxModel> getTranscripts() {
        return new ArrayList<>(this.svPriority.getAffectedTranscripts());
    }

    /**
     * Count up the number of unique (distinct) genes affected by this structural variant.
     * @return
     */
    @Override
    public int getGeneCount() {
        return (int)this.svPriority.getAffectedTranscripts()
                .stream()
                .map(SvAnnTxModel::getGeneSymbol)
                .distinct()
                .count();
    }

    @Override
    public List<Enhancer> getEnhancers() {
        return this.svPriority.getAffectedEnhancers();
    }

    /**
     * Return strings for display of the format chr3:123-456
     * We return two strings for translocations.
     * @return
     */
    @Override
    public List<HtmlLocation> getLocations() {
        List<HtmlLocation> locs = new ArrayList<>();
        if (rearrangement.getType() == SvType.DELETION) {
            locs.add(getDeletionLocation(rearrangement));
        } else if (rearrangement.getType() == SvType.INSERTION) {
            locs.add(getInsertionLocation(rearrangement));
        } else if (rearrangement.getType() == SvType.TRANSLOCATION) {
            locs.addAll(getTranslocationLocations(rearrangement));
        } else if (rearrangement.getType() == SvType.DUPLICATION) {
            locs.add(getDeletionLocation(rearrangement)); // works for both
        } else if (rearrangement.getType() == SvType.INVERSION) {
            locs.add(getInversionLocation(rearrangement));
        }
        return locs;
    }

    @Override
    public List<Overlap> getOverlaps() {
        return svPriority.getOverlaps();
    }
}
