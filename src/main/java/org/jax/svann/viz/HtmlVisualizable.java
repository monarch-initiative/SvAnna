package org.jax.svann.viz;

import org.jax.svann.except.SvAnnRuntimeException;
import org.jax.svann.hpo.HpoDiseaseSummary;
import org.jax.svann.priority.SvImpact;
import org.jax.svann.priority.SvPriority;
import org.jax.svann.reference.Adjacency;
import org.jax.svann.reference.Breakend;
import org.jax.svann.reference.SequenceRearrangement;
import org.jax.svann.reference.SvType;
import org.jax.svann.reference.genome.Contig;

import java.util.*;

public class HtmlVisualizable implements  Visualizable {

    final private SvType svType;

    final private SvImpact svImpact;

    final private SvPriority svPriority;

    /**
     * Representation of the structural variant as it came from the VCF file.
     */
    private SequenceRearrangement rearrangement;

    /**
     *
     * @param svPriority
     */
    public HtmlVisualizable(SvPriority svPriority) {
        this.svImpact = svPriority.getImpact();
        this.rearrangement = svPriority.getRearrangement();
        this.svType = this.rearrangement.getType();
        this.svPriority = svPriority;
    }

    private HtmlLocation getDeletionLocation(SequenceRearrangement rearrangement) {
        List<Adjacency> adjacencies = rearrangement.getAdjacencies();
        if (adjacencies.size() != 1) {
            throw new SvAnnRuntimeException("Malformed deletion adjacency list with size " + adjacencies.size());
        }
        Adjacency deletion = adjacencies.get(0);
        Breakend left = deletion.getLeft();
        Breakend right = deletion.getRight();
        Contig chrom = left.getContig();
        int begin = left.getBegin();
        int end = right.getEnd();
        return new HtmlLocation(chrom, begin,end);
    }


    @Override
    public SequenceRearrangement getRearrangement() {
        return this.rearrangement;
    }

    @Override
    public String getType() {
        return svType.toString();
    }


    @Override
    public String getImpact() {
        return svImpact.toString();
    }

    @Override
    public boolean hasPhenotypicRelevance() {
        return this.svPriority.hasPhenotypicRelevance();
    }

    public List<HpoDiseaseSummary> getDiseaseSummaries() { return this.svPriority.getDiseases(); }

    /**
     * Return strings for display of the format chr3:123-456
     * @return
     */
    @Override
    public List<HtmlLocation> getLocations() {
        List<HtmlLocation> locs = new ArrayList<>();
        if (rearrangement.getType() == SvType.DELETION) {
            locs.add(getDeletionLocation(rearrangement));
        } else  if (rearrangement.getType() == SvType.INSERTION) {
            int c = 42;
            int y = 32;
        }



        return locs;
    }
}
