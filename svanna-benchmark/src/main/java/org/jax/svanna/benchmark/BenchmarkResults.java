package org.jax.svanna.benchmark;

import java.util.List;
import java.util.Objects;

class BenchmarkResults {

    private final String caseName;
    private final String backgroundVcfName;
    private final List<VariantPriority> priorities;

    BenchmarkResults(String caseName, String backgroundVcfName, List<VariantPriority> priorities) {
        this.caseName = caseName;
        this.backgroundVcfName = backgroundVcfName;
        this.priorities = priorities;
    }

    public String caseName() {
        return caseName;
    }

    public String backgroundVcfName() {
        return backgroundVcfName;
    }

    public List<VariantPriority> priorities() {
        return priorities;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BenchmarkResults that = (BenchmarkResults) o;
        return Objects.equals(caseName, that.caseName) && Objects.equals(backgroundVcfName, that.backgroundVcfName) && Objects.equals(priorities, that.priorities);
    }

    @Override
    public int hashCode() {
        return Objects.hash(caseName, backgroundVcfName, priorities);
    }
}
