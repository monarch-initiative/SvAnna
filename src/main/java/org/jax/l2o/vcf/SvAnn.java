package org.jax.l2o.vcf;


import org.jax.l2o.except.L2ORuntimeException;
import org.jax.l2o.lirical.LiricalHit;
import picocli.CommandLine;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Information related to a structural variant annotation by Jannovar.
 */
public class SvAnn implements Comparable<SvAnn> {

    private static final String PIPE_SYMBOL = Pattern.quote("|");
    private static final int UNINITIALIZED = -1;
    private static final String UNINITIALIZED_STRING = "";


    private final String chrom;
    private final int pos;
    private final String id;
    private final String ref;
    private final String alt;
    private final String qual;
    private final String filter;
    String info;
    private final String format;
    private final String gt;
    /** From the CIPOS VCF item, confidence interval of the position, e.f., CIPOS=-500,-500. */
    private int ciUp = UNINITIALIZED;
    private int ciDown = UNINITIALIZED;

    private int end = UNINITIALIZED;
    private List<String> categories;
    private Set<String> symbols = new HashSet<>();
    //private Set<String> mechanisms = new HashSet<>();
    private String mateId = UNINITIALIZED_STRING;
    private int mateDist = UNINITIALIZED;
    private SvType svtype;
    private int svlen = UNINITIALIZED;
    private boolean imprecise = false;

    private boolean isTranslocation = false;

    private Priority priority = Priority.UNKNOWN;

    private List<LiricalHit> hitlist = new ArrayList<>();

    public SvAnn(String chr,
            int pos,
    String id,
    String ref ,
    String alt ,
    String qual,
    String filter,
    String info,
    String format,
    String gt) {
        this.chrom = chr;
        this.pos = pos;
        this.id = id;
        this.ref = ref;
        this.alt = alt;
        this.qual = qual;
        this.filter = filter;
        this.info = info;
        this.format = format;
        this.gt = gt;
        this.categories = new ArrayList<String>();
        String [] fields = info.split(";");
        for (String f : fields) {
            //System.out.println("f="+f);
            if (f.startsWith("END=")) {
                this.end = Integer.parseInt(f.substring(4));
            } else if (f.startsWith("SVANN=")) {
                String cats = f.substring(6);
                this.categories = new ArrayList<>();
                for (String cat : cats.split("&")) {
                    if (cat.indexOf('|') > -1) {
                        String[] items = cat.split("\\|");
                        if (items.length < 7) {
                            throw new L2ORuntimeException("BAD Jannovar annots: items=" + cat);
                        }
                        Priority prio = Priority.fromString(items[1]);
                        if (prio.compareTo(priority)>0) {
                            priority = prio;
                        }
                        categories.add(items[0]);
                        symbols.add(items[2]);
                    }
                }
                if (f.indexOf("translocation")>0) {
                    isTranslocation = true;
                }
            } else if (f.startsWith("CIPOS=")) {
                String[] A = f.substring(6).split(",");
                if (A.length != 2) {
                    throw new RuntimeException("Bad CIPOS format: " + f);
                }
                ciUp = Integer.parseInt(A[0]);
                ciDown = Integer.parseInt(A[1]);
            } else if (f.startsWith("MATEID=")) {
                mateId = f.substring(7);
            } else if (f.startsWith("SVTYPE=")) {
                this.svtype = SvType.fromString(f.substring(7));
            } else if (f.startsWith("MATEDIST=")) {
                this.mateDist = Integer.parseInt(f.substring(9));
            } else if (f.startsWith("SVLEN=")) {
                this.svlen = Integer.parseInt(f.substring(6));
            } else if (f.equals("IMPRECISE")) {
                this.imprecise = true;
            } else if (f.startsWith("ANN=|||")) {
                continue; // nothing to do something like ANN=|||||||||||||||OTHER_MESSAGE
            } else if (f.startsWith("SHADOWED")) {
                continue; // todo what is SHADOWED?
            } else if (f.startsWith("SVMETHOD")) {
               continue;
            }else if (f.startsWith("CHR2")) {
                continue;
            } else if (f.startsWith("ZMW")) {
                continue;
            }else {
                System.err.println("Could not find annot " + f);
            }
        }
    }

    public boolean isTranslocation() {
        return this.svtype.equals(SvType.TRANSLOCATION) || isTranslocation;
    }

    public String getChrom() {
        return chrom;
    }

    public int getPos() {
        return pos;
    }

    public String getId() {
        return id;
    }

    public String getRef() {
        int N = ref.length();
        if (N<5) {
            return ref;
        } else if (N<21) {
            return String.format("%s (%d bp)", ref, N);
        } else {
            return String.format("%s[...]%s (%d bp)", ref.substring(0,7), ref.substring(N-7), N);
        }
    }

    public String getAlt() {
        int N = alt.length();
        if (N<5) {
            return alt;
        } else if (N<21) {
            return String.format("%s (%d bp)", alt, N);
        } else {
            return String.format("%s[...]%s (%d bp)", alt.substring(0,7), alt.substring(N-7), N);
        }
    }

    public String getQual() {
        return qual;
    }

    public String getFilter() {
        return filter;
    }

    public String getFormat() {
        return format;
    }

    public String getGt() {
        return gt;
    }

    public String getInfo() {
        return info;
    }

    public int getCiUp() {
        return ciUp;
    }

    public int getCiDown() {
        return ciDown;
    }

    public int getEnd() {
        return end;
    }

    public List<String> getCategories() {
        return categories;
    }

    public Set<String> getSymbols() {
        return symbols;
    }

    public String getMateId() {
        return mateId;
    }

    public int getMateDist() {
        return mateDist;
    }

    public SvType getSvtype() {
        return svtype;
    }

    public int getSvlen() {
        return svlen;
    }

    public boolean isImprecise() {
        return imprecise;
    }

    public boolean isLowPriority() {
        return this.priority == Priority.LOW;
    }

    public boolean isModifierPriority() {
        return this.priority == Priority.MODIFIER;
    }

    public boolean isHighPriority() {
        return this.priority == Priority.HIGH;
    }

    public void addLiricalHit(LiricalHit h) {
        this.hitlist.add(h);
    }

    public List<LiricalHit> getHitlist() {
        return hitlist;
    }

    public double getMaxPosteriorProb() {
        return this.hitlist
                .stream()
                .map(H -> H.getPosttestProbability())
                .max(Double::compareTo)
                .orElse(0.0);
    }

    private final static Comparator<SvAnn> COMPARATOR = new Comparator<SvAnn>() {
        @Override
        public int compare(SvAnn o1, SvAnn o2) {
            int c = o1.priority.compareTo(o2.priority);
            if (c==0) {
                double m1 = o1.getMaxPosteriorProb();
                double m2 = o2.getMaxPosteriorProb();
                if (m1 > m2) return 1;
                if (m2 > m1) return -1;
                return 0;
            }
            return c;
        }
    };


    @Override
    public int compareTo(SvAnn o) {
        return COMPARATOR.compare(this, o);
    }

    public String getBedLine() {
        int offset = 100;
        int start = -1;
        int end = -1;
        if (svtype.equals(SvType.DELETION)) {
            offset = Math.max(offset, svlen+100);
            start = this.pos - offset;
            end = this.pos + svlen + offset;
        } else if (svtype.equals(SvType.INSERTION)) {
            start = this.pos - offset;
            end = this.pos + offset;
        } else if (svtype.equals(SvType.TRANSLOCATION)) {
            start = this.pos - offset;
            end = this.pos + offset;
        }  else if (svtype.equals(SvType.DUPLICATION)) {
            offset = Math.max(offset, svlen+100);
            start = this.pos - offset;
            end = this.pos + svlen + offset;
        } else {
            throw new L2ORuntimeException("Did not recognize svtype in getBedLine");
        }
        return String.format("%s\t%d\t%d", this.chrom, start, end);
    }

    public String getIgvLine() {
        int offset = 1000;
        int start = -1;
        int end = -1;
        if (svtype.equals(SvType.DELETION)) {
            start = this.pos - offset;
            end = this.pos + svlen + offset;
        } else if (svtype.equals(SvType.INSERTION)) {
            start = this.pos - offset;
            end = this.pos + offset;
        } else if (svtype.equals(SvType.TRANSLOCATION)) {
            start = this.pos - offset;
            end = this.pos + offset;
        }  else if (svtype.equals(SvType.DUPLICATION)) {
            start = this.pos - offset;
            end = this.pos + svlen + offset;
        } else {
            return("Did not recognize svtype in getBedLine");
        }
        return String.format("%s\t%s:%d-%d",this.id, this.chrom, start, end);
    }
}
