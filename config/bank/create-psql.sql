create schema if not exists bank;

create table if not exists bank.account
(
    id             uuid           not null default gen_random_uuid(),
    city           varchar(128)   not null,
    balance        decimal(19, 3) not null,
    name           varchar(128)   not null,
    allow_negative integer        not null default 0,
    updated_at     timestamptz    not null default clock_timestamp(),

    primary key (id)
);

create table if not exists bank.transfer
(
    id            uuid         not null default gen_random_uuid(),
    city          varchar(128) not null,
    booking_date  date         not null default current_date,
    transfer_date date         not null default current_date,

    primary key (id)
);

create table if not exists bank.transfer_item
(
    transfer_id           uuid           not null,
    account_id            uuid           not null,
    city                  varchar(128)   not null,
    amount                decimal(19, 3) not null,
    running_balance       decimal(19, 3) not null,

    primary key (transfer_id, account_id)
);

create index if not exists account_city_idx on bank.account (city);

create index if not exists transfer_idx on bank.transfer (city);

create index if not exists transfer_item_idx on bank.transfer_item (city);
