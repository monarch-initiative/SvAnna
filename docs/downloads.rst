.. _rstdownloads:

=========
Downloads
=========

SvAnna database files are available for download from the following locations:

=========  ==============  ============================================================  =============================================
 Version    Genome build                           URL                                                      Size
=========  ==============  ============================================================  =============================================
 2204       hg38/GRCh38     https://storage.googleapis.com/svanna/2204_hg38.svanna.zip      ~627 MB for download, ~2.3 GB unpacked
=========  ==============  ============================================================  =============================================

Use ``curl`` or ``wget`` utilities to download the files from command line::

  $ wget https://storage.googleapis.com/svanna/2204_hg38.svanna.zip
  or
  $ curl --output 2204_hg38.svanna.zip https://storage.googleapis.com/svanna/2204_hg38.svanna.zip

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
