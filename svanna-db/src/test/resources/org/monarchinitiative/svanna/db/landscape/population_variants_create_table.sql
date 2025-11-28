create schema if not exists SVANNA;

drop table if exists SVANNA.POPULATION_VARIANTS;
create table SVANNA.POPULATION_VARIANTS
(
    CONTIG           INT          not null,
    START_POS        INT          not null,
    END_POS          INT          not null,

    ID               VARCHAR(200) not null,
    VARIANT_TYPE     VARCHAR(20)  not null,
    ORIGIN           VARCHAR(40)  not null,
    ALLELE_FREQUENCY FLOAT        not null
);
create index SVANNA.POPULATION_VARIANTS__CONTIG_START_ON_POS_END_ON_POS_IDX
    on SVANNA.POPULATION_VARIANTS (CONTIG, START_POS, END_POS);
