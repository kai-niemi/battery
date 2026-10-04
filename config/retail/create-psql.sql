create schema if not exists retail;

create table if not exists retail.product
(
    id          uuid           not null default gen_random_uuid(),
    sku         varchar(36)    not null,
    inventory   int            not null default 0,
    price       numeric(19, 2) not null,
    name        varchar(36)    not null,
    description jsonb          null,

    primary key (id)
);

create table if not exists retail.customer
(
    id         uuid        not null default gen_random_uuid(),
    email      varchar(64) not null,
    first_name varchar(64),
    last_name  varchar(64),

    primary key (id)
);

create table if not exists retail.orders
(
    id          uuid           not null default gen_random_uuid(),
    customer_id uuid           not null,
    total_price numeric(19, 2) not null,
    tags        varchar(36)    null,
    placed_at   date           not null default current_date,
    updated_at  timestamptz    not null default clock_timestamp(),
    status      varchar(128)   null,

    primary key (id)
);

create table if not exists retail.order_item
(
    order_id   uuid           not null,
    product_id uuid           not null,
    item_pos   int            not null,
    quantity   int            not null,
    unit_price numeric(19, 2) not null,

    primary key (order_id, product_id, item_pos)
);

-- alter table if exists retail.customer
--     add constraint if not exists uc_email unique (email);

-- alter table if exists retail.product
--     add constraint uc_product_sku unique (sku);

-- alter table retail.product
--     add constraint check_product_positive_inventory check (inventory >= 0);

-- alter table if exists retail.order_item
--     add constraint fk_order_item_ref_product
--         foreign key (product_id)
--             references retail.product;

-- alter table if exists retail.order_item
--     add constraint fk_order_item_ref_order
--         foreign key (order_id)
--             references retail.orders;

-- alter table if exists retail.orders
--     add constraint fk_order_ref_customer
--         foreign key (customer_id)
--             references retail.customer;

create index if not exists fk_order_item_ref_product_idx on retail.order_item (product_id);
create index if not exists fk_order_ref_customer_idx on retail.orders (customer_id);