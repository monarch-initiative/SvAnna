package org.jax.l2o.vcf;

public class SvAnn {


    private final SvType svType;
    private final String chromA;
    private final String chromB;
    private int svLen;
    /**
     * Start position of the SV on chromosome A.
     */
    private final int startPos;
    /**
     * Start position of the SV on chromosome A.
     */
    private final int endPos;

    private SvAnn(SvType svT, String chrA, String chrB, int startpos, int endpos) {
        this.svType = svT;
        this.chromA = chrA;
        this.chromB = chrB;
        this.startPos = startpos;
        this.endPos = endpos;
    }

    public SvType getSvType() {
        return svType;
    }

    public String getChromA() {
        return chromA;
    }

    public String getChromB() {
        return chromB;
    }

    public int getSvLen() {
        return svLen;
    }

    public int getStartPos() {
        return startPos;
    }

    public int getEndPos() {
        return endPos;
    }

    private String deletionToString() {
        return String.format("[DELETION] %s:%d-%d", this.chromA, this.startPos, this.endPos);
    }

    @Override
    public String toString() {
        switch (this.svType) {
            case DELETION:
                return deletionToString();
            default:
                return String.format("SVTYPE TO DO %s", this.svType);
        }
    }


    public static class SvAnnBuilder {

        private SvType svType;
        private String chromA;
        private String chromB;
        private int svLen;
        /**
         * Start position of the SV on chromosome A.
         */
        private int startPos;
        /**
         * End position of the SV if it is also on Chromosome A. If not TODO.
         */
        private int endPos;

        public SvAnnBuilder(SvType stype) {
            svType = stype;
        }


        public SvAnnBuilder chromA(String chrA) {
            this.chromA = chrA;
            return this;
        }

        public SvAnnBuilder chromB(String chrB) {
            this.chromB = chrB;
            return this;
        }

        public SvAnnBuilder svlen(int n) {
            this.svLen = n;
            return this;
        }

        public SvAnnBuilder startPos(int p) {
            this.startPos = p;
            return this;
        }

        public SvAnnBuilder endPos(int p) {
            this.endPos = p;
            return this;
        }


        public SvAnn build() {
            return new SvAnn(this.svType, this.chromA, this.chromB, this.startPos, this.endPos);
        }

    }


}
