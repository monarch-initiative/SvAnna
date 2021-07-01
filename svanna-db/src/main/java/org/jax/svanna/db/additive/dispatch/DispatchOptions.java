package org.jax.svanna.db.additive.dispatch;

import java.util.Objects;

public class DispatchOptions {

    private final boolean forceEvaluateTad;

    public static DispatchOptions of(boolean forceEvaluateTad) {
        return new DispatchOptions(forceEvaluateTad);
    }

    private DispatchOptions(boolean forceEvaluateTad) {
        this.forceEvaluateTad = forceEvaluateTad;
    }

    public boolean forceEvaluateTad() {
        return forceEvaluateTad;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DispatchOptions that = (DispatchOptions) o;
        return forceEvaluateTad == that.forceEvaluateTad;
    }

    @Override
    public int hashCode() {
        return Objects.hash(forceEvaluateTad);
    }

    @Override
    public String toString() {
        return "DispatchOptions{" +
                "forceEvaluateTad=" + forceEvaluateTad +
                '}';
    }
}
