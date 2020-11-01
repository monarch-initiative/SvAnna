package org.jax.svann.viz;

import org.jax.svann.except.SvAnnRuntimeException;
import org.jax.svann.priority.SvImpact;
import org.jax.svann.priority.SvPriority;
import org.jax.svann.reference.Adjacency;
import org.jax.svann.reference.Breakend;
import org.jax.svann.reference.SequenceRearrangement;
import org.jax.svann.reference.SvType;
import org.jax.svann.reference.genome.Contig;

import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

public class HtmlVisualizable implements  Visualizable {

    final private SvType svType;

    final private SvImpact svImpact;

    final private List<Adjacency> adjacencyList;

    final private Map<String, String> locationMap;

    final private SvPriority svPriority;


    private SequenceRearrangement rearrangement;

    public HtmlVisualizable(SvPriority svPriority) {
        svImpact = svPriority.getImpact();
        adjacencyList = rearrangement.getAdjacencies();
        svType = rearrangement.getType();
        this.svPriority = svPriority;
        this.rearrangement = svPriority.getRearrangement();
        this.locationMap =  initializeLocationStrings();

    }

    private String getDeletionLocationString(SequenceRearrangement rearrangement) {
        List<Adjacency> adjacencies = rearrangement.getAdjacencies();
        if (adjacencies.size() != 1) {
            throw new SvAnnRuntimeException("Malformed deletion adjacency list with size " + adjacencies.size());
        }
        Adjacency deletion = adjacencies.get(0);

        Breakend left = deletion.getLeft();
        Breakend right = deletion.getRight();
        Contig chrom = left.getContig();
        int id = chrom.getId();
        int begin = left.getBegin();
        int end = right.getEnd();
        return String.format("chr%s:%d-%d", id, begin,end);
    }

    /**
     * Create strings such as chr3:123-432 for the structural variant. For some kinds of SV, we will
     * want to return multiple rows.
     * @return a map of key value pairs representing the locations
     */
    private Map<String, String> initializeLocationStrings() {
        SortedMap<String, String> sortedMap = new TreeMap<>();
        if (this.rearrangement.getType() == SvType.DELETION) {
            String loc = getDeletionLocationString(this.rearrangement);
            sortedMap.put("location", loc);
        } else {
            sortedMap.put("todo #1", "A");
            sortedMap.put("todo #2", "B");
        }
        return sortedMap;
    }


    @Override
    public String getType() {
        return svType.toString();
    }

    @Override
    public Map<String, String> getLocationStrings() {
        return this.locationMap;
    }

    @Override
    public String getImpact() {
        return svImpact.toString();
    }

    @Override
    public  boolean hasPhenotypicRelevance() {
        return this.svPriority.hasPhenotypicRelevance();
    }
}
