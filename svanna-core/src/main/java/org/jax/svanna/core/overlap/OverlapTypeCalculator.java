package org.jax.svanna.core.overlap;

import org.jax.svanna.core.reference.Transcript;
import org.monarchinitiative.svart.GenomicRegion;

import static org.jax.svanna.core.overlap.OverlapType.*;

public class OverlapTypeCalculator {

    public static OverlapType calculate(GenomicRegion variant, Transcript transcript) {
        return transcript.overlapsWith(variant)
                ? forOverlappingTranscript(variant, transcript)
                : forIntergenicVariant(variant, transcript);
    }

    private static OverlapType forOverlappingTranscript(GenomicRegion variant, Transcript transcript) {
        if (variant.contains(transcript)) {
            return TRANSCRIPT_CONTAINED_IN_SV;
        }
        GenomicRegion variantRegion = variant.withStrand(transcript.strand());
        long nAffectedExons = transcript.exons().stream()
                .filter(e -> e.overlapsWith(variantRegion))
                .count();
        return (nAffectedExons == 0)
                ? INTRONIC
                : (nAffectedExons == 1) ? SINGLE_EXON_IN_TRANSCRIPT : MULTIPLE_EXON_IN_TRANSCRIPT;
    }

    private static OverlapType forIntergenicVariant(GenomicRegion variant, Transcript transcript) {
        int distance = transcript.distanceTo(variant);

        if (distance < 0) {
            // event is upstream from the transcript
            if (distance >= -500) {
                return UPSTREAM_GENE_VARIANT_500B;
            } else if (distance >= -2_000) {
                return UPSTREAM_GENE_VARIANT_2KB;
            } else if (distance >= -5_000) {
                return UPSTREAM_GENE_VARIANT_5KB;
            } else if (distance >= -500_000) {
                return UPSTREAM_GENE_VARIANT_500KB;
            } else {
                return UPSTREAM_GENE_VARIANT;
            }
        } else {
            // event is downstream from the tx
            if (distance <= 500) {
                return DOWNSTREAM_GENE_VARIANT_500B;
            } else if (distance <= 2_000) {
                return DOWNSTREAM_GENE_VARIANT_2KB;
            } else if (distance <= 5_000) {
                return DOWNSTREAM_GENE_VARIANT_5KB;
            } else if (distance <= 500_000) {
                return DOWNSTREAM_GENE_VARIANT_500KB;
            } else {
                return DOWNSTREAM_GENE_VARIANT;
            }
        }
    }
}
