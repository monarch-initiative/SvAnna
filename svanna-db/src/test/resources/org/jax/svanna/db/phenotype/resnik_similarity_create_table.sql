create schema if not exists SVANNA;
drop table if exists SVANNA.RESNIK_SIMILARITY;
create table SVANNA.RESNIK_SIMILARITY
(
    LEFT_ID    INT   not null, -- left term ID. The id for `HP:0001234` is 1234
    RIGHT_ID   INT   not null, -- right term ID
    SIMILARITY FLOAT not null  -- Resnik similarity value
);
create unique index SVANNA.RESNIK_SIMILARITY__IDX on SVANNA.RESNIK_SIMILARITY (LEFT_ID, RIGHT_ID);
