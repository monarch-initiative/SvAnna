package org.jax.svanna.io.parse;

import htsjdk.variant.variantcontext.Genotype;
import htsjdk.variant.variantcontext.GenotypesContext;
import htsjdk.variant.variantcontext.VariantContext;
import org.jax.svanna.core.reference.VariantMetadata;
import org.jax.svanna.core.reference.Zygosity;

import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

class Utils {

    /**
     *
     */
    static final String IUPAC_BASES = "ACGTUWSMKRYBDHVNacgtuwsmkrybdhvn";
    private static final Charset ASCII = StandardCharsets.US_ASCII;
    private static final Map<Character, Character> IUPAC = makeIupacMap();
    private static final Map<Byte, Byte> IUPAC_COMPLEMENT_MAP = makeIupacByteMap(IUPAC);

    private Utils() {
        // static utility class
    }

    static String makeVariantRepresentation(VariantContext vc) {
        return String.format("%s-%d:(%s)", vc.getContig(), vc.getStart(), vc.getID());
    }

    /**
     * Get reverse complement of a sequence represented by <code>seq</code> array. The input array is expected to contain
     * bases encoded using US_ASCII charset.
     *
     * @param seq array with input sequence bases
     * @return a new array with length of <code>seq.length</code> containing reverse complement of the input sequence
     */
    static byte[] reverseComplement(byte[] seq) {
        byte[] reversed = new byte[seq.length];
        for (int i = 0; i < seq.length; i++) {
            reversed[seq.length - i - 1] = IUPAC_COMPLEMENT_MAP.get(seq[i]);
        }
        return reversed;
    }

    /**
     * Get reverse complement of a nucleotide sequence <code>seq</code>. The sequence is expected to consist of IUPAC
     * nucleotide symbols. Both upper/lower cases are recognized.
     *
     * @param seq nucleotide sequence to reverse complement
     * @return reverse complemented sequence
     */
    static String reverseComplement(String seq) {
        char[] oldSeq = seq.toCharArray();
        char[] newSeq = new char[oldSeq.length];
        for (int i = 0; i < oldSeq.length; i++) {
            newSeq[oldSeq.length - i - 1] = IUPAC.get(oldSeq[i]);
        }
        return new String(newSeq);
    }

    private static Map<Byte, Byte> makeIupacByteMap(Map<Character, Character> iupac) {
        return iupac.entrySet().stream()
                .collect(Collectors.toMap(
                        // awful, I know..
                        e -> ASCII.encode(CharBuffer.wrap(new char[]{e.getKey()})).get(0),
                        e -> ASCII.encode(CharBuffer.wrap(new char[]{e.getValue()})).get(0)));
    }

    private static Map<Character, Character> makeIupacMap() {
        Map<Character, Character> temporary = new HashMap<>();
        temporary.putAll(
                Map.of(
                        // STANDARD
                        'A', 'T',
                        'a', 't',
                        'C', 'G',
                        'c', 'g',
                        'G', 'C',
                        'g', 'c',
                        'T', 'A',
                        't', 'a',
                        'U', 'A',
                        'u', 'a'));
        temporary.putAll(
                Map.of(
                        // AMBIGUITY BASES - 1st part
                        'W', 'W', // weak - A,T
                        'w', 'w',
                        'S', 'S', // strong - C,G
                        's', 's',
                        'M', 'K', // amino - A,C
                        'm', 'k',
                        'K', 'M', // keto - G,T
                        'k', 'm'));
        temporary.putAll(
                Map.of(
                        // AMBIGUITY BASES - 2nd part
                        'R', 'Y', // purine - A,G
                        'r', 'y', // purine - A,G
                        'Y', 'R', // pyrimidine - C,T
                        'y', 'r')); // pyrimidine - C,T

        temporary.putAll(
                Map.of(
                        // AMBIGUITY BASES - 3rd part
                        'B', 'V', // not A
                        'b', 'v', // not A
                        'D', 'H', // not C
                        'd', 'h', // not C
                        'H', 'D', // not G
                        'h', 'd', // not G
                        'V', 'B', // not T
                        'v', 'b', // not T
                        'N', 'N', // any one base
                        'n', 'n' // any one base
                )
        );
        return temporary;
    }


    static Zygosity parseZygosity(int sampleIdx, GenotypesContext gts) {
        if (gts.isEmpty() || sampleIdx >= gts.size()) {
            return Zygosity.UNKNOWN;
        }
        Genotype gt = gts.get(sampleIdx);
        switch (gt.getType()) {
            case HET:
                return Zygosity.HETEROZYGOUS;
            case HOM_VAR:
                return Zygosity.HOMOZYGOUS;
            case NO_CALL:
            case UNAVAILABLE:
            default:
                return Zygosity.UNKNOWN;
        }
    }

    static int parseDepthFromGenotype(int sampleIdx, GenotypesContext genotypes) {
        if (genotypes.isEmpty() || sampleIdx >= genotypes.size()) {
            return VariantMetadata.MISSING_DEPTH_PLACEHOLDER;
        }
        Genotype gt = genotypes.get(sampleIdx);
        return gt.hasDP()
                ? gt.getDP()
                : VariantMetadata.MISSING_DEPTH_PLACEHOLDER;
    }

}
