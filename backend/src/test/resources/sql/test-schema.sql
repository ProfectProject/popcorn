-- H2 compatible test schema

CREATE TYPE IF NOT EXISTS user_role AS ENUM ('CUSTOMER', 'OWNER', 'MANAGER');
CREATE TYPE IF NOT EXISTS store_status AS ENUM ('DRAFT', 'PENDING', 'ACTIVE', 'SUSPENDED', 'CLOSED', 'HIDDEN');
CREATE TYPE IF NOT EXISTS popup_status AS ENUM ('DRAFT', 'REQUEST', 'APPROVED', 'OPEN', 'CLOSED', 'CANCELLED', 'HIDDEN');
CREATE TYPE IF NOT EXISTS popup_category AS ENUM ('FOOD','IDOL','EXHIBITION','WORKSHOP','FASHION','BEAUTY','LIFESTYLE','ART','GAME','TECH','SPORTS','BOOK','PET','ETC');
CREATE TYPE IF NOT EXISTS order_status AS ENUM ('REQUESTED','ACCEPTED','REJECTED','RESERVED','PAYMENT_PENDING','PAID','COMPLETED','CANCELLED');
CREATE TYPE IF NOT EXISTS payment_method AS ENUM ('CARD','TRANSFER','EASY_PAY');
CREATE TYPE IF NOT EXISTS payment_status AS ENUM ('READY','PAID','FAILED','CANCELLED');
CREATE TABLE IF NOT EXISTS p_users (
    user_id     BIGSERIAL NOT NULL PRIMARY KEY,
    password    varchar(255) NOT NULL,
    name        varchar(100) NOT NULL,
    phone       varchar(11),
    email       varchar(255) NOT NULL,
    role        user_role NOT NULL,
    is_active   boolean NOT NULL DEFAULT true,
    created_at  timestamp NOT NULL,
    updated_at  timestamp NOT NULL,
    deleted_at  timestamp,
    created_by  BIGINT,
    updated_by  BIGINT,
    deleted_by  BIGINT
);

CREATE TABLE IF NOT EXISTS p_customer_addresses (
    addr_id     UUID NOT NULL PRIMARY KEY,
    user_id     BIGINT NOT NULL,
    addr_name   varchar(50) NOT NULL,
    address1    varchar(255) NOT NULL,
    address2    varchar(255),
    postal_code varchar(10),
    is_default  boolean NOT NULL DEFAULT false,
    created_at  timestamp NOT NULL,
    updated_at  timestamp NOT NULL,
    deleted_at  timestamp,
    created_by  BIGINT,
    updated_by  BIGINT,
    deleted_by  BIGINT
);

CREATE TABLE IF NOT EXISTS p_stores (
    store_id    UUID NOT NULL PRIMARY KEY,
    user_id     BIGINT NOT NULL,
    store_name  varchar(100) NOT NULL,
    status      store_status NOT NULL DEFAULT 'DRAFT',
    reason      varchar(500),
    created_at  timestamp NOT NULL,
    updated_at  timestamp NOT NULL,
    deleted_at  timestamp,
    created_by  BIGINT,
    updated_by  BIGINT,
    deleted_by  BIGINT
);

CREATE TABLE IF NOT EXISTS p_popups (
    popup_id    UUID NOT NULL PRIMARY KEY,
    store_id    UUID NOT NULL,
    title       varchar(200) NOT NULL,
    description text,
    category    popup_category NOT NULL,
    status      popup_status NOT NULL,
    created_at  timestamp NOT NULL,
    updated_at  timestamp NOT NULL,
    deleted_at  timestamp,
    created_by  BIGINT,
    updated_by  BIGINT,
    deleted_by  BIGINT
);

CREATE TABLE IF NOT EXISTS p_popup_schedules (
    schedule_id UUID NOT NULL PRIMARY KEY,
    popup_id    UUID NOT NULL,
    start_at    timestamp NOT NULL,
    end_at      timestamp NOT NULL,
    price       int NOT NULL,
    capacity    int NOT NULL,
    remaining_capacity int NOT NULL,
    is_active   boolean NOT NULL DEFAULT false,
    created_at  timestamp NOT NULL,
    updated_at  timestamp NOT NULL,
    deleted_at  timestamp,
    created_by  BIGINT,
    updated_by  BIGINT,
    deleted_by  BIGINT
);

CREATE TABLE IF NOT EXISTS p_goods_variants (
    goods_id    UUID NOT NULL PRIMARY KEY,
    popup_id    UUID NOT NULL,
    stock_unit  varchar(64),
    goods_name  varchar(100) NOT NULL,
    goods_price int NOT NULL,
    stock       int NOT NULL,
    is_active   boolean DEFAULT true,
    created_at  timestamp NOT NULL,
    updated_at  timestamp NOT NULL,
    deleted_at  timestamp,
    created_by  BIGINT,
    updated_by  BIGINT,
    deleted_by  BIGINT
);

CREATE TABLE IF NOT EXISTS p_orders (
    order_id          UUID NOT NULL PRIMARY KEY,
    order_no          varchar(32),
    user_id           BIGINT NOT NULL,
    store_id          UUID NOT NULL,
    status            order_status NOT NULL,
    cancelable_until  timestamp,
    total_price       int NOT NULL,
    created_at        timestamp NOT NULL,
    updated_at        timestamp NOT NULL,
    deleted_at        timestamp,
    created_by        BIGINT,
    updated_by        BIGINT,
    deleted_by        BIGINT
);

CREATE TABLE IF NOT EXISTS p_order_goods (
    order_goods_id     UUID NOT NULL PRIMARY KEY,
    order_id           UUID NOT NULL,
    schedule_id        UUID,
    goods_variant_id   UUID,
    qty                int NOT NULL,
    unit_price         int NOT NULL,
    price              int NOT NULL,
    created_at         timestamp NOT NULL,
    updated_at         timestamp NOT NULL,
    deleted_at         timestamp,
    created_by         BIGINT,
    updated_by         BIGINT,
    deleted_by         BIGINT
);

CREATE TABLE IF NOT EXISTS p_payments (
    payment_id  UUID NOT NULL PRIMARY KEY,
    order_id    UUID NOT NULL,
    method      payment_method NOT NULL,
    status      payment_status NOT NULL,
    amount      int NOT NULL,
    raw_payload text,
    approved_at timestamp,
    created_at  timestamp NOT NULL,
    updated_at  timestamp NOT NULL,
    deleted_at  timestamp,
    created_by  BIGINT,
    updated_by  BIGINT,
    deleted_by  BIGINT
);

CREATE TABLE IF NOT EXISTS p_order_status_histories (
    order_status_id UUID NOT NULL PRIMARY KEY,
    order_id        UUID NOT NULL,
    from_status     order_status NOT NULL,
    to_status       order_status NOT NULL,
    reason          varchar(255),
    changed_at      timestamp NOT NULL,
    created_at      timestamp NOT NULL,
    updated_at      timestamp NOT NULL,
    deleted_at      timestamp,
    created_by      BIGINT,
    updated_by      BIGINT,
    deleted_by      BIGINT
);

CREATE TABLE IF NOT EXISTS p_order_qr_codes (
    qr_id      UUID NOT NULL PRIMARY KEY,
    order_id   UUID NOT NULL,
    qr_code    varchar(255) NOT NULL,
    expires_at timestamp,
    created_at timestamp NOT NULL,
    created_by BIGINT
);

CREATE TABLE IF NOT EXISTS p_checkins (
    checkin_id       UUID NOT NULL PRIMARY KEY,
    order_id         UUID NOT NULL,
    order_qr_code_id UUID NOT NULL,
    created_at       timestamp NOT NULL,
    created_by       BIGINT
);

-- Unique constraints - only add if they don't already exist
ALTER TABLE p_users  ADD CONSTRAINT IF NOT EXISTS uq_p_users_email UNIQUE (email);
ALTER TABLE p_orders ADD CONSTRAINT IF NOT EXISTS uq_p_orders_order_no UNIQUE (order_no);

-- Foreign Keys - only add if they don't already exist
ALTER TABLE p_customer_addresses     ADD CONSTRAINT IF NOT EXISTS fk_addr_user           FOREIGN KEY (user_id) REFERENCES p_users(user_id);
ALTER TABLE p_stores                 ADD CONSTRAINT IF NOT EXISTS fk_stores_owner        FOREIGN KEY (user_id) REFERENCES p_users(user_id);
ALTER TABLE p_popups                 ADD CONSTRAINT IF NOT EXISTS fk_popups_store        FOREIGN KEY (store_id) REFERENCES p_stores(store_id);
ALTER TABLE p_popup_schedules        ADD CONSTRAINT IF NOT EXISTS fk_schedules_popup     FOREIGN KEY (popup_id) REFERENCES p_popups(popup_id);
ALTER TABLE p_goods_variants         ADD CONSTRAINT IF NOT EXISTS fk_goods_popup         FOREIGN KEY (popup_id) REFERENCES p_popups(popup_id);
ALTER TABLE p_orders                 ADD CONSTRAINT IF NOT EXISTS fk_orders_user         FOREIGN KEY (user_id) REFERENCES p_users(user_id);
ALTER TABLE p_orders                 ADD CONSTRAINT IF NOT EXISTS fk_orders_store        FOREIGN KEY (store_id) REFERENCES p_stores(store_id);
ALTER TABLE p_order_goods            ADD CONSTRAINT IF NOT EXISTS fk_order_goods_order   FOREIGN KEY (order_id) REFERENCES p_orders(order_id);
ALTER TABLE p_order_goods            ADD CONSTRAINT IF NOT EXISTS fk_order_goods_schedule FOREIGN KEY (schedule_id) REFERENCES p_popup_schedules(schedule_id);
ALTER TABLE p_order_goods            ADD CONSTRAINT IF NOT EXISTS fk_order_goods_variant FOREIGN KEY (goods_variant_id) REFERENCES p_goods_variants(goods_id);
ALTER TABLE p_payments               ADD CONSTRAINT IF NOT EXISTS fk_payments_order      FOREIGN KEY (order_id) REFERENCES p_orders(order_id);
ALTER TABLE p_order_status_histories ADD CONSTRAINT IF NOT EXISTS fk_order_status_order  FOREIGN KEY (order_id) REFERENCES p_orders(order_id);
ALTER TABLE p_order_qr_codes         ADD CONSTRAINT IF NOT EXISTS fk_qr_order            FOREIGN KEY (order_id) REFERENCES p_orders(order_id);
ALTER TABLE p_checkins               ADD CONSTRAINT IF NOT EXISTS fk_checkins_order      FOREIGN KEY (order_id) REFERENCES p_orders(order_id);
ALTER TABLE p_checkins               ADD CONSTRAINT IF NOT EXISTS fk_checkins_qr         FOREIGN KEY (order_qr_code_id) REFERENCES p_order_qr_codes(qr_id);
