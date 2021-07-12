.. _rstrunning:

==========
Run SvAnna
==========

SvAnna is a command-line Java tool that runs with Java version 11 or higher.

Before using SvAnna, you must setup SvAnna as describe in the :ref:`rstsetup` section.

SvAnna provides a command for performing phenotype-driven prioritization of structural variants (SVs) stored in
VCF format.

In the examples below, we assume that ``svanna-config.yml`` points to a configuration file with correct locations of
SvAnna resources.

``prioritize`` - Prioritization of structural variants
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

The aim of this command is to perform phenotype-driven prioritization of SVs in VCF file. The prioritized variants are
stored in one or more :ref:`rstoutputformats`.

To prioritize variants in the `example.vcf`_ file (an example VCF file with 8 variants stored in SvAnna repository), run::

  java -jar svanna-cli.jar prioritize --config svanna-config.yaml --vcf example.vcf --term HP:0011890 --term HP:0000978 --term HP:0012147 --prefix /path/to/output

After the annotation, the results are stored at ``/path/to/output.html``.

Mandatory arguments
~~~~~~~~~~~~~~~~~~~

All CLI arguments for the ``prioritize`` command are supplied as *options* (no positional parameters).

There is one *mandatory* option:

* ``-c | --config`` - path to Squirls configuration file

Then, input data is specified either as a path to VCF file along with one or more HPO terms, or as a *Phenopacket*:

* ``--vcf`` - path to input VCF file
* ``-t | --term`` - HPO term describing clinical condition of the proband, may be specified multiple times (e.g. ``--term HP:1234567 --term HP:9876543``
* ``-p | --phenopacket`` - path to Phenopacket in JSON format

If both ``--vcf`` and ``--phenopacket`` options are specified, ``--vcf`` has a precedence and the phenopacket will *not*
be processed.

Optional arguments
~~~~~~~~~~~~~~~~~~

SvAnna allows to fine-tune the prioritization using the following *optional* options:

* ``--n-threads`` - prioritize variants using *n* threads to speed up the prioritization. More threads require more RAM (default ``2``)
* ``--min-read-support`` - minimum number of reads that must support presence of the *alt* allele in order for variant to be included in the analysis (default `3``)
* ``--overlap-threshold`` - threshold value to determine if the SV matches a variant from the population variant databases. The threshold is provided as a percentage (default ``80``)
* ``--no-breakends`` - do not report breakend variants in HTML report
* ``--frequency-threshold`` - threshold for labeling SVs in population variant databases *pv* as common. If query SV *v* overlaps with *pv* that has frequency above the threshold, then *v* is considered to be *common*.
* ``--output-format`` - comma separated list of output formats to use for writing the results. See :ref:`rstoutputformats` section for available output formats (default ``html``)
* ``--prefix`` - prefix for output files (default: based on the input VCF name)
* ``--report-top-variants`` - report top *n* variants in the HTML report (default: ``100``)
* ``--uncompressed-output`` - the tabular and VCF output files are compressed by default. Use this flag if you want to disable compressing the output files (default: ``false``)


See the next section to learn more about the SvAnna :ref:`rstoutputformats`.

.. _example.vcf: https://github.com/TheJacksonLaboratory/Squirls/blob/development/squirls-cli/src/examples/example.vcf