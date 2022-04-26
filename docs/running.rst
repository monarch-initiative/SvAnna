.. _rstrunning:

==========
Run SvAnna
==========

SvAnna is a command-line Java tool that runs with Java version 11 or higher.

In the examples below, we assume that ``svanna-cli.jar`` points to the executable JAR file and
``svanna-data`` points to the data directory we created in the :ref:`rstsetup` section.

Prioritization of structural variants
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

SvAnna provides ``prioritize`` command for performing phenotype-driven prioritization of structural variants (SVs) stored in
VCF format. The prioritized variants are stored in one or more :ref:`rstoutputformats`.

To prioritize variants in the `example.vcf`_ file (an example VCF file with 8 variants stored in SvAnna repository), run::

  $ java -jar svanna-cli.jar prioritize -d svanna-data --vcf example.vcf --phenotype-term HP:0011890 --phenotype-term HP:0000978 --phenotype-term HP:0012147 --prefix results/example

After the run, the results are stored at ``results/example.html``.

Mandatory arguments
~~~~~~~~~~~~~~~~~~~

All CLI arguments for the ``prioritize`` command are supplied as *options* (no positional parameters).

There is one *mandatory* option:

* ``-d | --data-directory`` - path to SvAnna data directory.

Analysis input
##############

The input data can be specified in two ways: either as a path to a VCF file along with one or more HPO terms,
or as a *Phenopacket*:

* ``-p | --phenopacket`` - path to Phenopacket in JSON format.
* ``-t | --phenotype-term`` - HPO term describing clinical condition of the proband, may be specified multiple times (e.g. ``--term HP:1234567 --term HP:9876543``).
* ``--vcf`` - path to the input VCF file.

.. note::
  In case path to a VCF file is provided both in *phenopacket* and via ``--vcf`` option, the ``--vcf`` option has a precedence.

Optional parameters
~~~~~~~~~~~~~~~~~~~

SvAnna allows to fine-tune the prioritization using a number of *optional* parameters. For clarity, we group the options into several groups::

Run options
###########

* ``--frequency-threshold`` - threshold for labeling SVs in population variant databases *pv* as common.
  If query SV *v* overlaps with *pv* that has frequency above the threshold, then *v* is considered to be *common*.
  The value is provided as a percentage (default ``1``).
* ``--overlap-threshold`` - threshold to determine if a SV matches a variant from the population variant databases.
  The value is provided as a percentage (default ``80``).
* ``--min-read-support`` - minimum number of reads supporting the presence of the *alt* allele required
  to include a variant into the analysis (default `3``).
* ``--n-threads`` - number of threads used to prioritize the SVs (default ``2``).

Output options
##############

* ``--no-breakends`` - do not report breakends/translocations in the HTML report (default: ``false``).
* ``--output-format`` - comma separated list of output formats to use for writing the results (default ``html``).
.. note::
  See :ref:`rstoutputformats` section for more details.
* ``--prefix`` - prefix for output files (default: based on the input VCF name).
* ``--report-top-variants`` - include top *n* variants in the HTML report (default: ``100``).
.. note::
  Beware, the HTML report becomes rather large when including large number of variants.
* ``--uncompressed-output`` - the tabular and VCF output files are compressed by default.
  Use this flag if you want to disable compressing the output files (default: ``false``).

SvAnna configuration
####################

* ``--term-similarity-measure`` - phenotype term similarity measure, use one of ``{RESNIK_SYMMETRIC, RESNIK_ASYMETRIC}`` (default: RESNIK_SYMMETRIC).
* ``--ic-mica-mode`` - the mode for getting information content of the most informative common ancestors for terms :math:`t_1`, and :math:`t_2`.
  Use one of ``{DATABASE, IN_MEMORY}`` (default: ``DATABASE``).
* ``--promoter-length`` - number of bases pre-pended to a transcript and evaluated as a promoter region (default: ``2000``).
* ``--promoter-fitness-gain`` - set to ``0.`` to score the promoter variants as strictly as coding variants
  or to ``1.`` to completely disregard the promoter variants (default: ``0.6``).

See the next section to learn more about the SvAnna :ref:`rstoutputformats`,
and the :ref:`rstexamples` section to see how SvAnna prioritizes various SV classes.

.. _example.vcf: https://github.com/TheJacksonLaboratory/SvAnna/blob/master/svanna-cli/src/examples/example.vcf