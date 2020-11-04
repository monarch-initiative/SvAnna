package org.jax.svann.viz;

import org.jax.svann.except.SvAnnRuntimeException;
import org.jax.svann.hpo.HpoDiseaseSummary;
import org.jax.svann.overlap.Overlap;
import org.jax.svann.priority.SvPriority;
import org.jax.svann.reference.Adjacency;
import org.jax.svann.reference.Breakend;
import org.jax.svann.reference.SequenceRearrangement;
import org.jax.svann.reference.SvType;
import org.jax.svann.reference.genome.Contig;

import java.util.ArrayList;
import java.util.List;

public class HtmlVisualizable implements Visualizable {

    /**
     * Representation of the structural variant as it came from the VCF file.
     */
    private final SequenceRearrangement rearrangement;

    private final SvPriority svPriority;

    public HtmlVisualizable(SequenceRearrangement rearrangement, SvPriority svPriority) {
        this.rearrangement = rearrangement;
        this.svPriority = svPriority;
    }

    private HtmlLocation getDeletionLocation(SequenceRearrangement rearrangement) {
        List<Adjacency> adjacencies = rearrangement.getAdjacencies();
        if (adjacencies.size() != 1) {
            throw new SvAnnRuntimeException("Malformed deletion adjacency list with size " + adjacencies.size());
        }
        Adjacency deletion = adjacencies.get(0);
        Breakend left = deletion.getStart();
        Breakend right = deletion.getEnd();
        Contig chrom = left.getContig();
        int begin = left.getPosition();
        int end = right.getPosition();
        return new HtmlLocation(chrom, begin, end);
    }


    @Override
    public SequenceRearrangement getRearrangement() {
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

    public List<HpoDiseaseSummary> getDiseaseSummaries() {
        return this.svPriority.getDiseases();
    }

    /**
     * Return strings for display of the format chr3:123-456
     *
     * @return
     */
    @Override
    public List<HtmlLocation> getLocations() {
        List<HtmlLocation> locs = new ArrayList<>();
        if (rearrangement.getType() == SvType.DELETION) {
            locs.add(getDeletionLocation(rearrangement));
        } else if (rearrangement.getType() == SvType.INSERTION) {
            int c = 42;
            int y = 32;
        }


        return locs;
    }

    @Override
    public List<Overlap> getOverlaps() {
        return svPriority.getOverlaps();
    }
}
