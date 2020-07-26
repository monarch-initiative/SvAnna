package org.jax.l2o.vcf;


import org.jax.l2o.except.L2ORuntimeException;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Information related to a structural variant annotation by Jannovar.
 */
public class SvAnn {

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
    //String info,
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
    private String svtype = UNINITIALIZED_STRING;
    private int svlen = UNINITIALIZED;
    private boolean imprecise = false;

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
                        if (items.length == 14) {
                            throw new L2ORuntimeException("BAD Jannovar annots: items=" + cat);
                        }
                        categories.add(items[0]);
                        symbols.add(items[2]);
                    }
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
                this.svtype = f.substring(7);
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
            } else {
                throw new RuntimeException("Could not find annot " + f);
            }
        }
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
        return ref;
    }

    public String getAlt() {
        return alt;
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

    public String getSvtype() {
        return svtype;
    }

    public int getSvlen() {
        return svlen;
    }

    public boolean isImprecise() {
        return imprecise;
    }
}
