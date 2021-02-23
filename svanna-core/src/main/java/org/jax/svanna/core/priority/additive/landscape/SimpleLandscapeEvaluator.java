package org.jax.svanna.core.priority.additive.landscape;

import org.jax.svanna.core.landscape.Enhancer;
import org.jax.svanna.core.priority.additive.simple.EnhancerGeneRelevanceCalculator;
import org.jax.svanna.core.priority.additive.simple.GeneRelevanceCalculator;
import org.jax.svanna.core.priority.additive.simple.ImpactCalculator;
import org.jax.svanna.core.reference.Gene;
import org.monarchinitiative.svart.Variant;

import java.util.Objects;

@Deprecated(forRemoval = true)
public class SimpleLandscapeEvaluator implements LandscapeEvaluator<LandscapeSimple> {

    // A properly functional gene contributes this much to the final score.
    // The score is used in ref branch. Impact of variant to a gene should reduce the score.
    private static final double BASE_GENE_SCORE = 10.;

    // A properly functional enhancer-gene interaction contributes this much to the final score.
    private static final double BASE_ENHANCER_GENE_SCORE = 1.;

    // Non-functional gene/enhancer will contribute this much to the final score.
    private static final double BASE_LOF = 0.;

    private final GeneRelevanceCalculator geneRelevanceCalculator;

    private final EnhancerGeneRelevanceCalculator enhancerGeneRelevanceCalculator;

    private final ImpactCalculator<Gene> geneImpactCalculator;

    private final ImpactCalculator<Enhancer> enhancerImpactCalculator;

    protected SimpleLandscapeEvaluator(Builder builder) {
        this.geneRelevanceCalculator = Objects.requireNonNull(builder.geneRelevanceCalculator);
        this.enhancerGeneRelevanceCalculator = Objects.requireNonNull(builder.enhancerGeneRelevanceCalculator);
        this.geneImpactCalculator = Objects.requireNonNull(builder.geneImpactCalculator);
        this.enhancerImpactCalculator = Objects.requireNonNull(builder.enhancerImpactCalculator);
    }

    public static Builder builder() {
        return new Builder();
    }


    @Override
    public double evaluateLandscape(Variant variant, LandscapeSimple landscape) {
        double reference = evaluateReferenceLandscape(landscape);
        double alternate = evaluateVariantLandscape(variant, landscape);
        return reference - alternate;
    }

    protected double evaluateReferenceLandscape(LandscapeSimple landscape) {
        double geneRelevance = landscape.genes().stream()
                .mapToDouble(gene -> BASE_GENE_SCORE * geneRelevanceCalculator.calculateRelevance(gene))
                .sum();

        // The best thing to have here would be GxE matrix where each cell quantifies importance of enhancer E
        // on expression of gene G
        double enhancerRelevance = landscape.enhancers().stream()
                .mapToDouble(e -> landscape.genes().stream()
                        .mapToDouble(g -> BASE_ENHANCER_GENE_SCORE * enhancerGeneRelevanceCalculator.calculateRelevance(g, e))
                        .sum())
                .sum();

        return geneRelevance + enhancerRelevance;
    }

    protected double evaluateVariantLandscape(Variant variant, LandscapeSimple landscape) {
        double geneRelevance = landscape.genes().stream()
                .mapToDouble(gene -> geneImpactCalculator.calculateImpact(variant, gene) * geneRelevanceCalculator.calculateRelevance(gene))
                .sum();

        double enhancerRelevance = landscape.enhancers().stream()
                .mapToDouble(enhancer -> landscape.genes().stream()
                        .mapToDouble(gene -> enhancerImpactCalculator.calculateImpact(variant, enhancer) * enhancerGeneRelevanceCalculator.calculateRelevance(gene, enhancer))
                        .sum())
                .sum();
        return geneRelevance + enhancerRelevance;
    }

    public static class Builder {

        private GeneRelevanceCalculator geneRelevanceCalculator = GeneRelevanceCalculator.defaultGeneRelevanceCalculator();

        private EnhancerGeneRelevanceCalculator enhancerGeneRelevanceCalculator = EnhancerGeneRelevanceCalculator.defaultCalculator();

        private ImpactCalculator<Gene> geneImpactCalculator = ImpactCalculator.defaultImpactCalculator(BASE_LOF, BASE_GENE_SCORE);

        private ImpactCalculator<Enhancer> enhancerImpactCalculator = ImpactCalculator.defaultImpactCalculator(BASE_LOF, BASE_ENHANCER_GENE_SCORE);

        public Builder geneRelevanceCalculator(GeneRelevanceCalculator geneRelevanceCalculator) {
            this.geneRelevanceCalculator = geneRelevanceCalculator;
            return self();
        }

        public Builder enhancerGeneInteractionCalculator(EnhancerGeneRelevanceCalculator enhancerGeneRelevanceCalculator) {
            this.enhancerGeneRelevanceCalculator = enhancerGeneRelevanceCalculator;
            return self();
        }

        public Builder variantImpactCalculator(ImpactCalculator<Gene> variantImpactCalculator) {
            this.geneImpactCalculator = variantImpactCalculator;
            return self();
        }

        public Builder enhancerImpactCalculator(ImpactCalculator<Enhancer> enhancerImpactCalculator) {
            this.enhancerImpactCalculator = enhancerImpactCalculator;
            return self();
        }

        public SimpleLandscapeEvaluator build() {
            return new SimpleLandscapeEvaluator(self());
        }

        protected Builder self() {
            return this;
        }
    }

}
