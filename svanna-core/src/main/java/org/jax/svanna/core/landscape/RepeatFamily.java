package org.jax.svanna.core.landscape;

public enum RepeatFamily {

    // DNA
    //DNA?
    //DNA/hAT
    //DNA/hAT?
    //DNA/hAT-Ac
    //DNA/hAT-Blackjack
    //DNA/hAT-Charlie
    //DNA/hAT-Tag1
    //DNA/hAT-Tip100
    //DNA/hAT-Tip100?
    //DNA/Merlin
    //DNA/MULE-MuDR
    //DNA/PIF-Harbinger
    //DNA/PiggyBac
    //DNA/PiggyBac?
    //DNA/TcMar
    //DNA/TcMar?
    //DNA/TcMar-Mariner
    //DNA/TcMar-Pogo
    //DNA/TcMar-Tc2
    //DNA/TcMar-Tigger
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

    // LINE/CR1, LINE/Dong-R4, LINE/L1, LINE/L2, LINE/Penelope, LINE/RTE-BovB, LINE/RTE-X
    LINE,

    LOW_COMPLEXITY,

    //LTR, LTR?, LTR/ERV1, LTR/ERV1?, LTR/ERVK, LTR/ERVL, LTR/ERVL?, , LTR/ERVL-MaLR, LTR/Gypsy, LTR/Gypsy?
    LTR,
    LTR_ERV1(LTR),
    LTR_ERVK(LTR),
    LTR_ERVL(LTR),
    LTR_ERVL_MaLR(LTR, LTR_ERVL),
    LTR_Gypsy(LTR),

    //SINE?, SINE/5S-Deu-L2, SINE/Alu, SINE/MIR, SINE?/tRNA, SINE/tRNA, SINE/tRNA-Deu, SINE/tRNA-RTE
    SINE,
    SINE_5SDeuL2(SINE),
    SINE_ALU(SINE),
    SINE_MIR(SINE),
    SINE_tRNA(SINE),
    SINE_tRNA_Deu(SINE, SINE_tRNA),
    SINE_tRNA_RTE(SINE, SINE_tRNA),

    //Retroposon/SVA
    RETROPOSON,

    //RC?/Helitron?
    //RC/Helitron
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
