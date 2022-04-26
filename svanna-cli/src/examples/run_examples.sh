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
java -jar ${SVANNA_JAR} prioritize -d ${DATA_DIRECTORY} --vcf ${EXAMPLE_VCF} --prefix ${OUTPUT_DIR}/NF1_single_exon_del --phenotype-term HP:0007565 --phenotype-term HP:0009732 --phenotype-term HP:0009735 --phenotype-term HP:0009736

# BRCA1 (multi-exon deletion)
java -jar ${SVANNA_JAR} prioritize -d ${DATA_DIRECTORY} --vcf ${EXAMPLE_VCF} --prefix ${OUTPUT_DIR}/BRCA1_multi_exon_del --phenotype-term HP:0003002

# NPHP1 (multi-gene deletion)
java -jar ${SVANNA_JAR} prioritize -d ${DATA_DIRECTORY} --vcf ${EXAMPLE_VCF} --prefix ${OUTPUT_DIR}/NPHP1_multi_gene_del --phenotype-term HP:0003774 --phenotype-term HP:0001320 --phenotype-term HP:0002078 --phenotype-term HP:0000618 --phenotype-term HP:0000508 --phenotype-term HP:0002419 --phenotype-term HP:0011933 --phenotype-term HP:0002070 --phenotype-term HP:0000543 --phenotype-term HP:0000589

# PIBF1 (duplication)
java -jar ${SVANNA_JAR} prioritize -d ${DATA_DIRECTORY} --vcf ${EXAMPLE_VCF} --prefix ${OUTPUT_DIR}/PIBF1_dup --phenotype-term HP:0032417 --phenotype-term HP:0000076 --phenotype-term HP:0002079 --phenotype-term HP:0001541 --phenotype-term HP:0000540 --phenotype-term HP:0011968 --phenotype-term HP:0001250 --phenotype-term HP:0000490 --phenotype-term HP:0001263 --phenotype-term HP:0001284 --phenotype-term HP:0002240 --phenotype-term HP:0001290 --phenotype-term HP:0031200 --phenotype-term HP:0011800 --phenotype-term HP:0000090 --phenotype-term HP:0000092 --phenotype-term HP:0001919 --phenotype-term HP:0012650 --phenotype-term HP:0002419 --phenotype-term HP:0002119 --phenotype-term HP:0000105

# BRPF1 (inversion)
java -jar ${SVANNA_JAR} prioritize -d ${DATA_DIRECTORY} --vcf ${EXAMPLE_VCF} --prefix ${OUTPUT_DIR}/BRPF1_multi_gene_inv --phenotype-term HP:0000286 --phenotype-term HP:0002069 --phenotype-term HP:0000494 --phenotype-term HP:0002342 --phenotype-term HP:0000486 --phenotype-term HP:0000750 --phenotype-term HP:0000431 --phenotype-term HP:0001252 --phenotype-term HP:0002194 --phenotype-term HP:0012368 --phenotype-term HP:0011150 --phenotype-term HP:0002949 --phenotype-term HP:0000508 --phenotype-term HP:0000316 --phenotype-term HP:0000311

# AMER1 (transcription start site)
java -jar ${SVANNA_JAR} prioritize -d ${DATA_DIRECTORY} --vcf ${EXAMPLE_VCF} --prefix ${OUTPUT_DIR}/AMER1_tss_del --phenotype-term HP:0001561 --phenotype-term HP:0000750 --phenotype-term HP:0002684 --phenotype-term HP:0002781 --phenotype-term HP:0000316 --phenotype-term HP:0031367 --phenotype-term HP:0002744 --phenotype-term HP:0000256 --phenotype-term HP:0001004

# VWF (promoter variant)
java -jar ${SVANNA_JAR} prioritize -d ${DATA_DIRECTORY} --vcf ${EXAMPLE_VCF} --prefix ${OUTPUT_DIR}/VWF_promoter_del --phenotype-term HP:0011890 --phenotype-term HP:0000978 --phenotype-term HP:0012147

# SLC6A1 (translocation)
java -jar ${SVANNA_JAR} prioritize -d ${DATA_DIRECTORY} --vcf ${EXAMPLE_VCF} --prefix ${OUTPUT_DIR}/SLC6A1_multi_gene_translocation  --phenotype-term HP:0000252 --phenotype-term HP:0000446 --phenotype-term HP:0000272 --phenotype-term HP:0000219 --phenotype-term HP:0000179 --phenotype-term HP:0002650 --phenotype-term HP:0002987 --phenotype-term HP:0006380 --phenotype-term HP:0001250 --phenotype-term HP:0001263 --phenotype-term HP:0001263 --phenotype-term HP:0001276
