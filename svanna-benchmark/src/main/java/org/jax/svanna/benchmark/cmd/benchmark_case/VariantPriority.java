package org.jax.svanna.benchmark.cmd.benchmark_case;

import org.jax.svanna.core.priority.SvPriority;
import org.monarchinitiative.svart.GenomicVariant;

import java.util.Objects;

class VariantPriority {
    private final GenomicVariant variant;
    private final SvPriority priority;


    VariantPriority(GenomicVariant variant, SvPriority priority) {
        this.variant = variant;
        this.priority = priority;
    }

    public GenomicVariant variant() {
        return variant;
    }

    public SvPriority priority() {
        return priority;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VariantPriority that = (VariantPriority) o;
        return Objects.equals(variant, that.variant) && Objects.equals(priority, that.priority);
    }

    @Override
    public int hashCode() {
        return Objects.hash(variant, priority);
    }
}
