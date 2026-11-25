create table drivers (
    id          varchar(64)  primary key,
    name        varchar(100) not null,
    email       varchar(150) not null unique,
    age         integer      not null,
    experience  integer      not null,
    has_deposit boolean      not null
);

create table cars (
    id              varchar(64)      primary key,
    car_type        varchar(31)      not null,
    brand           varchar(100)     not null,
    model           varchar(100)     not null,
    plate_number    varchar(32)      not null unique,
    production_year integer          not null,
    mileage         double precision not null,
    status          varchar(32)      not null,
    driver_id       varchar(64) unique references drivers (id)
);
