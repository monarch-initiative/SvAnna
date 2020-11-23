package org.jax.svann.priority;

import org.jax.svann.genomicreg.Enhancer;
import org.jax.svann.hpo.GeneWithId;
import org.jax.svann.hpo.HpoDiseaseSummary;
import org.jax.svann.overlap.EnhancerOverlapper;
import org.jax.svann.overlap.Overlap;
import org.jax.svann.overlap.OverlapType;
import org.jax.svann.overlap.Overlapper;
import org.jax.svann.reference.*;
import org.jax.svann.reference.genome.Contig;
import org.jax.svann.reference.transcripts.SvAnnTxModel;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * This class implements both sequence-based and phenotype-based prioritization. The sequence-based prioritizations
 * are different for each SV type, but each results in potentially empty lists of affected transcripts and enhancers.
 * The class then checks if any of the affected transcripts corresponds to a gene in {@link #diseaseSummaryMap}, and
 * if an enhancer is associated with an HPO term in {@link #relevantHpoIdsForEnhancers}. If so, we call the SV
 * {@code phenotypically relevant}. If an SV is relevant, then the IMPACT calculated by the sequence-based prioritization
 * is not changed. If not, then we apply the following rules
 * <ol>
 *     <li>HIGH-IMPACT is changed to INTERMEDIATE-IMPACT</li>
 *     <li>INTERMEDIATE-IMPACT is changed to LOW IMPACT</li>
 * </ol>
 */
public class PrototypeSvPrioritizer implements SvPrioritizer {

    private static final Logger LOGGER = LoggerFactory.getLogger(PrototypeSvPrioritizer.class);

    /**
     * In the first pass, we just look at sequence and do not analyze diseases, so this is the empty set.
     */
    private static final Set<HpoDiseaseSummary> EMPTYSET = Set.of();


    /**
     * The overlapper tests overlap of structural variants with transcripts.
     */
    private final Overlapper overlapper;
    /**
     * The enhancer overlapper tests overlap of structural variants with enhancers.
     */
    private final EnhancerOverlapper enhancerOverlapper;
    /**
     * Key -- gene symbol, value, {@link GeneWithId} object with symbol and id
     */
    private final Map<String, GeneWithId> geneSymbolMap;

    /**
     * Collection of top level HPO ids that are equal to or ancestors of terms used to annotate the clinical
     * features of the individual being investigated.
     */
    private final Set<TermId> relevantHpoIdsForEnhancers;

    private final Set<TermId> patientPhenotypeTerms;
    /**
     *  map with key=GeneId, value=Set of {@link HpoDiseaseSummary} objects. Only contains data for genes/diseases
     *  determined to be phenotypically relevant and is empty (but not null) if the user does not enter HPO terms.
     */
    private final Map<TermId, Set<HpoDiseaseSummary>> diseaseSummaryMap;
    /**
     * If an SV affects more genes than this, we assume it is likely to be an artifact, and downweight its
     * impact.
     */
    private final int maxGenes;

    /**
     *
     * @param overlapper object that calculates overlap of SVs with transcripts
     * @param enhancerOverlapper object that calculates overlap of SVs with enhancers
     * @param geneSymbolMap Map that allows us to go from gene symbol to GeneID TODO -- this is not robust, consider refactor
     * @param patientPhenotypeTerms Terms observed in patient
     * @param relevantHpoIdsForEnhancers HPO TermIds used to describe enhancers (contains only HPO terms equal/ancestors to patient terms)
     * @param diseaseSummaryMap key:NCBI Gene id; value:set of associated diseases (set contains only phenotypically relevant diseases)
     */
    public PrototypeSvPrioritizer(Overlapper overlapper,
                                  EnhancerOverlapper enhancerOverlapper,
                                  Map<String, GeneWithId> geneSymbolMap,
                                  Set<TermId> patientPhenotypeTerms,
                                  Set<TermId> relevantHpoIdsForEnhancers,
                                  Map<TermId, Set<HpoDiseaseSummary>> diseaseSummaryMap,
                                  int maxGenes) {
        this.geneSymbolMap = geneSymbolMap;
        this.overlapper = overlapper;
        this.enhancerOverlapper = enhancerOverlapper;
        this.patientPhenotypeTerms = patientPhenotypeTerms;
        this.relevantHpoIdsForEnhancers = relevantHpoIdsForEnhancers;
        this.diseaseSummaryMap = diseaseSummaryMap;
        this.maxGenes = maxGenes;
    }

    /**
     * Constructor with default value of 100 for the Max genes parameter.
     * @param overlapper
     * @param enhancerOverlapper
     * @param geneSymbolMap
     * @param patientPhenotypeTerms
     * @param relevantHpoIdsForEnhancers
     * @param diseaseSummaryMap
     */
    public PrototypeSvPrioritizer(Overlapper overlapper,
                                  EnhancerOverlapper enhancerOverlapper,
                                  Map<String, GeneWithId> geneSymbolMap,
                                  Set<TermId> patientPhenotypeTerms,
                                  Set<TermId> relevantHpoIdsForEnhancers,
                                  Map<TermId, Set<HpoDiseaseSummary>> diseaseSummaryMap) {
        this(overlapper, enhancerOverlapper, geneSymbolMap, patientPhenotypeTerms, relevantHpoIdsForEnhancers, diseaseSummaryMap, 100);
    }

    @Override
    public SvPriority prioritize(SequenceRearrangement rearrangement) {
        /*
        For each case we need to figure out the affected transcripts/genes.
        Then, we figure out the impact on the sequence (high/medium/low) and phenotype relevance.

        Sequence impact category:
        - use overlapper to get the overlapping transcripts
        - if overlap type is intergenic:
          - if variant disrupts an enhancer with tissue relevance -> medium sequence impact
            - else low
        - if overlap type is single/multiple/entire tx affected -> high sequence impact
        report the most severe sequence impact category

        Phenotypic relevance:
        - the affected gene is among the relevant genes -> relevant
        - translocation that removes enhancer from a phenotypically relevant gene
        - else not

        Note that the rules for getting "overlap" depends on SvType
         */
        switch (rearrangement.getType()) {
            case DELETION:
                return prioritizeDeletion(rearrangement);
            case INSERTION:
                return prioritizeInsertion(rearrangement);
            case INVERSION:
                return prioritizeInversion(rearrangement);
            case TRANSLOCATION:
                return prioritizeTranslocation(rearrangement);
            case DUPLICATION:
                return prioritizeDuplication(rearrangement);
            default:
                LOGGER.warn("Prioritization of {} is not yet supported", rearrangement.getType());
                return SvPriority.unknown();
        }
    }


    /**
     * This can be used to prioritize the phenotypic relevance of SVs that overlap
     * part of a gene that is phenotypically relevant. It should be used for SVs such
     * as deletions where overlapping with the CDS of a gene can be high priority.
     * We also prioritize enhancers at this step

     * @return a phenotypically prioritized {@link SvPriority} object
     */
    SvPriority prioritizeSimpleOverlapByPhenotype(SvImpact svImpact,
                                                  Set<SvAnnTxModel> affectedTranscripts,
                                                  Set<GeneWithId> affectedGeneIds,
                                                  List<Enhancer> affectedEnhancers,
                                                  List<Overlap> olaps) {

        List<HpoDiseaseSummary> diseaseList = new ArrayList<>();
        SvImpact phenotypeImpact;
        for (var gene : affectedGeneIds) {
            TermId tid = gene.getGeneId();
            if (diseaseSummaryMap.containsKey(tid)) {
                diseaseList.addAll(diseaseSummaryMap.get(tid));
            }
        }
        // decrement severity of the SV if it is not relevant to the disease
        phenotypeImpact = diseaseList.isEmpty()
                ? svImpact.decrementSeverity()
                : svImpact;

        if (affectedTranscripts.isEmpty() && affectedEnhancers.size() > 0) {
            // in this case, we have prioritize the SV based on overlap with
            // an enhancer. We will rate the SV high if the enhancer is
            // annotated to a tissue that has phenotypic relevant
            for (Enhancer e : affectedEnhancers) {
                if (this.relevantHpoIdsForEnhancers.contains(e.getHpoId())) {
                    // if there is at least one affected enhancer, then the impact is high
                    // no matter what else there is
                    phenotypeImpact = SvImpact.HIGH;
                    break;
                }
            }
        }

        return new DefaultSvPriority(phenotypeImpact, affectedTranscripts,
                affectedGeneIds, affectedEnhancers, olaps, diseaseList);
    }

    /**
     * When prioritizing a deletion, we start with default {@link SvImpact} for given {@link OverlapType} and adjust the
     * impact to the following if:
     * <ul>
     *     <li><em>HIGH</em> - deletion affects CDS of >=1 gene or deletion affects >1 gene or a phenotypically relevant
     *     enhancer or intronic and <= 25 bases away from the closest exon</li>
     *     <li><em>INTERMEDIATE</em> - deletion affects 1 gene but not the CDS or deletion affects some enhancer
     *     or deletion is intronic and <= 100 bases away from the closest exon</li>
     * </ul>
     *
     * @param deletion deletion to evaluate
     * @return priority
     */
    private SvPriority prioritizeDeletion(SequenceRearrangement deletion) {
        // find the gene/transcript with the most deleterious OverlapType
        List<Overlap> overlaps = overlapper.getOverlapList(deletion);
        OverlapType highestOT = getHighestOverlapType(overlaps);

        // select the relevant genes and transcripts
        Set<String> affectedGeneSymbols = overlaps.stream()
                .map(Overlap::getGeneSymbol)
                .collect(Collectors.toSet());
        Set<GeneWithId> geneWithIdsSet = new HashSet<>();
        for (String symbol : affectedGeneSymbols) {
            if (geneSymbolMap.containsKey(symbol)) {
                geneWithIdsSet.add(geneSymbolMap.get(symbol));
            }
        }
        Set<SvAnnTxModel> affectedTranscripts = overlaps.stream()
                .map(Overlap::getTranscriptModel)
                .collect(Collectors.toSet());

        // start figuring out the impact
        SvImpact impact = highestOT.defaultSvImpact();


        // now the impact might still be HIGH if the deletion overlaps with a phenotypically relevant enhancer
        // impact is INTERMEDIATE if the deletion overlaps with some enhancer
        List<Enhancer> enhancers = enhancerOverlapper.getEnhancerOverlaps(deletion);
        if (!enhancers.isEmpty()) {
            SvImpact enhancerImpact = enhancers.stream().anyMatch(enhancer -> relevantHpoIdsForEnhancers.contains(enhancer.getHpoId()))
                    ? SvImpact.HIGH
                    : SvImpact.INTERMEDIATE;
            if (impact != SvImpact.HIGH && enhancerImpact == SvImpact.HIGH) {
                impact = SvImpact.HIGH;
            } else if (impact != SvImpact.HIGH && enhancerImpact == SvImpact.INTERMEDIATE) {
                impact = SvImpact.INTERMEDIATE;
            }
        }
        // counts of gene regardless of relevance
        int genecount =  (int)affectedTranscripts
                .stream()
                .map(SvAnnTxModel::getGeneSymbol)
                .distinct()
                .count();

        if (genecount > maxGenes) {
            impact = impact.decrementSeverity();
            System.out.println(geneWithIdsSet.size());
        }
        return prioritizeSimpleOverlapByPhenotype(impact, affectedTranscripts, geneWithIdsSet, enhancers, overlaps);

    }

    private boolean affectedGenesRelevant(Set<GeneWithId> genesAffectedBySv) {
        return genesAffectedBySv.stream()
                .map(GeneWithId::getGeneId)
                .anyMatch(diseaseSummaryMap::containsKey);
    }

    private boolean affectedEnhancersRelevant(List<Enhancer> enhancers) {
        return enhancers.stream()
                .map(Enhancer::getHpoId)
                .anyMatch(relevantHpoIdsForEnhancers::contains);
    }


    /**
     * Sequence based insertion prioritization. Any insertion that occurs in exonic regions is considered HIGH impact
     * except for insertions into UTR regions of coding genes (Any insertion in a transcript of a non-coding gene
     * is considered high impact).
     *
     * @param insertion an insertion
     * @return Corresponding prioritization according to sequence
     */
    private SvPriority prioritizeInsertion(SequenceRearrangement insertion) {
        List<Overlap> overlaps = overlapper.getOverlapList(insertion);
        Optional<Overlap> highestImpactOverlapOpt = overlaps.stream()
                .max(Comparator.comparing(Overlap::getOverlapType));
        if (highestImpactOverlapOpt.isEmpty()) {
            // should never happen
            LOGGER.error("Could not identify highest impact overlap for insertion: {}.", insertion);
            return DefaultSvPriority.unknown();
        }
        Overlap highestImpactOverlap = highestImpactOverlapOpt.get();
        OverlapType highestOT = highestImpactOverlap.getOverlapType();
        Set<String> affectedGeneIds = overlaps.stream()
                .map(Overlap::getGeneSymbol)
                .collect(Collectors.toSet());
        Set<GeneWithId> geneWithIdsSet = new HashSet<>();
        for (String symbol : affectedGeneIds) {
            if (geneSymbolMap.containsKey(symbol)) {
                geneWithIdsSet.add(geneSymbolMap.get(symbol));
            }
        }
        Set<SvAnnTxModel> affectedTranscripts = overlaps.stream()
                .map(Overlap::getTranscriptModel)
                .collect(Collectors.toSet());

        // start figuring out the impact
        SvImpact impact = highestOT.defaultSvImpact();
        if (affectedGeneIds.size() > 1 && highestOT.isExonic()) {
            // Insertion affects >1 genes, although I'm not sure if this can actually happen
            impact = SvImpact.HIGH;
        } else if (highestOT.isExonic()) {
            // insertion into CDS is HIGH, UTR is intermediate
            impact = highestImpactOverlap.overlapsCds()
                    ? SvImpact.HIGH
                    : SvImpact.INTERMEDIATE;
        } else if (highestOT.isIntronic()) {
            // intronic insertion close to CDS has HIGH impact,
            // an insertion further away is INTERMEDIATE impact
            int distance = Math.abs(highestImpactOverlap.getDistance());
            impact = distance <= 25
                    ? SvImpact.HIGH
                    : distance <= 100
                    ? SvImpact.INTERMEDIATE
                    : SvImpact.LOW;
        }

        List<Enhancer> enhancers = enhancerOverlapper.getEnhancerOverlaps(insertion);
        if (!enhancers.isEmpty()) {
            impact = enhancers.stream().anyMatch(enhancer -> relevantHpoIdsForEnhancers.contains(enhancer.getHpoId()))
                    ? SvImpact.HIGH
                    : SvImpact.INTERMEDIATE;
        }
        return prioritizeSimpleOverlapByPhenotype(impact, affectedTranscripts, geneWithIdsSet, enhancers, overlaps);
    }

    /**
     * An inversion that affects a coding sequence of a gene is HIGH impact.
     * <p>
     * An inversion that affects intronic part only is HIGH/INTERMEDIATE/LOW impact depending on the distance to the
     * closest exon.
     * <p>
     * A non-coding inversion
     * <ul>
     *     <li>upstream of a gene that contains any enhancer is HIGH impact</li>
     *     <li>affecting 2kb upstream from a gene is HIGH impact</li>
     *     <li>with no enhancer is LOW impact</li>
     * </ul>
     *
     * @param inversion inversion to prioritize
     * @return prioritization result
     */
    private SvPriority prioritizeInversion(SequenceRearrangement inversion) {
        // Gather information
        List<Overlap> overlaps = overlapper.getOverlapList(inversion);
        Optional<Overlap> highestImpactOverlapOpt = overlaps.stream()
                .max(Comparator.comparing(Overlap::getOverlapType));
        if (highestImpactOverlapOpt.isEmpty()) {
            // should never happen
            LOGGER.error("Could not identify highest impact overlap for inversion: {}.", inversion);
            return DefaultSvPriority.unknown();
        }
        Overlap highestImpactOverlap = highestImpactOverlapOpt.get();
        OverlapType highestOT = highestImpactOverlap.getOverlapType();

        Set<String> affectedGeneIds = overlaps.stream()
                .map(Overlap::getGeneSymbol)
                .collect(Collectors.toSet());
        Set<GeneWithId> geneWithIdsSet = new HashSet<>();
        for (String symbol : affectedGeneIds) {
            if (geneSymbolMap.containsKey(symbol)) {
                geneWithIdsSet.add(geneSymbolMap.get(symbol));
            }
        }
        Set<SvAnnTxModel> affectedTranscripts = overlaps.stream()
                .map(Overlap::getTranscriptModel)
                .collect(Collectors.toSet());

        List<Enhancer> enhancers = enhancerOverlapper.getEnhancerOverlaps(inversion);

        // Start figuring out the impact
        SvImpact impact = highestOT.defaultSvImpact(); // default

        if (highestOT.isExonic() && highestOT != OverlapType.TRANSCRIPT_CONTAINED_IN_SV) {
            // (1) the inversion affects an exon
            // If an inversion completely contains a gene, we do not rank it as high impact
            // but we will look at regulatory effects
            impact = SvImpact.HIGH;
        } else if (highestOT == OverlapType.TRANSCRIPT_CONTAINED_IN_SV) {
            impact = SvImpact.INTERMEDIATE;
        } else if (highestOT.isIntronic()) {
            // (2) intronic inversion close to CDS has HIGH impact,
            // an inversion further away is INTERMEDIATE impact
            int distance = highestImpactOverlap.getDistance();
            impact = distance <= 25
                    ? SvImpact.HIGH
                    : distance <= 100
                    ? SvImpact.INTERMEDIATE
                    : SvImpact.LOW;
        } else if (highestOT.isIntergenic()) {
            // (3) intergenic inversion, let's consider promoter and enhancers
            if (highestOT.equals(OverlapType.UPSTREAM_GENE_VARIANT_500B)) {
                // promoter region
                impact = SvImpact.HIGH;
            } else if (highestOT.equals(OverlapType.UPSTREAM_GENE_VARIANT_2KB)) {
                // promoter region
                impact = SvImpact.INTERMEDIATE;
            } else {
                impact = enhancers.isEmpty()
                        ? SvImpact.LOW
                        : SvImpact.HIGH;
            }
        }
        // if the inversion already has high priority, then return it
        // otherwise look for longer range position effects
        SvPriority prio = prioritizeSimpleOverlapByPhenotype(impact, affectedTranscripts, geneWithIdsSet, enhancers, overlaps);
        if (prio.getImpact() == SvImpact.HIGH) {
            return prio;
        }
        // if the inversion is located within an intron of a gene, we will assume its effects
        // are only due to the intron. Similarly, if the inversion is upstream or downstream of the
        // it will not affect the distance to enhancers on the "other side" -- by definition,
        // enhancers are not sensitive to orientation
        if (highestOT.isIntronic() || highestOT.isDownstream() || highestOT.isUpstream()) {
            return prio;
        }
        //if we get here, we look and see if there are both relevant genes within the inversion
        // and relevant enhancers within a window, or vice version.

        final int OFFSET = 100_000; // TODO make class variable, adjust from command line
        GenomicRegion inversionRegion = getRegion(inversion);
        // check if there are genes within the inversion. If so, look for enhancers outside
        // of the inversion boundaries
       if (! affectedTranscripts.isEmpty()) {
           // in this case, at least one gene is inside the inversion
           // we look and see if there are relevant enhancers outside of the inversion
           List<Enhancer> outsideEnhancers = enhancerOverlapper.getEnhancerRegionOverlaps(inversionRegion, OFFSET);
           boolean relevantEnhancer = false;
           for (var e : outsideEnhancers) {
               if (this.relevantHpoIdsForEnhancers.contains(e.getHpoId())) {
                   relevantEnhancer = true;
                   break;
               }
           }
           if (relevantEnhancer) {
               // this can now make the gene inside the inversion be a disease gene
               List<HpoDiseaseSummary> diseases = new ArrayList<>();
               for (var gwi : geneWithIdsSet) {
                   TermId geneid = gwi.getGeneId();
                   if (this.diseaseSummaryMap.containsKey(geneid)) {
                       diseases.addAll(diseaseSummaryMap.get(geneid));
                   }
               }
               return new DefaultSvPriority(SvImpact.HIGH, affectedTranscripts, geneWithIdsSet, outsideEnhancers, overlaps, diseases);
           }
       }
           return prio; // stick to the local interpretation
    }

    /**
     * By assumption, this method can only be used for SVs on a single chromosome
     * @param rearrangement
     * @return
     */
    private GenomicRegion getRegion(SequenceRearrangement rearrangement) {
        Contig chrom = rearrangement.getLeftmostBreakend().getContig();
        int begin = rearrangement.getLeftmostPosition();
        int end = rearrangement.getRightmostPosition();
        if (begin > end) {
            int tmp = end;
            end = begin;
            begin = tmp;
        }
        GenomicPosition leftPos = StandardGenomicPosition.precise(chrom, begin, Strand.FWD);
        GenomicPosition rightPos = StandardGenomicPosition.precise(chrom, end, Strand.FWD);
        return StandardGenomicRegion.of(leftPos, rightPos);
    }


    private OverlapType getHighestOverlapType(List<Overlap> overlaps) {
        Optional<Overlap> highestImpactOverlapOpt = overlaps.stream()
                .max(Comparator.comparing(Overlap::getOverlapType));
        if (highestImpactOverlapOpt.isEmpty()) {
            // should never happen
            LOGGER.error("Could not identify highest impact overlap for list of {} overlaps.", overlaps.size());
            return OverlapType.UNKNOWN;
        }
        Overlap highestImpactOverlap = highestImpactOverlapOpt.get();
        return highestImpactOverlap.getOverlapType();
    }


    private SvPriority prioritizeTranslocation(SequenceRearrangement rearrangement) {
        // the following gets overlaps that disrupt transcripts only
        List<Overlap> overlaps = overlapper.getOverlapList(rearrangement);
        SvImpact impact = SvImpact.LOW; // default
        OverlapType otype = getHighestOverlapType(overlaps);
        Set<SvAnnTxModel> affectedTranscripts;
        Set<GeneWithId> geneWithIdsSet;
        if (!overlaps.isEmpty()) {
            Set<String> affectedGeneIds = overlaps.stream().map(Overlap::getGeneSymbol).collect(Collectors.toSet());
            geneWithIdsSet = new HashSet<>();
            for (String symbol : affectedGeneIds) {
                if (geneSymbolMap.containsKey(symbol)) {
                    geneWithIdsSet.add(geneSymbolMap.get(symbol));
                }
            }
            affectedTranscripts =
                    overlaps.stream().map(Overlap::getTranscriptModel).collect(Collectors.toSet());
            if (otype.translocationDisruptable()) {
                impact = SvImpact.HIGH;
                otype = OverlapType.TRANSCRIPT_DISRUPTED_BY_TRANSLOCATION;
            } else {
                impact = SvImpact.INTERMEDIATE;
                otype = OverlapType.TRANSLOCATION_WITHOUT_TRANSCRIPT_DISRUPTION;
            }
        } else {
            affectedTranscripts = Set.of();
            geneWithIdsSet = Set.of();
        }
        List<Enhancer> enhancers = enhancerOverlapper.getEnhancerOverlaps(rearrangement);
        if (enhancers.size() > 0) {
            impact = SvImpact.HIGH;
            otype = OverlapType.ENHANCER_DISRUPTED_BY_TRANSLOCATION;
        }
        if (impact == SvImpact.HIGH) {
            // this means that the translocation disrupts a gene, so we will
            // take gene disruption, rather than regulatory effects, to be
            // the primary mode of action of this translocation
            return prioritizeSimpleOverlapByPhenotype(impact, affectedTranscripts, geneWithIdsSet, enhancers, overlaps);
        }

        // TODO -- we need a bespoke prioritizer for translocations
        // FOR NOW there are no diseases
        return new DefaultSvPriority(impact, affectedTranscripts, geneWithIdsSet, enhancers, overlaps, List.of());
    }

    private SvPriority prioritizeDuplication(SequenceRearrangement duplication) {
        List<Overlap> overlaps = overlapper.getOverlapList(duplication);
        Optional<Overlap> highestImpactOverlapOpt = overlaps.stream()
                .max(Comparator.comparing(Overlap::getOverlapType));
        if (highestImpactOverlapOpt.isEmpty()) {
            // should never happen
            LOGGER.error("Could not identify highest impact overlap for duplication: {}.", duplication);
            return DefaultSvPriority.unknown();
        }
        Overlap highestImpactOverlap = highestImpactOverlapOpt.get();
        OverlapType highestOT = highestImpactOverlap.getOverlapType();

        Set<String> affectedGeneIds = overlaps.stream()
                .map(Overlap::getGeneSymbol)
                .collect(Collectors.toSet());
        Set<SvAnnTxModel> affectedTranscripts = overlaps.stream()
                .map(Overlap::getTranscriptModel)
                .collect(Collectors.toSet());

        List<Enhancer> enhancers = enhancerOverlapper.getEnhancerOverlaps(duplication);

        // Start figuring out the impact
        SvImpact impact = highestOT.defaultSvImpact(); // default

        if (highestOT.isIntronic()) {
            // intronic duplication close to CDS has HIGH impact,
            // an inversion further away is INTERMEDIATE impact
            int distance = Math.abs(highestImpactOverlap.getDistance());
            impact = distance <= 25
                    ? SvImpact.HIGH
                    : distance <= 100
                    ? SvImpact.INTERMEDIATE
                    : SvImpact.LOW;
        } else if (highestOT.isIntergenic()) {
            //  intergenic duplication, let's consider promoter and enhancers
            if (highestOT.equals(OverlapType.UPSTREAM_GENE_VARIANT_500B)) {
                // promoter region
                impact = SvImpact.HIGH;
            } else {
                impact = enhancers.isEmpty()
                        ? SvImpact.LOW
                        : SvImpact.HIGH;
            }
        }
        Set<GeneWithId> geneWithIdsSet = new HashSet<>();
        for (String symbol : affectedGeneIds) {
            if (geneSymbolMap.containsKey(symbol)) {
                geneWithIdsSet.add(geneSymbolMap.get(symbol));
            }
        }
        if (geneWithIdsSet.size() > maxGenes) {
            impact = impact.decrementSeverity();
        }
        return prioritizeSimpleOverlapByPhenotype(impact, affectedTranscripts, geneWithIdsSet, enhancers, overlaps);
    }

}
