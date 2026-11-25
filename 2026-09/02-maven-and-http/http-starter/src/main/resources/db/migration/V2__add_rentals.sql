alter table drivers
    add column rental_count integer not null default 0;

create table rentals (
    id         varchar(64) primary key,
    car_id     varchar(64) not null references cars (id) on delete cascade,
    driver_id  varchar(64) not null references drivers (id) on delete cascade,
    hours      bigint,
    distance   double precision,
    total_cost double precision,
    active     boolean     not null default true,
    started_at timestamp   not null default now(),
    ended_at   timestamp
);

insert into rentals (id, car_id, driver_id, active)
select 'rent-' || substring(md5(id || '-' || driver_id), 1, 8), id, driver_id, true
from cars
where driver_id is not null;

update drivers d
set rental_count = 1
where exists (select 1 from cars c where c.driver_id = d.id);
