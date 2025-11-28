.. _rstvista:

VISTA
=====

svanna uses data on enhancers to prioritize candidate regulatory variants. The ``svanna-ingest`` module contains code
for preparing enhancer data including data from VISTA. This module creates files that can be used directly by svanna and
most users will not need to generate the files themselves. The code and this documentation is provided for those
who may desire to reproduce or extend our results.



`VISTA <https://enhancer.lbl.gov/>`_ is a database of distant-acting transcriptional enhancers in the human and mouse
genomes.  As of 1/31/2021 the VISTA database contained information on 3148 in vivo tested elements - 1631 elements with
enhancer activity, with data from human and mouse. For our purposes, we import the positive data from human.


Transgenic mouse assays were used by the VISTA authors to investigate candidate enhancers. Each positive developmental
enhancer is annotated based on the observed spatial pattern of expression (More information is available on the
`VISTA <https://enhancer.lbl.gov/>`_ web portal and in `Vissel at el, 2007 <https://pubmed.ncbi.nlm.nih.gov/17130149/>`_).

Because the data is represented using hg19 coordinates, we use the UCSC liftOver tool to transform to hg38. We use the
Python script ``extractVistaEnhancers.py`` in the ``scripts`` folder for this. The script downloads the VISTA data, the
liftOver tool, and the chainFile, and performs a liftover to hg38 and relabels the enhancers using
`UBERON <https://www.ebi.ac.uk/ols/ontologies/uberon>`_ terms. It outputs a file called ``hg38-vista-enhancers.tsv``
that is used for in the ``svanna-ingest`` module to input the VISTA enhancers. This file has the following
structure.

.. csv-table:: hg38-vista-enhancers.tsv file structure
   :header: "name", "chr", "begin","end","tissues"
   :widths: 10, 10, 10,10,40

   "element 1","chr16","86396481","86397120","neural tube[UBERON:0001049]; presumptive hindbrain[UBERON:0007277]; limb[UBERON:0002101]; cranial nerve[UBERON:0001785]"
   "element 4","chr16","80338696","80339858","neural tube[UBERON:0001049]; presumptive hindbrain[UBERON:0007277]; presumptive midbrain[UBERON:0009616]"
   "element 12","chr16","78476711","78478047","presumptive hindbrain[UBERON:0007277]; forebrain[UBERON:0001890]"
