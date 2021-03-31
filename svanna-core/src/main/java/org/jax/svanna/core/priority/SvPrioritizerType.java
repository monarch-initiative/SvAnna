package org.jax.svanna.core.priority;

public enum SvPrioritizerType {

    @Deprecated
    PROTOTYPE,

    ADDITIVE,
    ADDITIVE_SIMPLE(ADDITIVE),
    ADDITIVE_GRANULAR(ADDITIVE);


    private final SvPrioritizerType baseType;

    SvPrioritizerType() {
        this.baseType = this;
    }

    SvPrioritizerType(SvPrioritizerType baseType) {
        this.baseType = baseType;
    }

    public SvPrioritizerType baseType() {
        return baseType;
    }

}
