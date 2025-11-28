.. _rstdownloads:

#########
Downloads
#########

SvAnna database files are available for download from the following locations:

=========  ================ ==============  ================================================================  =============================================  =========================================
 Version    Compatibility    Genome build                           URL                                                         Size                                          DOI
=========  ================ ==============  ================================================================  =============================================  =========================================
 2204       [1.0.0, 1.0.3)   hg38/GRCh38     https://zenodo.org/records/17747479/files/2204_hg38.svanna.zip      ~627 MB for download, ~2.3 GB unpacked       https://doi.org/10.5281/zenodo.17747479
 2304       [1.0.3, 1.1.0)   hg38/GRCh38     https://zenodo.org/records/17747798/files/2304_hg38.svanna.zip      ~674 MB for download, ~2.6 GB unpacked       https://doi.org/10.5281/zenodo.17747797
 2402       [1.1.0, 1.2.0)   hg38/GRCh38     https://zenodo.org/records/17748602/files/2402_hg38.svanna.zip      ~648 MB for download, ~2.1 GB unpacked       https://doi.org/10.5281/zenodo.17748601
 2511       [1.2.0, latest)  hg38/GRCh38     https://zenodo.org/records/17749179/files/2511_hg38.svanna.zip      ~339 MB for download, ~1.3 GB unpacked       https://doi.org/10.5281/zenodo.17749179
=========  ================ ==============  ================================================================  =============================================  =========================================

.. note::

  The *Compatibility* column indicates the SvAnna application version that is compatible
  with given database files *Version*. The bracket `[` is inclusive while `)` is exclusive.
  For instance version `2304` works with SvAnna versions `1.0.3` and `1.0.4` but *not* with `1.1.0`.

.. note::

  The database files were moved from Google Cloud to Zenodo.


Use ``curl`` or ``wget`` utilities to download the files from command line::

  $ wget https://storage.googleapis.com/svanna/2204_hg38.svanna.zip
  or
  $ curl --output 2304_hg38.svanna.zip https://storage.googleapis.com/svanna/2304_hg38.svanna.zip

Alternatively, use a GUI FTP client such as `FileZilla <https://filezilla-project.org/>`_.


*************************
Database folder structure
*************************

The folder structure differs a bit between the releases.

Since ``2511_hg38`` (newer releases), the database archive release has the following structure::

    svanna-data
      |- checksum.sha256
      |- gencode.v38.genes.json.gz
      \- svanna_db.mv.db


The database folders of older SvAnna releases, including ``2204_hg38``, ``2304_hg38``, and ``2402_hg38`` had the following structure::

    svanna-data
      |- checksum.sha256
      |- gencode.v38.genes.json.gz
      |- hp.json
      \- svanna_db.mv.db


************************
Check the file integrity
************************

Check that the download went well by running::

  $ sha256sum -c checksum.sha256

All lines should end with ``OK``.
