# README

This is how a database can be built.

Build the JAR:

```shell
./mvnw clean package
alias svanna-ingest="java -jar $(pwd)/svanna-ingest/target/svanna-ingest-*.jar"
svanna-ingest --help
```

## Generate the config

Generate the config file:

```shell
svanna-ingest generate-config 
```

The command generates a config file in the current working directory.

## Download FANTOM5 files (if necessary)

Edit the file (if necessary) and download the FANTOM5 resources.

```shell
DWN_DIR=/home/ielis/dub/data/svanna/download
svanna-ingest download -d ${DWN_DIR} 
```

## Build the database

```shell
ASSEMBLY=hg38
DB_VERSION=2511 # 👈 update
PARENT_DIR=/home/ielis/dub/data/clinical-long-read-genome/svanna
CONFIG_PATH=${PARENT_DIR}/${DB_VERSION}_${ASSEMBLY}.svanna-ingest-config.yaml
BUILD_DIR=${PARENT_DIR}/${DB_VERSION}_${ASSEMBLY}
svanna-ingest build-db --assembly ${ASSEMBLY} \
  --db-version ${DB_VERSION} \
  ${CONFIG_PATH} \
  ${BUILD_DIR}
```

During ingest, you may need to download some files manually due to network interrupts.
Last time, the `phenotype.hpoa` included a weird disease name for [#111400](https://omim.org/entry/111400),
which was manually removed from HPOA.
