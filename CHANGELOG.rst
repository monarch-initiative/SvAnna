=========
Changelog
=========

------
latest
------

------
v1.0.2
------

- ``SvAnna``
  - update dependency versions.
- ``svanna-core``
  - Fix bug that assigned wrong score to multi-gene inversions based on promoter overlap.

------
v1.0.1
------
- ``svanna-core``
  - Fix bug leading to wrong query coordinates for getting the population variants.
- ``svanna-cli``
  - Fix logback configuration.

------
v1.0.0
------
- ``SvAnna``
  - ``SvannaVariant`` *has* a ``GenomicVariant`` and it is *not* a ``GenomicVariant`` anymore.
  - Updated dependencies: phenol ``v2.0.0-RC2``, phenopackets ``v2.0.0``, protobuf ``v3.14.0``, spring-boot-starter-parent ``v2.6.7``.
  - Simplify the documentation.
  - Change the namespace.
- ``svanna-cli``
  - Remove the config YAML file and expose the CLI parameters instead.
- ``svanna-ingest``
  - Ingest the resources into a ZIP file, calculate ``sha256`` checksums.

----------
v1.0.0-RC5
----------
- changes required for benchmarking the other SV prioritization tools
- drop *Jannovar*

----------
v1.0.0-RC4
----------

- Treat deletion and duplications that affect CDS but do not change the reading frame in a milder way
- Drop the TAD idea and only evaluate the variant with respect to the overlapping genomic elements
- Externalize gene model and gene definition sources
- Report a track with dosage sensitive regions in the HTML report
- Store gene/disease/phenotype mapping in the database instead of the files in the data directory
- Improve logging - create a log file in the current working directory and store ``DEBUG`` info in the file

----------
v1.0.0-RC3
----------

- Bug fixes
  - fix null pointer that was thrown when processing translocation that involved non-primary contig (alt, unplaced, etc.)
  - fix incorrect generation of SVGs for translocation


----------
v1.0.0-RC2
----------

- Implement VCF output format
- Clean up the repo from the obsolete code
- Improve documentation & test coverage
- Bug fixes
  - remove null pointer in ``GeneService``
  - do not run coverage filter if the coverage data is missing for a variant


----------
v1.0.0-RC1
----------

- Rename ``annotate`` CLI command to ``prioritize``
- Multiple minor adjustments


------
v0.3.1
------

- Major progress in algorithm development


------
v0.3.0
------
- Adding phenopacket support
