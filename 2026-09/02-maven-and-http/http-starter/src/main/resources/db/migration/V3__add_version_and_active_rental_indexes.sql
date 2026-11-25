alter table cars
    add column version bigint not null default 0;

alter table drivers
    add column version bigint not null default 0;

create unique index uq_rentals_active_car on rentals (car_id) where active = true;
create unique index uq_rentals_active_driver on rentals (driver_id) where active = true;
