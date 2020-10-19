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



**Large deletion**

The following call represents a 34 million bp deletion on chromosome 15. In practice,
calls such as this of very large deletions may be artefacts.


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
