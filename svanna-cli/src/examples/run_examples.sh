#!/usr/bin/env bash
##
# This script will run SvAnna on 8 variants stored in `example.vcf` file.
# Note that the paths to SvAnna JAR, data directory, and the example VCFs must be adjusted below.
# The script will not run otherwise.


########################################################################################################################
# Adjust the paths to point to the real files

# SvAnna JAR
SVANNA_JAR=

# Path to SvAnna data directory.
DATA_DIRECTORY=

# Path to the VCF file with example variants.
EXAMPLE_VCF=

# Where to write the output VCF files.
OUTPUT_DIR=$(pwd)/output

#
########################################################################################################################

if [ -n "${SVANNA_JAR}" ]; then
  echo "Using SvAnna at" ${SVANNA_JAR}
else
  echo "You must set the variables in the script first"
  exit 1
fi

mkdir -p ${OUTPUT_DIR}

# NF1 (single-exon deletion)
java -jar ${SVANNA_JAR} prioritize -d ${DATA_DIRECTORY} --vcf ${EXAMPLE_VCF} --prefix ${OUTPUT_DIR}/NF1_single_exon_del --term HP:0007565 --term HP:0009732 --term HP:0009735 --term HP:0009736

# BRCA1 (multi-exon deletion)
java -jar ${SVANNA_JAR} prioritize -d ${DATA_DIRECTORY} --vcf ${EXAMPLE_VCF} --prefix ${OUTPUT_DIR}/BRCA1_multi_exon_del --term HP:0003002

# NPHP1 (multi-gene deletion)
java -jar ${SVANNA_JAR} prioritize -d ${DATA_DIRECTORY} --vcf ${EXAMPLE_VCF} --prefix ${OUTPUT_DIR}/NPHP1_multi_gene_del --term HP:0003774 --term HP:0001320 --term HP:0002078 --term HP:0000618 --term HP:0000508 --term HP:0002419 --term HP:0011933 --term HP:0002070 --term HP:0000543 --term HP:0000589

# PIBF1 (duplication)
java -jar ${SVANNA_JAR} prioritize -d ${DATA_DIRECTORY} --vcf ${EXAMPLE_VCF} --prefix ${OUTPUT_DIR}/PIBF1_dup --term HP:0032417 --term HP:0000076 --term HP:0002079 --term HP:0001541 --term HP:0000540 --term HP:0011968 --term HP:0001250 --term HP:0000490 --term HP:0001263 --term HP:0001284 --term HP:0002240 --term HP:0001290 --term HP:0031200 --term HP:0011800 --term HP:0000090 --term HP:0000092 --term HP:0001919 --term HP:0012650 --term HP:0002419 --term HP:0002119 --term HP:0000105

# BRPF1 (inversion)
java -jar ${SVANNA_JAR} prioritize -d ${DATA_DIRECTORY} --vcf ${EXAMPLE_VCF} --prefix ${OUTPUT_DIR}/BRPF1_multi_gene_inv --term HP:0000286 --term HP:0002069 --term HP:0000494 --term HP:0002342 --term HP:0000486 --term HP:0000750 --term HP:0000431 --term HP:0001252 --term HP:0002194 --term HP:0012368 --term HP:0011150 --term HP:0002949 --term HP:0000508 --term HP:0000316 --term HP:0000311

# AMER1 (transcription start site)
java -jar ${SVANNA_JAR} prioritize -d ${DATA_DIRECTORY} --vcf ${EXAMPLE_VCF} --prefix ${OUTPUT_DIR}/AMER1_tss_del --term HP:0001561 --term HP:0000750 --term HP:0002684 --term HP:0002781 --term HP:0000316 --term HP:0031367 --term HP:0002744 --term HP:0000256 --term HP:0001004

# VWF (promoter variant)
java -jar ${SVANNA_JAR} prioritize -d ${DATA_DIRECTORY} --vcf ${EXAMPLE_VCF} --prefix ${OUTPUT_DIR}/VWF_promoter_del --term HP:0011890 --term HP:0000978 --term HP:0012147

# SLC6A1 (translocation)
java -jar ${SVANNA_JAR} prioritize -d ${DATA_DIRECTORY} --vcf ${EXAMPLE_VCF} --prefix ${OUTPUT_DIR}/SLC6A1_multi_gene_translocation  --term HP:0000252 --term HP:0000446 --term HP:0000272 --term HP:0000219 --term HP:0000179 --term HP:0002650 --term HP:0002987 --term HP:0006380 --term HP:0001250 --term HP:0001263 --term HP:0001263 --term HP:0001276
