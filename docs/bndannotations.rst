.. _bndannotations:

BND annotations
===============

This page explains how svann treats BND annotations. In the parsing of the VCF file, svann pairs together the two component
lines of paired BND annotations (single-breakend BNDs do not have a matepair and are treated separately). Consult
the `VCF 4.3 documentation <https://samtools.github.io/hts-specs/VCFv4.3.pdf>`_ for more details.

Deletion
^^^^^^^^




**Simple deletion**

The following table shows the contents of the ID field of the VCF file (`Mate ID`), the `CHROM`, `POS`,
`REF`, and `ALT` fields. The Matepair id is taken from the `MATEPAIR` element of the `INFO` field.


.. list-table:: Excerpt of the two VCF lines that describe a simple deletion
   :widths: 25 25 50 50 50 50
   :header-rows: 1

   * - Mate ID
     - Matepair ID
     - Chr
     - Pos
     - Ref
     - Alt
   * - BND.1
     - BND.2
     - chrX
     - 135754456
     - G
     - G[chrX:135935363[
   * - BND.2
     - BND.1
     - chrX
     - 135935363
     - G
     - ]chrX:135754456]G


The SV described by these two entries represents two breakends on the X chromosome. The first line specifies a breakend
right after the `G` base at position 135,754,456 on the X chromosome. The piece extending to the right starts at
chrX:135935363 and extends to the right thereafter. (Case 1, ``t[p[`` notation).

The second line describes the structural variant from the point of view of the other breakend. Here, the breakend is
located at chrX:135935363  -- the piece extending to the left of the `G` at this position is joined before it (Case 3,
``]p]t`` notation).

This corresponds to a deletion of 135935363 - 135754456 = 180,907 basepairs.

**Translocation**

.. list-table:: Excerpt of the two VCF lines that describe a translocation
   :widths: 25 25 50 50 50 50
   :header-rows: 1

   * - Mate ID
     - Matepair ID
     - Chr
     - Pos
     - Ref
     - Alt
   * - BND.1
     - BND.2
     - chr5
     - 170530664
     - C
     - C]chr17:45867415]
   * - BND.2
     - BND.1
     - chr17
     - 45867415
     - C
     - C]chr5:170530664]


This is a translocation between chromosomes 5 and 17. The base at the breakpoint of chromosome 5 is
a C at position 170,530,664, which is towards the 3' end of the q arm of chromosome 5, 5q35.1
(`visualize in UCSC <https://genome.ucsc.edu/cgi-bin/hgTracks?db=hg38&lastVirtModeType=default&lastVirtModeExtraState=&virtModeType=default&virtMode=0&nonVirtPosition=&position=chr5%3A170530664%2D170530664>`_).
The ``C]chr17:45867415]`` notation means that the reverse complementary sequence extending left of chr17:45867415
is joined after the C at position 170530664 of chromosome 5.

The ``C]chr5:170530664]`` notation means that the reverse complementary  sequence extending to the left of
chr17:45867415 is joined before the C at position 45,867,415 of chromosome 17
(`visualize in UCSC <https://genome.ucsc.edu/cgi-bin/hgTracks?db=hg38&lastVirtModeType=default&lastVirtModeExtraState=&virtModeType=default&virtMode=0&nonVirtPosition=&position=chr17%3A45867415%2D45867415>`_).


This is therefore a translocation between 5q and 17q.

Note this does not seem to make much sense, this would result in a translocation product with two centromeres.



**'Twisted' deletion**

The following call represents a 34 million bp deletion on chromosome 15. In practice,
calls such as this of very large deletions may be artefacts. The orientations
of the rejoined chromosomal segments have different orientations, for which reason we call
this a twisted deletion.


.. list-table:: Excerpt of the two VCF lines that describe a twisted deletion
   :widths: 25 25 50 50 50 50
   :header-rows: 1

   * - Mate ID
     - Matepair ID
     - Chr
     - Pos
     - Ref
     - Alt
   * - BND.1
     - BND.2
     - chr15
     - 41578122
     - T
     - [chr15:76259140[T
   * - BND.2
     - BND.1
     - chr15
     - 76259140
     - A
     - [chr15:41578122[A


This is a deletion on chromosome 15. The first line describes a breakend whereby
the reverse complement of the sequence extending right of chr15:76259140 is joined
before the T at position chr15:41578122  (``[p[t`` notation). This
breakend is at (15q15) (`visualize in UCSC <https://genome.ucsc.edu/cgi-bin/hgTracks?db=hg38&lastVirtModeType=default&virtModeType=default&virtMode=0&position=chr15%3A41578122%2D41578122>`_).

The other breakend is at position chr15:76259140, which is at 15q24.2
(`visualize in UCSC <https://genome.ucsc.edu/cgi-bin/hgTracks?db=hg38&lastVirtModeType=default&virtModeType=default&virtMode=0&position=chr15%3A76259140%2D76259140>`_).
The reverse complement of the sequence extending right from chr15:41578122
is joined before the A at position chr15:76259140 (``[p[t`` notation).

This would correspond to a deletion of 76259140-41578122=34681018 base pairs, i.e., 34.7 million bp.

Note that because both breakends have the ``[p[t`` notation, the relative orientation of the
two joined ends is different (if one is 5' to 3', the other is 3' to 5' with respect to the
canonical sequence).

Note that this deletion does not make sense because it would result in a chromosome without
a centromere.

**Twisted deletion (case2/case2)**

.. list-table:: Excerpt of the two VCF lines that describe a twisted deletion
   :widths: 25 25 50 50 50 50
   :header-rows: 1

   * - Mate ID
     - Matepair ID
     - Chr
     - Pos
     - Ref
     - Alt
   * - BND.1
     - BND.2
     - chr4
     - 49175170
     - G
     - G]chr4:49547618]
   * - BND.2
     - BND.1
     - chr4
     - 49547618
     - C
     - C]chr4:49175170]


This is a deletion on chromosome 4. The first line describes a breakend whereby
the reverse complement of the sequence extending left of chr4:49547618 is joined
after the G at position chr4:49175170  (``t]p]`` notation). This
breakend is near at 4p12 near the centromere (`visualize in UCSC <https://genome.ucsc.edu/cgi-bin/hgTracks?db=hg38&lastVirtModeType=default&virtModeType=default&virtMode=0&position=chr4%3A49175170%2D49175170>`_).

The other breakend is even closer to the centromere, chr4:49547618
(`visualize in UCSC <https://genome.ucsc.edu/cgi-bin/hgTracks?db=hg38&lastVirtModeType=default&virtModeType=default&virtMode=0&position=chr4%3A49547618%2D49547618>`_).
The reverse complement of the sequence extending left of chr4:4917517 is joined after the
C at position 49547618.

This is a deletion of 49547618-49175170=372448 basepairs (as denoted by the ``MATEDIST=372448`` entry in the INFO field).

Note that because both breakends have the ``t]p]`` notation, the relative orientation of the
two joined ends is different (if one is 5' to 3', the other is 3' to 5' with respect to the
canonical sequence).



**Simple deletion (case3/case1) **



.. list-table:: Excerpt of the two VCF lines that describe a twisted deletion
   :widths: 25 25 50 50 50 50
   :header-rows: 1

   * - Mate ID
     - Matepair ID
     - Chr
     - Pos
     - Ref
     - Alt
   * - BND.1
     - BND.2
     - chr21
     - 7280317
     - T
     - ]chr21:10757960]T
   * - BND.2
     - BND.1
     - chr21
     - 10757960
     - A
     - A[chr21:7280317[

This is a deletion on chromosome 21.

The first line specifies that the segment extending to the left of chr21:10757960 is
joined before the T at position 7280317 of chromosome 21.
(`visualize in UCSC <https://genome.ucsc.edu/cgi-bin/hgTracks?db=hg38&virtMode=0&position=chr21%3A7280317%2D7280317>`_).
This is the ``]p]t`` notation.

The second line specifies that segment extending to the right of chr21:7280317, which is at 21p11.2,
joined after the
A at position 10757960
(`visualize in UCSC <https://genome.ucsc.edu/cgi-bin/hgTracks?db=hg38&virtMode=0&position=chr21%3A10757960%2D10757960>`_).
This position is located near the q-terminal side of 21p11.2. This is the ``t[p[`` notation.

The result is a deletion on chromosome 21p11.2 of length 3,477,643.

This is a simple deletion, since the relative orientation of the two parts of chr21 is unchanged.



