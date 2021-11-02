package org.jax.svanna.model.landscape;

public enum RepeatFamily {

    DNA,
    DNA_hAT(DNA),
    DNA_hAT_Ac(DNA, DNA_hAT),
    DNA_hAT_Blackjack(DNA, DNA_hAT),
    DNA_hAT_Charlie(DNA, DNA_hAT),
    DNA_hAT_Tag1(DNA, DNA_hAT),
    DNA_hAT_Tip100(DNA, DNA_hAT),
    DNA_Merlin(DNA),
    DNA_MULE_MuDR(DNA),
    DNA_PIF_Harbinger(DNA),
    DNA_PiggyBac(DNA),
    DNA_TcMar(DNA),
    DNA_TcMar_Mariner(DNA, DNA_TcMar),
    DNA_TcMar_Pogo(DNA, DNA_TcMar),
    DNA_TcMar_Tc2(DNA, DNA_TcMar),
    DNA_TcMar_Tigger(DNA, DNA_TcMar),

    LINE,

    LOW_COMPLEXITY,

    LTR,
    LTR_ERV1(LTR),
    LTR_ERVK(LTR),
    LTR_ERVL(LTR),
    LTR_ERVL_MaLR(LTR, LTR_ERVL),
    LTR_Gypsy(LTR),

    SINE,
    SINE_5SDeuL2(SINE),
    SINE_ALU(SINE),
    SINE_MIR(SINE),
    SINE_tRNA(SINE),
    SINE_tRNA_Deu(SINE, SINE_tRNA),
    SINE_tRNA_RTE(SINE, SINE_tRNA),

    RETROPOSON,

    RC_HELITRON,

    RNA,
    RNA_rRNA(RNA),
    RNA_scRNA(RNA),
    RNA_snRNA(RNA),
    RNA_srpRNA(RNA),
    RNA_tRNA(RNA),
    SATELLITE,
    SATELLITE_ACRO(SATELLITE),
    SATELLITE_CENTR(SATELLITE),
    SATELLITE_TELO(SATELLITE),

    SIMPLE_REPEAT,
    UNKNOWN;

    private final RepeatFamily baseType, subType;

    RepeatFamily() {
        this.baseType = this;
        this.subType = this;
    }

    RepeatFamily(RepeatFamily parent) {
        this.baseType = parent;
        this.subType = this;
    }

    RepeatFamily(RepeatFamily baseType, RepeatFamily subType) {
        this.baseType = baseType;
        this.subType = subType;
    }

    public RepeatFamily baseType() {
        return baseType;
    }

    public RepeatFamily subType() {
        return subType;
    }

}
