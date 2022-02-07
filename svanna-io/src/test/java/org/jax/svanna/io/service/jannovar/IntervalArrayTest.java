package org.jax.svanna.io.service.jannovar;

import org.junit.jupiter.api.Test;
import org.monarchinitiative.svart.*;
import org.monarchinitiative.svart.assembly.SequenceRole;
import org.monarchinitiative.svart.assembly.AssignedMoleculeType;
import org.monarchinitiative.sgenes.model.Located;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class IntervalArrayTest {

    private static final Contig CONTIG = Contig.of(1, "", SequenceRole.ASSEMBLED_MOLECULE, "", AssignedMoleculeType.CHROMOSOME, 100, "", "", "");


    class Triple implements Comparable<Triple>, Located {

        final GenomicRegion region;
        final String text;

        Triple(int beginPos, int endPos, String text) {
            this.region = GenomicRegion.of(CONTIG, Strand.POSITIVE, CoordinateSystem.zeroBased(), beginPos, endPos);
            this.text = text;
        }

        @Override
        public String toString() {
            return "Triple [beginPos=" + start() + ", endPos=" + end() + ", text=" + text + "]";
        }

        public int compareTo(Triple o) {
            final int result = (region.start() - o.start());
            if (result != 0)
                return result;
            return (region.end() - o.end());
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + getOuterType().hashCode();
            result = prime * result + start();
            result = prime * result + end();
            result = prime * result + ((text == null) ? 0 : text.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Triple other = (Triple) obj;
            if (!getOuterType().equals(other.getOuterType()))
                return false;
            if (start() != other.start())
                return false;
            if (end() != other.end())
                return false;
            if (text == null) {
                if (other.text != null)
                    return false;
            } else if (!text.equals(other.text))
                return false;
            return true;
        }

        private IntervalArrayTest getOuterType() {
            return IntervalArrayTest.this;
        }

        @Override
        public GenomicRegion location() {
            return region;
        }
    }

    class TripleEndExtractor implements IntervalEndExtractor<Triple> {

        public int getBegin(Triple triple) {
            return triple.start();
        }

        public int getEnd(Triple triple) {
            return triple.end();
        }

    }

    ArrayList<Triple> getList1() {
        ArrayList<Triple> lst = new ArrayList<>();

        lst.add(new Triple(1, 4, "a"));
        lst.add(new Triple(5, 9, "b"));
        lst.add(new Triple(4, 8, "c"));
        lst.add(new Triple(5, 7, "d"));
        lst.add(new Triple(16, 20, "e"));
        lst.add(new Triple(11, 16, "f"));
        lst.add(new Triple(30, 67, "g"));

        return lst;
    }

    ArrayList<Triple> getList2() {
        ArrayList<Triple> lst = new ArrayList<>();

        lst.add(new Triple(1, 9, "a"));
        lst.add(new Triple(2, 4, "b"));
        lst.add(new Triple(5, 8, "d"));
        lst.add(new Triple(4, 12, "c"));
        lst.add(new Triple(7, 13, "e"));
        lst.add(new Triple(9, 20, "f"));
        lst.add(new Triple(16, 20, "g"));
        lst.add(new Triple(17, 21, "h"));

        lst.add(new Triple(26, 31, "i"));
        lst.add(new Triple(27, 30, "j"));

        return lst;

    }

    ArrayList<Triple> getList3() {
        ArrayList<Triple> lst = new ArrayList<>();

        lst.add(new Triple(1, 9, "a"));
        lst.add(new Triple(2, 4, "b"));
        lst.add(new Triple(4, 15, "c"));
        lst.add(new Triple(5, 8, "d"));
        lst.add(new Triple(7, 13, "e"));
        lst.add(new Triple(9, 23, "f"));
        lst.add(new Triple(16, 20, "g"));
        lst.add(new Triple(17, 21, "h"));
        lst.add(new Triple(29, 34, "i"));
        lst.add(new Triple(30, 33, "j"));

        return lst;

    }

    ArrayList<Triple> getList4() {
        ArrayList<Triple> lst = new ArrayList<>();

        lst.add(new Triple(0, 11, "a"));
        lst.add(new Triple(15, 36, "b"));

        return lst;
    }

    @Test
    public void testSearchPub1() {
        IntervalArray<Triple> tree = new IntervalArray<>(getList1(), new TripleEndExtractor());
        IntervalArray<Triple>.QueryResult res = tree.findOverlappingWithInterval(1, 2);

        assertEquals(1, res.getEntries().size());
        assertEquals(new Triple(1, 4, "a"), res.getEntries().get(0));
    }

    @Test
    public void testSearchPub2() {
        IntervalArray<Triple> tree = new IntervalArray<>(getList2(), new TripleEndExtractor());
        IntervalArray<Triple>.QueryResult res = tree.findOverlappingWithInterval(13, 16);

        assertEquals(1, res.getEntries().size());
        assertEquals(new Triple(9, 20, "f"), res.getEntries().get(0));
    }

    @Test
    public void testSearchPub3() {
        IntervalArray<Triple> tree = new IntervalArray<>(getList3(), new TripleEndExtractor());
        IntervalArray<Triple>.QueryResult res = tree.findOverlappingWithInterval(30, 31);

        assertEquals(2, res.getEntries().size());
        assertEquals(new Triple(29, 34, "i"), res.getEntries().get(0));
        assertEquals(new Triple(30, 33, "j"), res.getEntries().get(1));
    }

    @Test
    public void testSearch1() {
        IntervalArray<Triple> tree = new IntervalArray<>(getList1(), new TripleEndExtractor());
        IntervalArray<Triple>.QueryResult res = tree.findOverlappingWithInterval(1, 3);

        assertEquals(1, res.getEntries().size());
        assertEquals(new Triple(1, 4, "a"), res.getEntries().get(0));
    }

    @Test
    public void testSearch2a() {
        IntervalArray<Triple> tree = new IntervalArray<>(getList1(), new TripleEndExtractor());
        IntervalArray<Triple>.QueryResult res = tree.findOverlappingWithInterval(6, 8);

        assertEquals(3, res.getEntries().size());
        assertEquals(new Triple(4, 8, "c"), res.getEntries().get(0));
        assertEquals(new Triple(5, 7, "d"), res.getEntries().get(1));
        assertEquals(new Triple(5, 9, "b"), res.getEntries().get(2));
    }

    @Test
    public void testSearch2b() {
        IntervalArray<Triple> tree = new IntervalArray<>(getList1(), new TripleEndExtractor());
        IntervalArray<Triple>.QueryResult res = tree.findOverlappingWithInterval(11, 13);

        assertEquals(1, res.getEntries().size());
        assertEquals(new Triple(11, 16, "f"), res.getEntries().get(0));
    }

    // Tests not finding any interval
    @Test
    public void testSearch3a() {
        IntervalArray<Triple> tree = new IntervalArray<>(getList1(), new TripleEndExtractor());
        IntervalArray<Triple>.QueryResult res = tree.findOverlappingWithInterval(20, 21);

        assertEquals(0, res.getEntries().size());
        assertEquals(new Triple(16, 20, "e"), res.getLeft());
        assertEquals(new Triple(30, 67, "g"), res.getRight());
    }

    // Tests not finding an interval but getting the right neighbor
    @Test
    public void testSearch3d() {
        IntervalArray<Triple> tree = new IntervalArray<>(getList1(), new TripleEndExtractor());
        IntervalArray<Triple>.QueryResult res = tree.findOverlappingWithInterval(512, 513);

        assertEquals(0, res.getEntries().size());
        assertEquals(new Triple(30, 67, "g"), res.getLeft());
        assertNull(res.getRight());
    }

    // Tests not finding an interval but getting the right neighbor
    @Test
    public void testSearch3e() {
        IntervalArray<Triple> tree = new IntervalArray<>(getList1(), new TripleEndExtractor());
        IntervalArray<Triple>.QueryResult res = tree.findOverlappingWithInterval(69, 70);

        assertEquals(0, res.getEntries().size());
        assertEquals(new Triple(30, 67, "g"), res.getLeft());
        assertNull(res.getRight());
    }

    // Tests median
    @Test
    public void testSearch100() {
        IntervalArray<Triple> tree = new IntervalArray<>(getList4(), new TripleEndExtractor());
        IntervalArray<Triple>.QueryResult res = tree.findOverlappingWithInterval(5, 6);

        assertEquals(1, res.getEntries().size());
        assertEquals(new Triple(0, 11, "a"), res.getEntries().get(0));
    }

    // Tests median
    @Test
    public void testSearch101() {
        IntervalArray<Triple> tree = new IntervalArray<>(getList4(), new TripleEndExtractor());
        IntervalArray<Triple>.QueryResult res = tree.findOverlappingWithInterval(25, 26);

        assertEquals(1, res.getEntries().size());
        assertEquals(new Triple(15, 36, "b"), res.getEntries().get(0));
    }

}