-- H2 compatible test schema
CREATE TABLE IF NOT EXISTS p_users (
    user_id     BIGINT NOT NULL,
    password    varchar(255) NOT NULL,
    name        varchar(100) NOT NULL,
    phone       varchar(11),
    email       varchar(255) NOT NULL,
    role        varchar(255) NOT NULL,
    is_active   boolean NOT NULL DEFAULT true,
    created_at  timestamp NOT NULL,
    updated_at  timestamp NOT NULL,
    deleted_at  timestamp,
    created_by  BIGINT,
    updated_by  BIGINT,
    deleted_by  BIGINT
);

CREATE TABLE IF NOT EXISTS p_customer_addresses (
    addr_id     UUID NOT NULL,
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
    store_id    UUID NOT NULL,
    user_id     BIGINT NOT NULL,
    store_name  varchar(100) NOT NULL,
    status      varchar(255) NOT NULL DEFAULT 'DRAFT',
    reason      varchar(500),
    created_at  timestamp NOT NULL,
    updated_at  timestamp NOT NULL,
    deleted_at  timestamp,
    created_by  BIGINT,
    updated_by  BIGINT,
    deleted_by  BIGINT
);

CREATE TABLE IF NOT EXISTS p_popups (
    popup_id    UUID NOT NULL,
    store_id    UUID NOT NULL,
    title       varchar(200) NOT NULL,
    description text,
    category    varchar(255) NOT NULL,
    status      varchar(255) NOT NULL,
    created_at  timestamp NOT NULL,
    updated_at  timestamp NOT NULL,
    deleted_at  timestamp,
    created_by  BIGINT,
    updated_by  BIGINT,
    deleted_by  BIGINT
);

CREATE TABLE IF NOT EXISTS p_popup_schedules (
    schedule_id UUID NOT NULL,
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
    goods_id    UUID NOT NULL,
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
    order_id          UUID NOT NULL,
    order_no          varchar(32),
    user_id           BIGINT NOT NULL,
    store_id          UUID NOT NULL,
    status            varchar(255) NOT NULL,
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
    order_goods_id     UUID NOT NULL,
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
    payment_id  UUID NOT NULL,
    order_id    UUID NOT NULL,
    method      varchar(255) NOT NULL,
    status      varchar(255) NOT NULL,
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
    order_status_id UUID NOT NULL,
    order_id        UUID NOT NULL,
    from_status     varchar(255) NOT NULL,
    to_status       varchar(255) NOT NULL,
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
    qr_id      UUID NOT NULL,
    order_id   UUID NOT NULL,
    qr_code    varchar(255) NOT NULL,
    expires_at timestamp,
    created_at timestamp NOT NULL,
    created_by BIGINT
);

CREATE TABLE IF NOT EXISTS p_checkins (
    checkin_id       UUID NOT NULL,
    order_id         UUID NOT NULL,
    order_qr_code_id UUID NOT NULL,
    created_at       timestamp NOT NULL,
    created_by       BIGINT
);

-- Primary Keys
ALTER TABLE p_users                   ADD CONSTRAINT pk_p_users PRIMARY KEY (user_id);
ALTER TABLE p_customer_addresses      ADD CONSTRAINT pk_p_customer_addresses PRIMARY KEY (addr_id);
ALTER TABLE p_stores                  ADD CONSTRAINT pk_p_stores PRIMARY KEY (store_id);
ALTER TABLE p_popups                  ADD CONSTRAINT pk_p_popups PRIMARY KEY (popup_id);
ALTER TABLE p_popup_schedules         ADD CONSTRAINT pk_p_popup_schedules PRIMARY KEY (schedule_id);
ALTER TABLE p_goods_variants          ADD CONSTRAINT pk_p_goods_variants PRIMARY KEY (goods_id);
ALTER TABLE p_orders                  ADD CONSTRAINT pk_p_orders PRIMARY KEY (order_id);
ALTER TABLE p_order_goods             ADD CONSTRAINT pk_p_order_goods PRIMARY KEY (order_goods_id);
ALTER TABLE p_payments                ADD CONSTRAINT pk_p_payments PRIMARY KEY (payment_id);
ALTER TABLE p_order_status_histories  ADD CONSTRAINT pk_p_order_status_histories PRIMARY KEY (order_status_id);
ALTER TABLE p_order_qr_codes          ADD CONSTRAINT pk_p_order_qr_codes PRIMARY KEY (qr_id);
ALTER TABLE p_checkins                ADD CONSTRAINT pk_p_checkins PRIMARY KEY (checkin_id);

-- Unique constraints
ALTER TABLE p_users  ADD CONSTRAINT uq_p_users_email UNIQUE (email);
ALTER TABLE p_orders ADD CONSTRAINT uq_p_orders_order_no UNIQUE (order_no);

-- Foreign Keys
ALTER TABLE p_customer_addresses     ADD CONSTRAINT fk_addr_user           FOREIGN KEY (user_id) REFERENCES p_users(user_id);
ALTER TABLE p_stores                 ADD CONSTRAINT fk_stores_owner        FOREIGN KEY (user_id) REFERENCES p_users(user_id);
ALTER TABLE p_popups                 ADD CONSTRAINT fk_popups_store        FOREIGN KEY (store_id) REFERENCES p_stores(store_id);
ALTER TABLE p_popup_schedules        ADD CONSTRAINT fk_schedules_popup     FOREIGN KEY (popup_id) REFERENCES p_popups(popup_id);
ALTER TABLE p_goods_variants         ADD CONSTRAINT fk_goods_popup         FOREIGN KEY (popup_id) REFERENCES p_popups(popup_id);
ALTER TABLE p_orders                 ADD CONSTRAINT fk_orders_user         FOREIGN KEY (user_id) REFERENCES p_users(user_id);
ALTER TABLE p_orders                 ADD CONSTRAINT fk_orders_store        FOREIGN KEY (store_id) REFERENCES p_stores(store_id);
ALTER TABLE p_order_goods            ADD CONSTRAINT fk_order_goods_order   FOREIGN KEY (order_id) REFERENCES p_orders(order_id);
ALTER TABLE p_order_goods            ADD CONSTRAINT fk_order_goods_schedule FOREIGN KEY (schedule_id) REFERENCES p_popup_schedules(schedule_id);
ALTER TABLE p_order_goods            ADD CONSTRAINT fk_order_goods_variant FOREIGN KEY (goods_variant_id) REFERENCES p_goods_variants(goods_id);
ALTER TABLE p_payments               ADD CONSTRAINT fk_payments_order      FOREIGN KEY (order_id) REFERENCES p_orders(order_id);
ALTER TABLE p_order_status_histories ADD CONSTRAINT fk_order_status_order  FOREIGN KEY (order_id) REFERENCES p_orders(order_id);
ALTER TABLE p_order_qr_codes         ADD CONSTRAINT fk_qr_order            FOREIGN KEY (order_id) REFERENCES p_orders(order_id);
ALTER TABLE p_checkins               ADD CONSTRAINT fk_checkins_order      FOREIGN KEY (order_id) REFERENCES p_orders(order_id);
ALTER TABLE p_checkins               ADD CONSTRAINT fk_checkins_qr         FOREIGN KEY (order_qr_code_id) REFERENCES p_order_qr_codes(qr_id);
