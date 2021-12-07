.. _rstoutputformats:

==============
Output formats
==============

SvAnna supports storing results in 4 output formats: *HTML*, *VCF* *CSV*, and *TSV*. Use the ``--output-format`` option
to select one or more of the desired output formats (e.g. ``--output-format html,vcf``).

HTML output format
^^^^^^^^^^^^^^^^^^

SvAnna creates an *HTML* file with the analysis summary and with variants sorted by the :math:`PSV` score
in descending order.
By default, top 100 variants are included into the report. The number of the reported variants can be adjusted by
the ``--report-top-variants`` option.

The report consists of several parts:

* *Analysis summary* - Details of HPO terms of the proband, paths of the input files, and the analysis parameters.
* *Variant counts* - Breakdown of the number of the variant types of the different categories.
* *Prioritized SVs* - Visualizations of the prioritized variants.

.. TODO - write more about the HTML report

.. note::
  Only the variants that passed all the filters are visualized in the *Prioritized SVs* section

The ``--no-breakends`` option excludes breakends/translocations from the report.

VCF output format
^^^^^^^^^^^^^^^^^
When including ``vcf`` into the ``--output-format`` option, a VCF file with all input variants is created.
The prioritization adds a novel *INFO* field to each variant:

* ``PSV`` - an *INFO* field containing :math:`PSV` score for the variant.

.. note::
  * ``--report-top-variants`` option has no effect for the *VCF* output format.
  * add ``--uncompressed-output`` flag if you want to get uncompressed VCF file


CSV/TSV output format
^^^^^^^^^^^^^^^^^^^^^
To write *n* most deleterious variants into a *CSV* (or *TSV*) file, use ``csv`` (``tsv``) in the ``--output-format`` option.

The results are written into a tabular file with the following columns:

* *contig* - name of the contig/chromosome (e.g. ``1``, ``2``, ``X``)
* *start* - 0-based start coordinate (excluded) of the variant on positive strand
* *end* - 0-based end coordinate (included) of the variant on positive strand
* *id* - variant ID as it was present in the input VCF file
* *vtype* - variant type, one of {``DEL``, ``DUP``, ``INV``, ``INS``, ``BND``, ``CNV``}
* *failed_filters* - the names of filters that the variant failed to pass. The names are separated by semicolon (``;``)
  * ``filter`` - the variant failed previous VCF filters - at least one filter flag is present in the variant VCF line, except for ``PASS``.
  * ``coverage`` - the variant is supported by less reads than specified by ``--min-read-support`` option
* *psv* - the :math:`PSV` score value

.. table:: Tabular output

  ======== ========= ========== ====== ======= ================= =====================
   contig    start      end       id    vtype   failed_filters         psv
  ======== ========= ========== ====== ======= ================= =====================
   11       31130456  31671718   abcd   DEL                       109.75766900764305
   18       46962113  46969912   efgh   DUP     filter;coverage   3.2
   ...      ...       ...        ...    ...     ...               ...
  ======== ========= ========== ====== ======= ================= =====================

.. note::
  add ``--uncompressed-output`` flag if you want to get uncompressed tabular file