package org.jax.svanna.cli.cmd.benchmark;

import org.jax.svanna.core.priority.SvPriority;
import org.monarchinitiative.svart.Variant;

import java.util.Objects;

class VariantPriority {
    private final Variant variant;
    private final SvPriority priority;


    VariantPriority(Variant variant, SvPriority priority) {
        this.variant = variant;
        this.priority = priority;
    }

    public Variant variant() {
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
