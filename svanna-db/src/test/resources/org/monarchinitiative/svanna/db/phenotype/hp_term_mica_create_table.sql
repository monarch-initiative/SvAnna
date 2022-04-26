create schema if not exists SVANNA;
drop table if exists SVANNA.HP_TERM_MICA;
create table SVANNA.HP_TERM_MICA
(
    LEFT_VALUE  INT   not null, -- left term value. The value for `HP:0001234` is 1234
    RIGHT_VALUE INT   not null, -- right term value
    IC_MICA     FLOAT not null  -- information content of the most common informative ancestor
);
drop index if exists SVANNA.HP_TERM_MICA__IDX;
create unique index SVANNA.HP_TERM_MICA__IDX on SVANNA.HP_TERM_MICA (LEFT_VALUE, RIGHT_VALUE);
