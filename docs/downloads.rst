.. _rstdownloads:

=========
Downloads
=========

SvAnna database files are available for download from the following locations:

=========  ================ ==============  ============================================================  =============================================
 Version    Compatibility    Genome build                           URL                                                      Size
=========  ================ ==============  ============================================================  =============================================
 2204       [1.0.0, 1.0.3)   hg38/GRCh38     https://storage.googleapis.com/svanna/2204_hg38.svanna.zip      ~627 MB for download, ~2.3 GB unpacked
 2304       [1.0.3, 1.1.0)   hg38/GRCh38     https://storage.googleapis.com/svanna/2304_hg38.svanna.zip      ~674 MB for download, ~2.6 GB unpacked
 2402       [1.1.0, latest)  hg38/GRCh38     https://storage.googleapis.com/svanna/2402_hg38.svanna.zip      ~648 MB for download, ~2.1 GB unpacked
=========  ================ ==============  ============================================================  =============================================

.. note::

  The *Compatibility* column indicates the SvAnna application version that is compatible
  with given database files *Version*. The bracket `[` is inclusive while `)` is exclusive.
  For instance version `2304` works with SvAnna versions `1.0.3` and `1.0.4` but *not* with `1.1.0`.

Use ``curl`` or ``wget`` utilities to download the files from command line::

  $ wget https://storage.googleapis.com/svanna/2204_hg38.svanna.zip
  or
  $ curl --output 2304_hg38.svanna.zip https://storage.googleapis.com/svanna/2304_hg38.svanna.zip

Alternatively, use a GUI FTP client such as `FileZilla <https://filezilla-project.org/>`_.

Unzip the archive to get a folder with the following structure::

    path/to/svanna-data
      |- checksum.sha256
      |- gencode.v38.genes.json.gz
      |- hp.json
      \- svanna_db.mv.db

In the example above, we use ``-d path/to/svanna-data`` to let SvAnna know about the resource data directory.

Check resource integrity
~~~~~~~~~~~~~~~~~~~~~~~~

Check that the download went well by running::

  $ sha256sum -c checksum.sha256

All lines should end with ``OK``.
