package org.jax.svann.priority;

import de.charite.compbio.jannovar.reference.TranscriptModel;
import org.jax.svann.genomicreg.Enhancer;
import org.jax.svann.hpo.GeneWithId;
import org.jax.svann.hpo.HpoDiseaseSummary;
import org.jax.svann.overlap.EnhancerOverlapper;
import org.jax.svann.overlap.Overlap;
import org.jax.svann.overlap.OverlapType;
import org.jax.svann.overlap.Overlapper;
import org.jax.svann.reference.SequenceRearrangement;
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
                                  Map<TermId, Set<HpoDiseaseSummary>> diseaseSummaryMap) {
        this.geneSymbolMap = geneSymbolMap;
        this.overlapper = overlapper;
        this.enhancerOverlapper = enhancerOverlapper;
        this.patientPhenotypeTerms = patientPhenotypeTerms;
        this.relevantHpoIdsForEnhancers = relevantHpoIdsForEnhancers;
        this.diseaseSummaryMap = diseaseSummaryMap;
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
                                                  Set<TranscriptModel> affectedTranscripts,
                                                  Set<GeneWithId> affectedGeneIds,
                                                  List<Enhancer> affectedEnhancers,
                                                  List<Overlap> olaps) {

        List<HpoDiseaseSummary> diseaseList = new ArrayList<>();
        SvImpact phenotypeImpact;
        for (var gene : affectedGeneIds) {
            TermId tid = gene.getGeneId();
            if (this.diseaseSummaryMap.containsKey(tid)) {
                diseaseList.addAll(this.diseaseSummaryMap.get(tid));
            }
        }
        if (diseaseList.isEmpty()) {
            switch (svImpact) {
                case HIGH:
                    phenotypeImpact = SvImpact.INTERMEDIATE;
                    break;
                case INTERMEDIATE:
                    phenotypeImpact = SvImpact.LOW;
                    break;
                default:
                    phenotypeImpact = svImpact;
            }
        } else {
            phenotypeImpact = svImpact;
        }
        if (affectedTranscripts.isEmpty() && affectedEnhancers.size() > 0 ) {
            // in this case, we have prioritize the SV based on overlap with
            // an enhancer. We will rate the SV high if the enhancer is
            // annotated to a tissue that has phenotypic relevant
            boolean relevant = false;
            for (Enhancer e : affectedEnhancers) {
                if (this.relevantHpoIdsForEnhancers.contains(e.getTermId())) {
                    relevant = true;
                    break;
                }
            }
            if (! relevant) {
                // downgrade the impact
                if (svImpact == SvImpact.HIGH) {
                    phenotypeImpact = SvImpact.INTERMEDIATE;
                } else {
                    phenotypeImpact = SvImpact.LOW;
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
        Optional<Overlap> highestImpactOverlapOpt = overlaps.stream()
                .max(Comparator.comparing(Overlap::getOverlapType));
        if (highestImpactOverlapOpt.isEmpty()) {
            // should never happen
            LOGGER.error("Could not identify highest impact overlap for {}.", deletion);
            return DefaultSvPriority.unknown();
        }
        Overlap highestImpactOverlap = highestImpactOverlapOpt.get();
        OverlapType highestOT = highestImpactOverlap.getOverlapType();

        // select the relevant genes and transcripts
        Set<String> affectedGeneIds = overlaps.stream()
                .map(Overlap::getGeneSymbol)
                .collect(Collectors.toSet());
        Set<GeneWithId> geneWithIdsSet = new HashSet<>();
        for (String symbol : affectedGeneIds) {
            if (geneSymbolMap.containsKey(symbol)) {
                geneWithIdsSet.add(geneSymbolMap.get(symbol));
            }
        }
        Set<TranscriptModel> affectedTranscripts = overlaps.stream()
                .map(Overlap::getTranscriptModel)
                .collect(Collectors.toSet());

        // start figuring out the impact
        SvImpact impact = highestOT.defaultSvImpact();

        if (affectedGeneIds.size() > 1) {
            // impact is high if >1 gene is affected
            impact = SvImpact.HIGH;
        } else if (highestOT.isExonic()) {
            // set impact to INTERMEDIATE if the deletion does not affect the coding sequence (is UTR)
            impact = highestImpactOverlap.overlapsCds()
                    ? SvImpact.HIGH
                    : SvImpact.INTERMEDIATE;
        } else if (highestOT.isIntronic()) {
            // intronic deletion close to CDS has HIGH impact,
            // an insertion further away is INTERMEDIATE impact
            int distance = highestImpactOverlap.getDistance();
            impact = distance <= 25
                    ? SvImpact.HIGH
                    : distance <= 100
                    ? SvImpact.INTERMEDIATE
                    : SvImpact.LOW;
        }

        // now the impact might still be HIGH if the deletion overlaps with a phenotypically relevant enhancer
        // impact is INTERMEDIATE if the deletion overlaps with some enhancer
        List<Enhancer> enhancers = enhancerOverlapper.getEnhancerOverlaps(deletion);
        if (!enhancers.isEmpty()) {
            SvImpact enhancerImpact = enhancers.stream().anyMatch(enhancer -> relevantHpoIdsForEnhancers.contains(enhancer.getTermId()))
                    ? SvImpact.HIGH
                    : SvImpact.INTERMEDIATE;
            if (impact != SvImpact.HIGH && enhancerImpact == SvImpact.HIGH) {
                impact = SvImpact.HIGH;
            } else if (impact == SvImpact.LOW && enhancerImpact == SvImpact.HIGH) {
                impact = SvImpact.HIGH;
            }
        }
        return prioritizeSimpleOverlapByPhenotype(impact, affectedTranscripts, geneWithIdsSet, enhancers, overlaps);

    }

    private boolean affectedGenesRelevant(Set<GeneWithId> genesAffeectedBySv) {
        for (var gwi : genesAffeectedBySv) {
            TermId tid = gwi.getGeneId();
             if (this.diseaseSummaryMap.containsKey(tid)) {
                 return true;
             }
        }
        return false;
    }

    private boolean affectedEnhancersRelevant(List<Enhancer> enhancers) {
        return enhancers.
                stream().
                map(Enhancer::getTermId).
                anyMatch(relevantHpoIdsForEnhancers::contains);
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
        Set<TranscriptModel> affectedTranscripts = overlaps.stream()
                .map(Overlap::getTranscriptModel)
                .collect(Collectors.toSet());

        // start figuring out the impact
        SvImpact impact = highestOT.defaultSvImpact();
        if (affectedGeneIds.size() > 1) {
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
            int distance = highestImpactOverlap.getDistance();
            impact = distance <= 25
                    ? SvImpact.HIGH
                    : distance <= 100
                    ? SvImpact.INTERMEDIATE
                    : SvImpact.LOW;
        }

        List<Enhancer> enhancers = enhancerOverlapper.getEnhancerOverlaps(insertion);
        if (!enhancers.isEmpty()) {
            impact = enhancers.stream().anyMatch(enhancer -> relevantHpoIdsForEnhancers.contains(enhancer.getTermId()))
                    ? SvImpact.HIGH
                    : SvImpact.INTERMEDIATE;
        }
        // When we get here, we will perform phenotypic prioritization.
        List<HpoDiseaseSummary> diseaseList;
        if (this.diseaseSummaryMap.isEmpty()) {
            // i.e., the user did not provide phenotypic data
            diseaseList = List.of(); // empty list
        } else {
            // check relevance with respect to transcripts
            boolean affectsTranscripts = affectedGenesRelevant(geneWithIdsSet);
            boolean affectsEnhancers = affectedEnhancersRelevant(enhancers);
            boolean relevant = affectsEnhancers || affectsTranscripts;
            if (! relevant) {
                // downgrade the impact
                if (impact == SvImpact.HIGH) {
                    impact = SvImpact.INTERMEDIATE;
                } else {
                    impact = SvImpact.LOW;
                }
            }
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
        List<Overlap> overlaps = overlapper.getInversionOverlapsRegionBased(inversion);
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
        Set<TranscriptModel> affectedTranscripts = overlaps.stream()
                .map(Overlap::getTranscriptModel)
                .collect(Collectors.toSet());

        List<Enhancer> enhancers = enhancerOverlapper.getEnhancerOverlaps(inversion);

        // Start figuring out the impact
        SvImpact impact = highestOT.defaultSvImpact(); // default

        if (highestOT.isExonic()) {
            // (1) the inversion affects an exon
            impact = SvImpact.HIGH;
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
            if (highestOT.equals(OverlapType.UPSTREAM_GENE_VARIANT_2KB)) {
                // promoter region
                impact = SvImpact.HIGH;
            } else {
                impact = enhancers.isEmpty()
                        ? SvImpact.LOW
                        : SvImpact.HIGH;
            }
        }

        return new DefaultSvPriority(impact, affectedTranscripts, geneWithIdsSet, enhancers, overlaps);
    }


    private DefaultSvPriority prioritizeTranslocation(SequenceRearrangement rearrangement) {
        // the following gets overlaps that disrupt transcripts only
        List<Overlap> overlaps = overlapper.getOverlapList(rearrangement);
        SvImpact impact = SvImpact.LOW; // default
        OverlapType otype = OverlapType.UNKNOWN; // default
        Set<TranscriptModel> affectedTranscripts;
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
            impact = SvImpact.HIGH;
            otype = OverlapType.TRANSCRIPT_DISRUPTED_BY_INVERSION;
        } else {
            affectedTranscripts = Set.of();
            geneWithIdsSet = Set.of();
        }
        List<Enhancer> enhancers = enhancerOverlapper.getEnhancerOverlaps(rearrangement);
        if (enhancers.size() > 0) {
            impact = SvImpact.HIGH;
        }
        return new DefaultSvPriority(impact, affectedTranscripts, geneWithIdsSet, enhancers, overlaps);
    }

    private SvPriority prioritizeDuplication(SequenceRearrangement rearrangement) {
        // TODO: 2. 11. 2020 implement
        return SvPriority.unknown();
    }

}
