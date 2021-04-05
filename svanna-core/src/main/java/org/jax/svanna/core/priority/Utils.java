package org.jax.svanna.core.priority;

import java.util.Set;

class Utils {

    private Utils() {}

    static <T> boolean atLeastOneSharedItem(Set<? extends T> a, Set<? extends T> b) {
        Set<? extends T> smaller = (a.size() < b.size()) ? a : b;
        Set<? extends T> larger = (a.size() < b.size()) ? b : a;

        return smaller.stream().anyMatch(larger::contains);
    }

}
