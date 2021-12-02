create schema if not exists SVANNA;

drop table if exists SVANNA.ENHANCERS;
create table SVANNA.ENHANCERS
(
    ENHANCER_ID      INT auto_increment,
    CONTIG           INT          not null,
    START            INT          not null, -- zero-based start on POSITIVE strand
    END              INT          not null, -- zero-based end on POSITIVE strand
    ENHANCER_SOURCE  VARCHAR(50)  not null,
    NAME             VARCHAR(255) not null,
    IS_DEVELOPMENTAL BOOL         not null,
    TAU              DOUBLE       not null
);

create index SVANNA.ENHANCERS__CONTIG_START_END_IDX
    on SVANNA.ENHANCERS (CONTIG, START, END);


drop table if exists SVANNA.ENHANCER_TISSUE_SPECIFICITY;
create table SVANNA.ENHANCER_TISSUE_SPECIFICITY
(
    ENHANCER_ID INT          not null,
    TERM_ID     VARCHAR(50)  not null,
    TERM_LABEL  VARCHAR(500) not null,
    HPO_ID      VARCHAR(50)  not null,
    HPO_LABEL   VARCHAR(500) not null,
    SPECIFICITY DOUBLE       not null
);

create index SVANNA.ENHANCER_TISSUE_SPECIFICITY__ENHANCER_ID
    on SVANNA.ENHANCER_TISSUE_SPECIFICITY (ENHANCER_ID);

---------------------------------- REPETITIVE REGIONS ------------------------------------------------------------------
drop table if exists SVANNA.REPETITIVE_REGIONS;
create table SVANNA.REPETITIVE_REGIONS
(
    CONTIG        INT         not null,
    START         INT         not null, -- zero-based start on POSITIVE strand
    END           INT         not null, -- zero-based end on POSITIVE strand
    REPEAT_FAMILY VARCHAR(50) not null
);
create
    index SVANNA.REPETITIVE_REGIONS__CONTIG_START_END_IDX
    on SVANNA.REPETITIVE_REGIONS (CONTIG, START, END);

---------------------------------- POPULATION VARIANTS -----------------------------------------------------------------
drop table if exists SVANNA.POPULATION_VARIANTS;
create table SVANNA.POPULATION_VARIANTS
(
    CONTIG           INT          not null,
    START            INT          not null, -- zero-based start on POSITIVE strand
    END              INT          not null, -- zero-based end on POSITIVE strand

    ID               VARCHAR(200) not null,
    VARIANT_TYPE     VARCHAR(20)  not null,
    ORIGIN           VARCHAR(40)  not null,
    ALLELE_FREQUENCY FLOAT        not null
);
create
    index SVANNA.POPULATION_VARIANTS__CONTIG_START_END_IDX
    on SVANNA.POPULATION_VARIANTS (CONTIG, START, END);

---------------------------------- TAD BOUNDARY ------------------------------------------------------------------------
drop table if exists SVANNA.TAD_BOUNDARY;
create table SVANNA.TAD_BOUNDARY
(
    CONTIG    INT          not null,
    START     INT          not null, -- zero-based start on POSITIVE strand
    END       INT          not null, -- zero-based end on POSITIVE strand
    MIDPOINT  INT          not null,
    ID        VARCHAR(200) not null,
    STABILITY FLOAT        not null
);

create index SVANNA.TAD_BOUNDARY__CONTIG_START_END_IDX
    on SVANNA.TAD_BOUNDARY (CONTIG, START, END);

create index SVANNA.TAD_BOUNDARY__CONTIG_MIDPOINT_IDX
    on SVANNA.TAD_BOUNDARY (CONTIG, MIDPOINT);

---------------------------------- IC MICA -----------------------------------------------------------------------------
drop table if exists SVANNA.HP_TERM_MICA;
create table SVANNA.HP_TERM_MICA
(
    LEFT_VALUE  INT   not null, -- left term value. The value for `HP:0001234` is 1234
    RIGHT_VALUE INT   not null, -- right term value
    IC_MICA     FLOAT not null  -- information content of the most common informative ancestor
);
drop index if exists SVANNA.HP_TERM_MICA__LEFT_VALUE_RIGHT_VALUE_IDX;
create unique index SVANNA.HP_TERM_MICA__LEFT_VALUE_RIGHT_VALUE_IDX on SVANNA.HP_TERM_MICA (LEFT_VALUE, RIGHT_VALUE);

---------------------------------- CLINGEN DOSAGE ELEMENT --------------------------------------------------------------
drop table if exists SVANNA.CLINGEN_DOSAGE_ELEMENT;
create table SVANNA.CLINGEN_DOSAGE_ELEMENT
(
    CONTIG             INT          not null,
    START              INT          not null, -- zero-based start on POSITIVE strand
    END                INT          not null, -- zero-based end on POSITIVE strand

    ID                 VARCHAR(200) not null, -- HGNC ID or other ID if available
    DOSAGE_SENSITIVITY VARCHAR(20)  not null,
    DOSAGE_EVIDENCE    VARCHAR(20)  not null
);
drop index if exists SVANNA.CLINGEN_DOSAGE_ELEMENT__CONTIG_START_END_IDX;
create index SVANNA.CLINGEN_DOSAGE_ELEMENT__CONTIG_START_END_IDX on SVANNA.CLINGEN_DOSAGE_ELEMENT (CONTIG, START, END);

-- TODO - we should update DA layer to use numeric IDs, where available
drop index if exists SVANNA.CLINGEN_DOSAGE_ELEMENT__ID;
create index SVANNA.CLINGEN_DOSAGE_ELEMENT__ID on SVANNA.CLINGEN_DOSAGE_ELEMENT (ID);

---------------------------------- PHENOTYPE DATA ----------------------------------------------------------------------

drop table if exists SVANNA.GENE_IDENTIFIER;
create table SVANNA.GENE_IDENTIFIER
(
    ACCESSION VARCHAR(50) not null, -- primary gene accession
    SYMBOL    VARCHAR(50) not null, -- HGVS gene symbol
    HGNC_ID   INT,                  -- integral part of HGNC gene id
    NCBI_GENE INT                   -- integral part of NCBIGene/Entrez gene ID
);
drop index if exists SVANNA.GENE_IDENTIFIER__ACCESSION;
create index SVANNA.GENE_IDENTIFIER__ACCESSION on SVANNA.GENE_IDENTIFIER (ACCESSION);
drop index if exists SVANNA.GENE_IDENTIFIER__HGNC_ID;
create index SVANNA.GENE_IDENTIFIER__HGNC_ID on SVANNA.GENE_IDENTIFIER (HGNC_ID);



drop table if exists SVANNA.GENE_TO_DISEASE;
create table SVANNA.GENE_TO_DISEASE
(
    HGNC_ID    INT         not null, -- integral part of HGNC gene id
    DISEASE_ID VARCHAR(50) not null  -- e.g. OMIM:123456
);
drop index if exists SVANNA.GENE_TO_DISEASE__DISEASE_ID;
create index SVANNA.GENE_TO_DISEASE__DISEASE_ID on SVANNA.GENE_TO_DISEASE (DISEASE_ID);
drop index if exists SVANNA.GENE_TO_DISEASE__HGNC_ID;
create index SVANNA.GENE_TO_DISEASE__HGNC_ID on SVANNA.GENE_TO_DISEASE (HGNC_ID);



drop table if exists SVANNA.HPO_DISEASE_SUMMARY;
create table SVANNA.HPO_DISEASE_SUMMARY
(
    DISEASE_ID   VARCHAR(50)  not null, -- e.g. OMIM:123456
    DISEASE_NAME VARCHAR(200) not null
);
drop index if exists SVANNA.HPO_DISEASE_SUMMARY__DISEASE_ID;
create unique index SVANNA.HPO_DISEASE_SUMMARY__DISEASE_ID on SVANNA.HPO_DISEASE_SUMMARY (DISEASE_ID);



drop table if exists SVANNA.DISEASE_TO_PHENOTYPE;
create table SVANNA.DISEASE_TO_PHENOTYPE
(
    DISEASE_ID VARCHAR(50) not null, -- e.g. OMIM:123456, maps to SVANNA.HPO_DISEASE_SUMMARY.DISEASE_ID
    TERM_ID    CHAR(10)    not null -- `HP`, `:`, and exactly 7 digits
);
drop index if exists SVANNA.DISEASE_TO_PHENOTYPE__DISEASE_ID;
create index SVANNA.DISEASE_TO_PHENOTYPE__DISEASE_ID on SVANNA.DISEASE_TO_PHENOTYPE (DISEASE_ID);
