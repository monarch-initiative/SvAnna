package org.jax.svanna.core.priority;

import org.jax.svanna.core.exception.LogUtils;
import org.jax.svanna.core.hpo.GeneWithId;
import org.jax.svanna.core.hpo.HpoDiseaseSummary;
import org.jax.svanna.core.landscape.AnnotationDataService;
import org.jax.svanna.core.landscape.Enhancer;
import org.jax.svanna.core.overlap.Overlap;
import org.jax.svanna.core.overlap.OverlapType;
import org.jax.svanna.core.overlap.Overlapper;
import org.jax.svanna.core.reference.SvannaVariant;
import org.jax.svanna.core.reference.Transcript;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.svart.Variant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static org.jax.svanna.core.priority.Utils.atLeastOneSharedItem;


// class to show that we can replace the enhancer interval arrays with annotationDataService
@SuppressWarnings("Duplicates") // TODO - remove Prototype prioritizer if this works
public class StrippedSvPrioritizer implements SvPrioritizer<SvannaVariant, DiscreteSvPriority> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StrippedSvPrioritizer.class);

    private final AnnotationDataService annotationDataService;

    private final Overlapper overlapper;

    /**
     * Key -- gene symbol, value, {@link GeneWithId} object with symbol and id
     */
    private final Map<String, GeneWithId> geneSymbolMap;

    /**
     * Collection of top level HPO ids that are equal to or ancestors of terms used to annotate the clinical
     * features of the individual being investigated.
     */
    private final Set<TermId> relevantHpoIdsForEnhancers;

    /**
     *  map with key=GeneId, value=Set of {@link HpoDiseaseSummary} objects. Only contains data for genes/diseases
     *  determined to be phenotypically relevant and is empty (but not null) if the user does not enter HPO terms.
     */
    private final Map<TermId, Set<HpoDiseaseSummary>> diseaseSummaryMap;
    /**
     * If an SV affects more genes than this, we assume it is likely to be an artifact, and we down-weight its
     * impact.
     */
    private final int maxGenes;

    public StrippedSvPrioritizer(AnnotationDataService annotationDataService,
                                 Overlapper overlapper,
                                 Map<String, GeneWithId> geneSymbolMap,
                                 Set<TermId> relevantHpoIdsForEnhancers,
                                 Map<TermId, Set<HpoDiseaseSummary>> diseaseSummaryMap,
                                 int maxGenes) {
        this.annotationDataService = annotationDataService;
        this.overlapper = overlapper;
        this.geneSymbolMap = geneSymbolMap;
        this.relevantHpoIdsForEnhancers = relevantHpoIdsForEnhancers;
        this.diseaseSummaryMap = diseaseSummaryMap;
        this.maxGenes = maxGenes;
    }


    @Override
    public DiscreteSvPriority prioritize(SvannaVariant variant) {
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
        switch (variant.variantType().baseType()) {
            case DEL:
                return prioritizeDeletion(variant);
            case INS:
                return prioritizeInsertion(variant);
            case DUP:
                return prioritizeDuplication(variant);
            case INV:
                return prioritizeInversion(variant);
            case TRA:
            case BND:
                return prioritizeTranslocation(variant);

            default:
                LogUtils.logWarn(LOGGER, "Prioritization of {} is not yet supported", variant.variantType());
                return DiscreteSvPriority.unknown();
        }
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
    private DiscreteSvPriority prioritizeDeletion(Variant deletion) {
        // find the gene/transcript with the most deleterious OverlapType
        List<Overlap> overlaps = overlapper.getOverlaps(deletion);

        // set default impact
        Optional<Overlap> highestOverlapType = findOverlapWithMaxPriority(overlaps);
        if (highestOverlapType.isEmpty()) {
            LogUtils.logWarn(LOGGER, "Did not find overlap for {}", LogUtils.variantSummary(deletion));
            return DiscreteSvPriority.unknown();
        }
        SvImpact impact = highestOverlapType.get().getOverlapType().defaultSvImpact();


        // now the impact might still be HIGH if the deletion overlaps with a phenotypically relevant enhancer
        // impact is INTERMEDIATE if the deletion overlaps with some enhancer
        List<Enhancer> enhancers = annotationDataService.overlappingEnhancers(deletion);
        SvImpact enhancerImpact = enhancerPhenotypicImpact(enhancers);

        if (enhancerImpact == SvImpact.HIGH) {
            impact = SvImpact.HIGH;
        } else if (!impact.satisfiesThreshold(SvImpact.HIGH)) {
            impact = SvImpact.INTERMEDIATE;
        }

        // counts of gene regardless of relevance
        Set<Transcript> affectedTranscripts = overlaps.stream()
                .map(Overlap::getTranscriptModel)
                .collect(Collectors.toSet());
        long geneCount = affectedTranscripts.stream()
                .map(Transcript::hgvsSymbol)
                .distinct()
                .count();

        // select the relevant genes and transcripts
        if (geneCount > maxGenes) {
            // deletions this big should have been discovered elsewhere, they are likely artifacts
            impact = impact.decrementSeverity();
            LogUtils.logWarn(LOGGER, "Deletion {} involves {} genes", LogUtils.variantSummary(deletion), geneCount);
        }
        Set<GeneWithId> geneWithIdsSet = selectOverlappingGeneIds(overlaps);
        return prioritizeSimpleOverlapByPhenotype(impact, !affectedTranscripts.isEmpty(), geneWithIdsSet, enhancers);

    }

    /**
     * Sequence based insertion prioritization. Any insertion that occurs in exonic regions is considered HIGH impact
     * except for insertions into UTR regions of coding genes (Any insertion in a transcript of a non-coding gene
     * is considered high impact).
     *
     * @param insertion an insertion
     * @return Corresponding prioritization according to sequence
     */
    private DiscreteSvPriority prioritizeInsertion(Variant insertion) {
        List<Overlap> overlaps = overlapper.getOverlaps(insertion);
        Optional<Overlap> highestImpactOverlapOpt = overlaps.stream()
                .max(Comparator.comparing(Overlap::getOverlapType));
        if (highestImpactOverlapOpt.isEmpty()) {
            // should never happen
            LogUtils.logWarn(LOGGER, "Could not identify highest impact overlap for insertion: {}.", insertion);
            return DiscreteSvPriority.unknown();
        }
        // sequence impact
        Overlap overlap = highestImpactOverlapOpt.get();
        Set<GeneWithId> geneWithIdsSet = selectOverlappingGeneIds(overlaps);
        SvImpact sequenceImpact = insertionSequenceImpact(overlap, geneWithIdsSet);

        // enhancer impact
        List<Enhancer> enhancers = annotationDataService.overlappingEnhancers(insertion);
        SvImpact enhancerImpact = enhancerPhenotypicImpact(enhancers);

        // final impact
        SvImpact impact = SvImpact.max(sequenceImpact, enhancerImpact);
        return prioritizeSimpleOverlapByPhenotype(impact, !overlaps.isEmpty(), geneWithIdsSet, enhancers);
    }

    private SvImpact insertionSequenceImpact(Overlap overlap, Set<GeneWithId> geneWithIdsSet) {
        OverlapType overlapType = overlap.getOverlapType();
        SvImpact impact = overlapType.defaultSvImpact();
        if (geneWithIdsSet.size() > 1 && overlapType.isExonic()) {
            // Insertion affects >1 genes, although I'm not sure if this can actually happen
            impact = SvImpact.max(impact, SvImpact.HIGH);
        } else if (overlapType.isExonic()) {
            // insertion into CDS is HIGH, UTR is intermediate
            impact = overlap.overlapsCds()
                    ? SvImpact.HIGH
                    : SvImpact.INTERMEDIATE;
        } else if (overlapType.isIntronic()) {
            // intronic insertion close to CDS has HIGH impact,
            // an insertion further away is INTERMEDIATE impact
            int distance = Math.abs(overlap.getDistance());
            impact = distance <= 25
                    ? SvImpact.HIGH
                    : distance <= 100
                    ? SvImpact.INTERMEDIATE
                    : SvImpact.LOW;
        }
        return impact;
    }


    private DiscreteSvPriority prioritizeDuplication(Variant duplication) {
        List<Overlap> overlaps = overlapper.getOverlaps(duplication);
        Optional<Overlap> maxPriorityOverlap = findOverlapWithMaxPriority(overlaps);
        if (maxPriorityOverlap.isEmpty()) {
            // should never happen
            LOGGER.error("Could not identify highest impact overlap for duplication: {}.", duplication);
            return DefaultAnnotatedSvPriority.unknown();
        }

        // Start figuring out the impact
        Overlap overlap = maxPriorityOverlap.get();
        List<Enhancer> enhancers = annotationDataService.overlappingEnhancers(duplication);
        SvImpact sequenceImpact = duplicationSequenceImpact(overlap, enhancers);

        Set<GeneWithId> geneWithIdsSet = selectOverlappingGeneIds(overlaps);
        // counts of gene regardless of relevance
        Set<Transcript> affectedTranscripts = overlaps.stream()
                .map(Overlap::getTranscriptModel)
                .collect(Collectors.toSet());
        long geneCount = affectedTranscripts.stream()
                .map(Transcript::hgvsSymbol)
                .distinct()
                .count();

        if (geneCount > maxGenes) {
            sequenceImpact = sequenceImpact.decrementSeverity();
            LogUtils.logWarn(LOGGER, "Duplication {} involves {} genes", LogUtils.variantSummary(duplication), geneCount);
        }
        return prioritizeSimpleOverlapByPhenotype(sequenceImpact, !affectedTranscripts.isEmpty(), geneWithIdsSet, enhancers);
    }

    private SvImpact duplicationSequenceImpact(Overlap overlap, List<Enhancer> enhancers) {
        OverlapType overlapType = overlap.getOverlapType();
        SvImpact impact = overlapType.defaultSvImpact();

        if (overlapType.isIntronic()) {
            // intronic duplication close to CDS has HIGH impact,
            // an inversion further away is INTERMEDIATE impact
            int distance = Math.abs(overlap.getDistance());
            impact = distance <= 25
                    ? SvImpact.HIGH
                    : distance <= 100
                    ? SvImpact.INTERMEDIATE
                    : SvImpact.LOW;
        } else if (overlapType.isIntergenic()) {
            //  intergenic duplication, let's consider promoter and enhancers
            if (overlapType.equals(OverlapType.UPSTREAM_GENE_VARIANT_500B)) {
                // promoter region
                impact = SvImpact.HIGH;
            } else {
                impact = enhancers.isEmpty()
                        ? SvImpact.LOW
                        : SvImpact.HIGH;
            }
        }
        return impact;
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
    private DiscreteSvPriority prioritizeInversion(Variant inversion) {
        List<Overlap> overlaps = overlapper.getOverlaps(inversion);

        Optional<Overlap> highestOTOpt = findOverlapWithMaxPriority(overlaps);
        if (highestOTOpt.isEmpty()) {
            LogUtils.logWarn(LOGGER, "Did not find overlap for variant {}", LogUtils.variantSummary(inversion));
            return DiscreteSvPriority.unknown();
        }
        Overlap overlap = highestOTOpt.get();
        OverlapType overlapType = overlap.getOverlapType();

        // set default impact
        List<Enhancer> enhancers = annotationDataService.overlappingEnhancers(inversion);
        SvImpact impact = considerEnhancersForInversion(overlapType.defaultSvImpact(), overlap, enhancers);
        // if the inversion already has high priority, then return it
        // otherwise look for longer range position effects
        Set<Transcript> affectedTranscripts = overlaps.stream()
                .map(Overlap::getTranscriptModel)
                .collect(Collectors.toSet());
        Set<GeneWithId> geneWithIdsSet = selectOverlappingGeneIds(overlaps);
        DiscreteSvPriority prio = prioritizeSimpleOverlapByPhenotype(impact, !affectedTranscripts.isEmpty(), geneWithIdsSet, enhancers);
        if (prio.getImpact() == SvImpact.HIGH) {
            return prio;
        }
        // if the inversion is located within an intron of a gene, we will assume its effects
        // are only due to the intron. Similarly, if the inversion is upstream or downstream of the
        // it will not affect the distance to enhancers on the "other side" -- by definition,
        // enhancers are not sensitive to orientation
        if (overlapType.isIntronic() || overlapType.isDownstream() || overlapType.isUpstream()) {
            return prio;
        }
        //if we get here, we look and see if there are both relevant genes within the inversion
        // and relevant enhancers within a window, or vice version.

        final int OFFSET = 100_000; // TODO make class variable, adjust from command line
        // check if there are genes within the inversion. If so, look for enhancers outside
        // of the inversion boundaries
        if (! affectedTranscripts.isEmpty()) {
            // in this case, at least one gene is inside the inversion
            // we look and see if there are relevant enhancers outside of the inversion
            List<Enhancer> outsideEnhancers = annotationDataService.overlappingEnhancers(inversion.withPadding(OFFSET));
            boolean relevantEnhancer = false;
            for (var e : outsideEnhancers) {
                if (atLeastOneSharedItem(relevantHpoIdsForEnhancers, e.hpoTermAssociations())) {
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
                return new DefaultAnnotatedSvPriority(SvImpact.HIGH, affectedTranscripts, geneWithIdsSet, outsideEnhancers, overlaps, diseases);
            }
        }
        return prio; // stick to the local interpretation
    }

    private DiscreteSvPriority prioritizeTranslocation(Variant variant) {
        // the following gets overlaps that disrupt transcripts only
        List<Overlap> overlaps = overlapper.getOverlaps(variant);
        SvImpact impact = SvImpact.LOW; // default
        Optional<Overlap> overlapOptional = findOverlapWithMaxPriority(overlaps);
        if (overlapOptional.isEmpty()) {
            LogUtils.logWarn(LOGGER, "Did not find overlap for variant {}", LogUtils.variantSummary(variant));
            return DiscreteSvPriority.unknown();
        }
        Overlap overlap = overlapOptional.get();
        OverlapType overlapType = overlap.getOverlapType();
        Set<Transcript> affectedTranscripts;
        Set<GeneWithId> geneWithIdsSet;
        if (!overlaps.isEmpty()) {
            Set<String> affectedGeneIds = overlaps.stream().map(Overlap::getGeneSymbol).collect(Collectors.toSet());
            geneWithIdsSet = new HashSet<>();
            for (String symbol : affectedGeneIds) {
                if (geneSymbolMap.containsKey(symbol)) {
                    geneWithIdsSet.add(geneSymbolMap.get(symbol));
                }
            }
            affectedTranscripts = overlaps.stream()
                    .map(Overlap::getTranscriptModel)
                    .collect(Collectors.toSet());
            if (overlapType.translocationDisruptable()) {
                impact = SvImpact.HIGH;
                overlapType = OverlapType.TRANSCRIPT_DISRUPTED_BY_TRANSLOCATION;
            } else {
                impact = SvImpact.INTERMEDIATE;
                overlapType = OverlapType.TRANSLOCATION_WITHOUT_TRANSCRIPT_DISRUPTION;
            }
        } else {
            affectedTranscripts = Set.of();
            geneWithIdsSet = Set.of();
        }
        List<Enhancer> enhancers = annotationDataService.overlappingEnhancers(variant);
        if (enhancers.size() > 0) {
            impact = SvImpact.HIGH;
            overlapType = OverlapType.ENHANCER_DISRUPTED_BY_TRANSLOCATION;
        }
        if (impact == SvImpact.HIGH) {
            // this means that the translocation disrupts a gene, so we will
            // take gene disruption, rather than regulatory effects, to be
            // the primary mode of action of this translocation
            return prioritizeSimpleOverlapByPhenotype(impact, !affectedTranscripts.isEmpty(), geneWithIdsSet, enhancers);
        }

        // TODO -- we need a bespoke prioritizer for translocations
        // FOR NOW there are no diseases
        return DiscreteSvPriorityDefault.of(impact, false);
    }

    private Set<GeneWithId> selectOverlappingGeneIds(List<Overlap> overlaps) {
        return overlaps.stream()
                .map(Overlap::getGeneSymbol)
                .filter(geneSymbolMap::containsKey)
                .map(geneSymbolMap::get)
                .collect(Collectors.toSet());
    }

    /**
     * This can be used to prioritize the phenotypic relevance of SVs that overlap
     * part of a gene that is phenotypically relevant. It should be used for SVs such
     * as deletions where overlapping with the CDS of a gene can be high priority.
     * We also prioritize enhancers at this step
     *
     * @return a phenotypically prioritized {@link AnnotatedSvPriority} object
     */
    private DiscreteSvPriority prioritizeSimpleOverlapByPhenotype(SvImpact impact,
                                                                  boolean affectsTranscript,
                                                                  Set<GeneWithId> affectedGeneIds,
                                                                  List<Enhancer> affectedEnhancers) {
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
                ? impact.decrementSeverity()
                : impact;

        if (affectsTranscript && affectedEnhancers.size() > 0) {
            // in this case, we have prioritize the SV based on overlap with
            // an enhancer. We will rate the SV high if the enhancer is
            // annotated to a tissue that has phenotypic relevant
            for (Enhancer enhancer : affectedEnhancers) {
                if (atLeastOneSharedItem(relevantHpoIdsForEnhancers, enhancer.hpoTermAssociations())) {
                    // if there is at least one affected enhancer, then the impact is high
                    // no matter what else there is
                    phenotypeImpact = SvImpact.HIGH;
                    break;
                }
            }
        }

        return DiscreteSvPriority.of(phenotypeImpact, !diseaseList.isEmpty());
    }

    private SvImpact enhancerPhenotypicImpact(Collection<Enhancer> enhancers) {
        return enhancers.stream().anyMatch(e -> atLeastOneSharedItem(relevantHpoIdsForEnhancers, e.hpoTermAssociations()))
                ? SvImpact.HIGH
                : SvImpact.INTERMEDIATE;
    }

    private static SvImpact considerEnhancersForInversion(SvImpact impact, Overlap overlap, List<Enhancer> enhancers) {
        OverlapType overlapType = overlap.getOverlapType();
        if (overlapType.isExonic() && overlapType != OverlapType.TRANSCRIPT_CONTAINED_IN_SV) {
            // (1) the inversion affects an exon
            // If an inversion completely contains a gene, we do not rank it as high impact
            // but we will look at regulatory effects
            impact = SvImpact.HIGH;
        } else if (overlapType == OverlapType.TRANSCRIPT_CONTAINED_IN_SV) {
            impact = SvImpact.INTERMEDIATE;
        } else if (overlapType.isIntronic()) {
            // (2) intronic inversion close to CDS has HIGH impact,
            // an inversion further away is INTERMEDIATE impact
            int distance = overlap.getDistance();
            impact = distance <= 25
                    ? SvImpact.HIGH
                    : distance <= 100
                    ? SvImpact.INTERMEDIATE
                    : SvImpact.LOW;
        } else if (overlapType.isIntergenic()) {
            // (3) intergenic inversion, let's consider promoter and enhancers
            if (overlapType.equals(OverlapType.UPSTREAM_GENE_VARIANT_500B)) {
                // promoter region
                impact = SvImpact.HIGH;
            } else if (overlapType.equals(OverlapType.UPSTREAM_GENE_VARIANT_2KB)) {
                // promoter region
                impact = SvImpact.INTERMEDIATE;
            } else {
                impact = enhancers.isEmpty()
                        ? SvImpact.LOW
                        : SvImpact.HIGH;
            }
        }
        return impact;
    }

    private static Optional<Overlap> findOverlapWithMaxPriority(List<Overlap> overlaps) {
        return overlaps.stream().max(Comparator.comparing(Overlap::getOverlapType));
    }
}
